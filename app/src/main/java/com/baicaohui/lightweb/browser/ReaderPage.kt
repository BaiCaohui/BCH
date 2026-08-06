package com.baicaohui.lightweb.browser

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

data class ReaderArticle(
    val title: String,
    val byline: String,
    val contentHtml: String,
)

/** 阅读模式纯逻辑：生成注入脚本、解析 JS 结果、渲染离线阅读页（JVM 可测）。 */
object ReaderPage {

    private val json = Json { ignoreUnknownKeys = true }
    private val themes = setOf("light", "sepia", "dark")

    private val READER_CSS = """
        #bch-reader-root{position:fixed;inset:0;z-index:2147483647;overflow-y:auto;background:var(--bch-bg);color:var(--bch-fg);font-size:var(--bch-font,100%);line-height:1.75;box-sizing:border-box;-webkit-text-size-adjust:100%}
        #bch-reader-root[data-theme="light"]{--bch-bg:#ffffff;--bch-fg:#24292f;--bch-muted:#57606a;--bch-border:#d0d7de;--bch-code:#f6f8fa}
        #bch-reader-root[data-theme="sepia"]{--bch-bg:#f6efe0;--bch-fg:#433422;--bch-muted:#7a6a52;--bch-border:#ddd0b4;--bch-code:#efe4cd}
        #bch-reader-root[data-theme="dark"]{--bch-bg:#14171c;--bch-fg:#e6e6e6;--bch-muted:#9aa0a6;--bch-border:#33383f;--bch-code:#1d2126}
        #bch-reader-root .bch-reader-inner{max-width:680px;margin:0 auto;padding:24px 20px 64px}
        #bch-reader-root .bch-reader-toolbar{position:sticky;top:0;display:flex;gap:8px;padding:8px 0;background:var(--bch-bg)}
        #bch-reader-root .bch-reader-btn{border:1px solid var(--bch-border);background:transparent;color:var(--bch-fg);border-radius:8px;padding:6px 12px;font-size:14px;cursor:pointer}
        #bch-reader-root h1.bch-reader-title{font-size:1.8em;line-height:1.3;margin:16px 0 8px;color:var(--bch-fg)}
        #bch-reader-root p.bch-reader-byline{color:var(--bch-muted);font-size:0.95em;margin:0 0 16px}
        #bch-reader-root .bch-reader-content p{margin:0 0 1em}
        #bch-reader-root .bch-reader-content img{max-width:100%;height:auto}
        #bch-reader-root .bch-reader-content a{color:var(--bch-fg);text-decoration:underline}
        #bch-reader-root .bch-reader-content pre,#bch-reader-root .bch-reader-content code{background:var(--bch-code);border-radius:6px}
        #bch-reader-root .bch-reader-content pre{padding:12px;overflow-x:auto}
        #bch-reader-root .bch-reader-content blockquote{border-left:3px solid var(--bch-border);margin:0 0 1em;padding-left:16px;color:var(--bch-muted)}
        #bch-reader-root .bch-reader-offline{display:inline-block;margin:0 0 12px;padding:4px 10px;border:1px solid var(--bch-border);border-radius:999px;color:var(--bch-muted);font-size:0.85em}
    """.trimIndent()

