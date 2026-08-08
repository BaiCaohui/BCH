package com.baicaohui.lightweb.browser

import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

@Serializable
data class MarkedAdSelection(
    val selector: String = "",
    val urls: List<String> = emptyList(),
    val html: String = "",
    val found: Boolean = false,
    val left: Double = 0.0,
    val top: Double = 0.0,
    val width: Double = 0.0,
    val height: Double = 0.0,
)

/** 框选标记广告：识别选框内元素并生成 CSS 选择器，同时收集内部广告资源 URL。 */
object PageMarkAd {
    private val json = Json { ignoreUnknownKeys = true }

    /**
     * 点击/框选后自动识别广告元素：从点击位置向上查找最贴合的广告区块
     * （iframe/img/video 等媒体、含广告特征 id/class、或包含广告链接的容器），
     * 返回元素 CSS 选择器、外层 HTML、内部全部资源链接以及元素实际边界（用于选框吸附）。
     */
    fun identifyScript(cx: Double, cy: Double): String = """
        (function() {
          function adLike(el) {
            if (!el || el === document.body || el === document.documentElement) return false;
            var t = (el.tagName || '').toLowerCase();
            if (t === 'iframe' || t === 'img' || t === 'video' || t === 'object' || t === 'embed') return true;
            if (el.querySelector('iframe, img, video, object, embed, a[href]')) return true;
            var s = ((el.id || '') + ' ' + (el.className || '')).toLowerCase();
            return /(^|[-_ ])(ad|ads|advert|advertise|banner|sponsor|promo)([-_ ]|$)/.test(s) ||
              s.indexOf('ad-') === 0 || s.indexOf('_ad') >= 0 || s.indexOf('ads-') === 0;
          }
          function cssSelector(el) {
            if (!el || el === document.body) return '';
            if (el.id) return '#' + CSS.escape(el.id);
            var path = [];
            var cur = el;
            while (cur && cur !== document.body && cur !== document.documentElement) {
              var part = cur.tagName.toLowerCase();
              if (cur.id) { path.unshift('#' + CSS.escape(cur.id)); break; }
              if (cur.classList && cur.classList.length) {
                part += '.' + Array.prototype.slice.call(cur.classList).slice(0, 2).map(function(c) { return CSS.escape(c); }).join('.');
              }
              var parent = cur.parentElement;
              if (parent) {
                var same = Array.prototype.filter.call(parent.children, function(c) { return c.tagName === cur.tagName; });
                if (same.length > 1) part += ':nth-of-type(' + (same.indexOf(cur) + 1) + ')';
              }
              path.unshift(part);
              cur = parent;
            }
            return path.join(' > ');
          }
          function collectUrls(el) {
            var urls = [];
            var nodes = el.querySelectorAll('iframe[src], img[src], a[href], script[src], video[src], audio source[src], source[src], object[data], embed[src], link[href], form[action]');
            var all = [el].concat(Array.prototype.slice.call(nodes));
            all.forEach(function(n) {
              var u = (n.currentSrc || n.src || n.href || n.data ||
                (n.getAttribute && (n.getAttribute('src') || n.getAttribute('href') || n.getAttribute('data') || n.getAttribute('action'))) || '').trim();
              if (u && u.indexOf('data:') !== 0 && u.indexOf('javascript:') !== 0 && urls.indexOf(u) === -1) urls.push(u);
            });
            return urls;
          }
          var el = document.elementFromPoint(__CX__, __CY__);
          if (!el) return JSON.stringify({found:false});
          var best = null;
          var cur = el;
          while (cur && cur !== document.body && cur !== document.documentElement) {
            if (adLike(cur)) { best = cur; break; }
            cur = cur.parentElement;
          }
          if (!best) {
            best = el;
            var b = el;
            while (b && b !== document.body) {
              var r = b.getBoundingClientRect();
              if (r.width >= 60 && r.height >= 40) { best = b; break; }
              b = b.parentElement;
            }
          }
          var rect = best.getBoundingClientRect();
          return JSON.stringify({
            found: true,
            selector: cssSelector(best),
            html: best.outerHTML ? best.outerHTML.slice(0, 8000) : '',
            urls: collectUrls(best),
            left: rect.left,
            top: rect.top,
            width: rect.width,
            height: rect.height
          });
        })()
    """.trimIndent()
        .replace("__CX__", cx.toInt().toString())
        .replace("__CY__", cy.toInt().toString())

