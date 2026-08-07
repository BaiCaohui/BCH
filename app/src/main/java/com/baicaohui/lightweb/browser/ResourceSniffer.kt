package com.baicaohui.lightweb.browser

import java.net.URI
import java.net.URLDecoder

enum class ResourceKind { VIDEO, AUDIO, IMAGE }

data class SniffedResource(
    val url: String,
    val kind: ResourceKind,
    val name: String,
    val sizeBytes: Long? = null,
)

object ResourceSniffer {

    private val VIDEO_EXTS = setOf("mp4", "flv", "webm", "m3u8")
    private val AUDIO_EXTS = setOf("mp3", "m4a", "ogg", "wav")
    private val IMAGE_EXTS = setOf("jpg", "jpeg", "png", "gif", "svg", "webp")

    fun kindOf(url: String): ResourceKind? {
        val ext = extensionOf(url) ?: return null
        return when {
            ext in VIDEO_EXTS -> ResourceKind.VIDEO
            ext in AUDIO_EXTS -> ResourceKind.AUDIO
            ext in IMAGE_EXTS -> ResourceKind.IMAGE
            else -> null
        }
    }

    fun extensionOf(url: String): String? {
        val path = url.substringBefore('?').substringBefore('#')
        val last = path.substringAfterLast('/')
        val ext = last.substringAfterLast('.', "").lowercase()
        return ext.takeIf { it.isNotBlank() && it != last }
    }

    fun nameFor(url: String): String {
        val path = url.substringBefore('?').substringBefore('#')
        val raw = path.substringAfterLast('/')
        val decoded = runCatching {
            URLDecoder.decode(raw, Charsets.UTF_8.name())
        }.getOrDefault(raw)
        val name = decoded.trim().ifBlank { "download" }
        val ext = extensionOf(url)
        return if (name.contains('.')) name else name + (ext?.let { ".$it" } ?: "")
    }

    fun resolveUrl(baseUrl: String, ref: String): String? {
        if (ref.isBlank()) return null
        val candidate = try {
            URI(baseUrl).resolve(ref).toString()
        } catch (_: Exception) {
            return null
        }
        return candidate.takeIf {
            it.startsWith("http://") || it.startsWith("https://")
        }
    }

    fun mimeFor(kind: ResourceKind, url: String): String = when (kind) {
        ResourceKind.VIDEO -> when (extensionOf(url)) {
            "flv" -> "video/x-flv"
            "webm" -> "video/webm"
            "m3u8" -> "application/vnd.apple.mpegurl"
            else -> "video/mp4"
        }
        ResourceKind.AUDIO -> when (extensionOf(url)) {
            "mp3" -> "audio/mpeg"
            "m4a" -> "audio/mp4"
            "ogg" -> "audio/ogg"
            else -> "audio/wav"
        }
        ResourceKind.IMAGE -> when (extensionOf(url)) {
            "jpg", "jpeg" -> "image/jpeg"
            "png" -> "image/png"
            "gif" -> "image/gif"
            "svg" -> "image/svg+xml"
            else -> "image/webp"
        }
    }

    fun domScanScript(): String = """
        (function() {
          function kindOf(u) {
            if (!u) return null;
            var p = u.split('?')[0].split('#')[0].toLowerCase();
            var i = p.lastIndexOf('.');
            if (i < 0) return null;
            var ext = p.substring(i + 1);
            if (['mp4','flv','webm','m3u8'].indexOf(ext) >= 0) return 'video';
            if (['mp3','m4a','ogg','wav'].indexOf(ext) >= 0) return 'audio';
            if (['jpg','jpeg','png','gif','svg','webp'].indexOf(ext) >= 0) return 'image';
            return null;
          }
          var out = [];
          function push(u, k) { if (u && k) out.push({u: u, k: k}); }
          var imgs = document.querySelectorAll('img');
          for (var i = 0; i < imgs.length; i++) push(imgs[i].currentSrc || imgs[i].src, 'image');
          var vids = document.querySelectorAll('video');
          for (var i = 0; i < vids.length; i++) push(vids[i].currentSrc || vids[i].src, 'video');
          var auds = document.querySelectorAll('audio');
          for (var i = 0; i < auds.length; i++) push(auds[i].currentSrc || auds[i].src, 'audio');
          var sources = document.querySelectorAll('video source, audio source');
          for (var i = 0; i < sources.length; i++) {
            push(sources[i].src, sources[i].parentNode && sources[i].parentNode.tagName === 'VIDEO' ? 'video' : 'audio');
          }
          var links = document.querySelectorAll('a[href]');
          for (var i = 0; i < links.length; i++) push(links[i].href, kindOf(links[i].href));
          var preloads = document.querySelectorAll('link[rel="preload"][href]');
          for (var i = 0; i < preloads.length; i++) push(preloads[i].href, kindOf(preloads[i].href));
          var posters = document.querySelectorAll('video[poster]');
          for (var i = 0; i < posters.length; i++) push(posters[i].poster, 'image');
          return JSON.stringify(out);
        })();
    """.trimIndent()
}