    private val READER_JS = """
        function bchReaderSetup(root) {
          root.addEventListener('click', function(e) {
            var t = e.target;
            while (t && t !== root) {
              if (t.getAttribute && t.getAttribute('data-bch-font')) {
                bchAdjustFont(root, parseInt(t.getAttribute('data-bch-font'), 10) || 0);
                return;
              }
              if (t.getAttribute && t.getAttribute('data-bch-theme')) {
                bchCycleTheme(root);
                return;
              }
              t = t.parentNode;
            }
          });
        }
        function bchAdjustFont(root, delta) {
          var size = parseInt(root.style.getPropertyValue('--bch-font') || '100', 10) || 100;
          size = Math.max(70, Math.min(180, size + delta * 10));
          root.style.setProperty('--bch-font', size + '%');
          try { localStorage.setItem('bch-reader-font', String(size)); } catch (e) {}
        }
        function bchCycleTheme(root) {
          var themes = ['light', 'sepia', 'dark'];
          var current = root.getAttribute('data-theme') || 'light';
          var next = themes[(themes.indexOf(current) + 1) % themes.length];
          root.setAttribute('data-theme', next);
          try { localStorage.setItem('bch-reader-theme', next); } catch (e) {}
        }
        function bchSavedTheme(fallback) {
          try { return localStorage.getItem('bch-reader-theme') || fallback; } catch (e) { return fallback; }
        }
        function bchSavedFont() {
          try { return parseInt(localStorage.getItem('bch-reader-font') || '100', 10); } catch (e) { return 100; }
        }
        function bchEscapeHtml(s) {
          return String(s == null ? '' : s)
            .replace(/&/g, '&amp;').replace(/</g, '&lt;').replace(/>/g, '&gt;')
            .replace(/"/g, '&quot;').replace(/'/g, '&#39;');
        }
        function bchReaderHtml(article, savedTitle) {
          var byline = article.byline
            ? '<p class="bch-reader-byline">' + bchEscapeHtml(article.byline) + '</p>'
            : '';
          return '<div class="bch-reader-inner">' +
            '<div class="bch-reader-toolbar">' +
              '<button type="button" class="bch-reader-btn" data-bch-font="-1">A−</button>' +
              '<button type="button" class="bch-reader-btn" data-bch-font="1">A+</button>' +
              '<button type="button" class="bch-reader-btn" data-bch-theme="1">主题</button>' +
            '</div>' +
            '<h1 class="bch-reader-title">' + bchEscapeHtml(article.title) + '</h1>' +
            byline +
            '<div class="bch-reader-content">' + article.content + '</div>' +
          '</div>';
        }
    """.trimIndent()

    private val ENTER_TEMPLATE = """
        (function() {
          if (window.__bchReaderState) {
            return JSON.stringify({ok:false, reason:'already-active'});
          }
          /*__BCH_READABILITY_SOURCE__*/
          /*__BCH_READER_JS__*/
          try {
            var doc = document.cloneNode(true);
            var article = new Readability(doc, {charThreshold: 500}).parse();
            if (!article || !article.content) {
              return JSON.stringify({ok:false, reason:'no-content'});
            }
            var savedTitle = document.title;
            window.__bchReaderState = {
              title: savedTitle,
              bodyChildren: Array.prototype.slice.call(document.body.childNodes),
              scrollY: window.scrollY
            };
            var style = document.createElement('style');
            style.id = 'bch-reader-style';
            style.textContent = __BCH_READER_CSS__;
            document.head.appendChild(style);
            var root = document.createElement('div');
            root.id = 'bch-reader-root';
            root.setAttribute('data-theme', bchSavedTheme(__BCH_INITIAL_THEME__));
            root.style.setProperty('--bch-font', bchSavedFont() + '%');
            root.innerHTML = bchReaderHtml(article, savedTitle);
            bchReaderSetup(root);
            document.body.innerHTML = '';
            document.body.appendChild(root);
            document.body.style.margin = '0';
            document.title = article.title || savedTitle;
            return JSON.stringify({
              ok: true,
              title: article.title || '',
              byline: article.byline || '',
              content: article.content
            });
          } catch (e) {
            return JSON.stringify({ok:false, reason: String(e && e.message ? e.message : e)});
          }
        })()
    """.trimIndent()

    private val EXIT_TEMPLATE = """
        (function() {
          var s = window.__bchReaderState;
          if (!s) return JSON.stringify({ok:false, reason:'not-active'});
          document.body.innerHTML = '';
          for (var i = 0; i < s.bodyChildren.length; i++) {
            document.body.appendChild(s.bodyChildren[i]);
          }
          document.body.style.margin = '';
          var style = document.getElementById('bch-reader-style');
          if (style) style.remove();
          document.title = s.title;
          window.__bchReaderState = null;
          window.scrollTo(0, s.scrollY);
          return JSON.stringify({ok:true});
        })()
    """.trimIndent()