    fun selectionScript(cx: Double, cy: Double, w: Double, h: Double): String = """
        (function() {
          function elAt(x, y) { return document.elementFromPoint(x, y); }
          function pick() {
            var pts = [[__CX__,__CY__],[__CX__+__W__*0.25,__CY__+__H__*0.25],[__CX__+__W__*0.75,__CY__+__H__*0.25],[__CX__+__W__*0.25,__CY__+__H__*0.75],[__CX__+__W__*0.75,__CY__+__H__*0.75]];
            var best = null, bestArea = 0, boxArea = __W__ * __H__;
            for (var i = 0; i < pts.length; i++) {
              var el = elAt(pts[i][0], pts[i][1]);
              while (el && el !== document.body && el !== document.documentElement) {
                var r = el.getBoundingClientRect();
                var area = r.width * r.height;
                if (area >= boxArea * 0.5 && area > bestArea) { bestArea = area; best = el; }
                el = el.parentElement;
              }
            }
            return best;
          }
          function cssSelector(el) {
            if (!el || el === document.body) return '';
            if (el.id) return '#' + CSS.escape(el.id);
            var path = [];
            var cur = el;
            while (cur && cur !== document.body && cur !== document.documentElement) {
              var part = cur.tagName.toLowerCase();
              if (cur.id) { path.unshift('#' + CSS.escape(cur.id)); break; }
              if (cur.classList && cur.classList.length) {
                part += '.' + Array.prototype.slice.call(cur.classList).slice(0, 2).map(function(c) { return CSS.escape(c); }).join('.');
              }
              var parent = cur.parentElement;
              if (parent) {
                var same = Array.prototype.filter.call(parent.children, function(c) { return c.tagName === cur.tagName; });
                if (same.length > 1) part += ':nth-of-type(' + (same.indexOf(cur) + 1) + ')';
              }
              path.unshift(part);
              cur = parent;
            }
            return path.join(' > ');
          }
          var el = pick();
          if (!el) return JSON.stringify({selector: '', urls: []});
          var urls = [];
          el.querySelectorAll('iframe[src], img[src], a[href], script[src], video[src], audio source[src]').forEach(function(n) {
            var u = (n.currentSrc || n.src || n.href || (n.getAttribute && n.getAttribute('src')) || '').trim();
            if (u && u.indexOf('data:') !== 0 && urls.indexOf(u) === -1) urls.push(u);
          });
          return JSON.stringify({selector: cssSelector(el), urls: urls});
        })()
    """.trimIndent()
        .replace("__CX__", cx.toInt().toString())
        .replace("__CY__", cy.toInt().toString())
        .replace("__W__", w.toInt().toString())
        .replace("__H__", h.toInt().toString())

    fun parseSelection(raw: String?): MarkedAdSelection {
        if (raw.isNullOrBlank()) return MarkedAdSelection()
        // evaluateJavascript 返回的是 JSON 编码字符串，先解码一次得到内部 JSON 文本。
        val text = runCatching { json.decodeFromString<String>(raw) }.getOrDefault(raw)
        return runCatching { json.decodeFromString<MarkedAdSelection>(text) }
            .getOrDefault(MarkedAdSelection())
    }

    /** 注入隐藏已标记广告的 CSS；重复注入时先移除旧样式再重建。 */
    fun hideSelectorScript(selectors: List<String>): String {
        val encoded = json.encodeToString(selectors)
        return "(function(){var old=document.getElementById('bch-marked-ad-hide');" +
            "if(old)old.remove();" +
            "var s=document.createElement('style');s.id='bch-marked-ad-hide';" +
            "s.textContent=$encoded.map(function(sel){return sel+'{display:none!important}'}).join(' ');" +
            "document.head.appendChild(s)})()"
    }
}
