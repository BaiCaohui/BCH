package com.baicaohui.lightweb.browser

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

@Serializable
data class SelectionInfo(
    val text: String = "",
    val left: Float = 0f,
    val top: Float = 0f,
    val width: Float = 0f,
    val height: Float = 0f,
)

object PageContextMenus {

    private val json = Json { ignoreUnknownKeys = true }

    fun selectionInfoScript(): String = """
        (function() {
          var sel = window.getSelection();
          if (!sel || sel.rangeCount === 0 || sel.isCollapsed) return null;
          var rect = sel.getRangeAt(0).getBoundingClientRect();
          return JSON.stringify({text: sel.toString(), left: rect.left, top: rect.top, width: rect.width, height: rect.height});
        })();
    """.trimIndent()

    fun selectionInfoOrSelectAtScript(x: Float, y: Float): String = """
        (function(x, y) {
          var sel = window.getSelection();
          if (sel && sel.rangeCount > 0 && !sel.isCollapsed) {
            var r0 = sel.getRangeAt(0).getBoundingClientRect();
            return JSON.stringify({text: sel.toString(), left: r0.left, top: r0.top, width: r0.width, height: r0.height});
          }
          var sx = x / window.devicePixelRatio;
          var sy = y / window.devicePixelRatio;
          var el = document.elementFromPoint(x, y) || document.elementFromPoint(sx, sy);
          if (!el) return null;
          try {
            var range = document.createRange();
            range.selectNodeContents(el);
            sel.removeAllRanges();
            sel.addRange(range);
            var rect = range.getBoundingClientRect();
            return JSON.stringify({text: sel.toString(), left: rect.left, top: rect.top, width: rect.width, height: rect.height});
          } catch (e) {
            return null;
          }
        })($x, $y);
    """.trimIndent()

    fun selectionTextScript(): String =
        "(function(){ var s = window.getSelection(); return s ? s.toString() : ''; })();"

    fun selectAllScript(): String =
        "(function(){ document.execCommand('selectAll'); })();"

    fun linkTextScript(x: Float, y: Float): String = """
        (function(x, y) {
          var sx = x / window.devicePixelRatio;
          var sy = y / window.devicePixelRatio;
          var sel = window.getSelection();
          var node = sel && sel.anchorNode;
          if (node && node.nodeType === 3) node = node.parentNode;
          while (node && node.tagName !== 'A') node = node.parentNode;
          if (!node) {
            node = document.elementFromPoint(x, y) || document.elementFromPoint(sx, sy);
            while (node && node.tagName !== 'A') node = node.parentElement;
          }
          return node ? node.textContent : '';
        })($x, $y);
    """.trimIndent()

    fun parseSelectionInfo(raw: String?): SelectionInfo? {
        if (raw.isNullOrBlank()) return null
        val inner = runCatching { json.decodeFromString<String>(raw) }.getOrNull() ?: return null
        return runCatching { json.decodeFromString<SelectionInfo>(inner) }.getOrNull()
    }

    fun parseText(raw: String?): String {
        if (raw.isNullOrBlank()) return ""
        return runCatching { json.decodeFromString<String>(raw) }.getOrDefault("")
    }
}