    private val OFFLINE_TEMPLATE = """
        <!DOCTYPE html>
        <html>
        <head>
        <meta charset="utf-8">
        <meta name="viewport" content="width=device-width, initial-scale=1">
        <style>__BCH_READER_CSS__</style>
        </head>
        <body>
        <div id="bch-reader-root" data-theme="__BCH_THEME__">
        <div class="bch-reader-inner">
        <span class="bch-reader-offline">__BCH_BADGE__</span>
        <div class="bch-reader-toolbar">
        <button type="button" class="bch-reader-btn" data-bch-font="-1">A−</button>
        <button type="button" class="bch-reader-btn" data-bch-font="1">A+</button>
        <button type="button" class="bch-reader-btn" data-bch-theme="1">主题</button>
        </div>
        <h1 class="bch-reader-title">__BCH_TITLE__</h1>
        __BCH_BYLINE__
        <div class="bch-reader-content">__BCH_CONTENT__</div>
        </div>
        </div>
        <script>__BCH_READER_JS__bchReaderSetup(document.getElementById('bch-reader-root'));</script>
        </body>
        </html>
    """.trimIndent()

    fun sanitizeTheme(theme: String): String = if (theme in themes) theme else "light"

    fun enterScript(readabilityJs: String, theme: String): String =
        ENTER_TEMPLATE
            .replace("/*__BCH_READABILITY_SOURCE__*/", readabilityJs)
            .replace("/*__BCH_READER_JS__*/", READER_JS)
            .replace("__BCH_READER_CSS__", ConsoleCommands.jsString(READER_CSS))
            .replace("__BCH_INITIAL_THEME__", ConsoleCommands.jsString(sanitizeTheme(theme)))

    fun exitScript(): String = EXIT_TEMPLATE

    fun parseResult(raw: String?): ReaderArticle? {
        val payload = decode<ReaderPayload>(raw) ?: return null
        if (!payload.ok || payload.content.isBlank()) return null
        return ReaderArticle(payload.title, payload.byline, payload.content)
    }

    fun parseExit(raw: String?): Boolean = decode<ReaderPayload>(raw)?.ok == true

    fun offlineHtml(
        title: String,
        byline: String,
        contentHtml: String,
        theme: String,
        offlineBadge: String,
    ): String = OFFLINE_TEMPLATE
        .replace("__BCH_THEME__", sanitizeTheme(theme))
        .replace("__BCH_BADGE__", htmlEscape(offlineBadge))
        .replace("__BCH_TITLE__", htmlEscape(title))
        .replace(
            "__BCH_BYLINE__",
            if (byline.isBlank()) "" else "<p class=\"bch-reader-byline\">${htmlEscape(byline)}</p>",
        )
        .replace("__BCH_CONTENT__", contentHtml)
        .replace("__BCH_READER_CSS__", READER_CSS)
        .replace("__BCH_READER_JS__", READER_JS)

    fun htmlEscape(value: String): String = buildString {
        value.forEach { c ->
            when (c) {
                '&' -> append("&amp;")
                '<' -> append("&lt;")
                '>' -> append("&gt;")
                '"' -> append("&quot;")
                '\'' -> append("&#39;")
                else -> append(c)
            }
        }
    }

    @Serializable
    private data class ReaderPayload(
        val ok: Boolean = false,
        val title: String = "",
        val byline: String = "",
        val content: String = "",
        val reason: String? = null,
    )

    private inline fun <reified T> decode(raw: String?): T? {
        val text = ConsoleCommands.unescapeJsResult(raw ?: return null)
        return runCatching { json.decodeFromString<T>(text) }.getOrNull()
    }
}
