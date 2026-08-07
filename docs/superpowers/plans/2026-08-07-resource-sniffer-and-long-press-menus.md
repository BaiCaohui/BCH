# 资源嗅探 + 长按菜单（文字/链接/图片）实施计划

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 为 BCH 浏览器加入网页资源嗅探（视频/音频/图片列表 + 下载），以及长按文字/链接/图片的上下文弹窗。
**Architecture:** 纯 Kotlin 逻辑（资源类型识别、URL 归一化、DOM 扫描脚本、嗅探收集器、上下文菜单 JS 解析）与 UI 分离；WebView 通过 `startActionMode` 拦截长按并回抛到 Compose 层；嗅探列表走新导航路由，复用现有下载管理器。
**Tech Stack:** Kotlin + Compose（Material3）+ Coil + kotlinx.serialization + StateFlow；现有 WebView/DownloadManager/FileProvider。

---

## 文件结构

### 新建
- `browser/ResourceSniffer.kt`：资源类型/扩展名/文件名/MIME/URL 解析 + DOM 扫描脚本（纯 Kotlin）
- `browser/ResourceSniffController.kt`：每个 WebView 的嗅探收集器（StateFlow）
- `browser/PageContextMenus.kt`：长按菜单 JS 脚本与结果解析（纯 Kotlin）
- `ui/sniffer/ResourceSniffScreen.kt`：嗅探结果列表页（缩略图/名字/URL/下载）
- `ui/browser/LongPressMenus.kt`：文字锚点弹窗、链接居中弹窗、图片居中弹窗
- 测试：`browser/ResourceSnifferTest.kt`、`browser/ResourceSniffControllerTest.kt`、`browser/PageContextMenusTest.kt`

### 修改
- `browser/BrowserWebView.kt`：嗅探控制器、长按回调、`startActionMode` 拦截、资源请求钩子
- `browser/WebClientPolicy.kt`：`BchWebViewClient` 增加资源请求回调
- `ui/navigation/BchRoute.kt`：新增 `SNIFFER` 路由
- `ui/browser/MenuItems.kt` / `MenuOrder.kt`：新增 `sniffer` 菜单项
- `ui/BchAppRoot.kt`、`ui/IncognitoAppRoot.kt`：菜单项 + 路由接线
- `ui/browser/BrowserScreen.kt`：长按弹窗状态、动作、WebView 监听器
- `res/values/strings.xml`：新增文案

## 关键设计决策

1. **嗅探收集**：WebView 进入嗅探页时 `ResourceSniffController.start()` + 清空 + 执行 DOM 扫描；随后 `shouldInterceptRequest` 把非主框架请求喂给收集器，动态加载的资源也会出现；页面切换时清空列表。
2. **长按拦截**：`BrowserWebView` 覆写 `startActionMode(callback, type)`，按 `hitTestResult.type` 分流：`SRC_ANCHOR_TYPE`→链接弹窗、`IMAGE_TYPE/SRC_IMAGE_ANCHOR_TYPE`→图片弹窗、`UNKNOWN_TYPE`→文字选择弹窗；返回 `null` 隐藏系统工具栏。输入框（`EDIT_TEXT_TYPE`）保持系统默认。
3. **文字弹窗定位**：JS 取选区 `getBoundingClientRect()`，CSS px × density 转视图 px，用 Compose `Popup(offset)` 锚在选区下方。
4. **下载复用**：嗅探条目和链接/图片下载都走现有 `DownloadHandler`（系统）或 `AppDownloadManager`（内置）分支，图片复制用 FileProvider + `ClipData.newUri`。
5. **无痕**：普通进程内“在无痕模式标签页中开启”启动 `IncognitoActivity` 并传 `EXTRA_URL`；无痕进程内的嗅探路由同样注册。

---

## Task 1：ResourceSniffer 纯逻辑（TDD）

**Files:**
- Create: `app/src/main/java/com/baicaohui/lightweb/browser/ResourceSniffer.kt`
- Test: `app/src/test/java/com/baicaohui/lightweb/browser/ResourceSnifferTest.kt`

- [ ] **Step 1: 写失败测试**

`ResourceSnifferTest.kt`：

```kotlin
package com.baicaohui.lightweb.browser

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ResourceSnifferTest {

    @Test
    fun `kind detects video extensions with query and case`() {
        assertEquals(ResourceKind.VIDEO, ResourceSniffer.kindOf("https://a.com/v.MP4?token=1"))
        assertEquals(ResourceKind.VIDEO, ResourceSniffer.kindOf("https://a.com/live.m3u8"))
        assertEquals(ResourceKind.VIDEO, ResourceSniffer.kindOf("https://a.com/a.flv"))
        assertEquals(ResourceKind.VIDEO, ResourceSniffer.kindOf("https://a.com/a.webm"))
    }

    @Test
    fun `kind detects audio extensions`() {
        assertEquals(ResourceKind.AUDIO, ResourceSniffer.kindOf("https://a.com/a.mp3"))
        assertEquals(ResourceKind.AUDIO, ResourceSniffer.kindOf("https://a.com/a.m4a"))
        assertEquals(ResourceKind.AUDIO, ResourceSniffer.kindOf("https://a.com/a.ogg"))
        assertEquals(ResourceKind.AUDIO, ResourceSniffer.kindOf("https://a.com/a.wav"))
    }

    @Test
    fun `kind detects image extensions`() {
        assertEquals(ResourceKind.IMAGE, ResourceSniffer.kindOf("https://a.com/a.jpg"))
        assertEquals(ResourceKind.IMAGE, ResourceSniffer.kindOf("https://a.com/a.jpeg"))
        assertEquals(ResourceKind.IMAGE, ResourceSniffer.kindOf("https://a.com/a.png"))
        assertEquals(ResourceKind.IMAGE, ResourceSniffer.kindOf("https://a.com/a.gif"))
        assertEquals(ResourceKind.IMAGE, ResourceSniffer.kindOf("https://a.com/a.svg"))
        assertEquals(ResourceKind.IMAGE, ResourceSniffer.kindOf("https://a.com/a.webp"))
    }

    @Test
    fun `kind ignores unsupported extensions`() {
        assertNull(ResourceSniffer.kindOf("https://a.com/page.html"))
        assertNull(ResourceSniffer.kindOf("https://a.com/api"))
        assertNull(ResourceSniffer.kindOf("https://a.com/file.pdf"))
    }

    @Test
    fun `name keeps extension and decodes`() {
        assertEquals("photo one.jpg", ResourceSniffer.nameFor("https://a.com/photo%20one.jpg"))
        assertEquals("video.mp4", ResourceSniffer.nameFor("https://a.com/video.mp4?x=1"))
        assertEquals("download", ResourceSniffer.nameFor("https://a.com/"))
    }

    @Test
    fun `resolve handles relative and protocol-relative urls`() {
        assertEquals(
            "https://a.com/v.mp4",
            ResourceSniffer.resolveUrl("https://a.com/page", "v.mp4"),
        )
        assertEquals(
            "https://cdn.com/v.mp4",
            ResourceSniffer.resolveUrl("https://a.com/page", "//cdn.com/v.mp4"),
        )
        assertEquals(
            "https://a.com/v.mp4",
            ResourceSniffer.resolveUrl("https://a.com/dir/page.html", "../v.mp4"),
        )
        assertEquals(
            "https://a.com/v.mp4",
            ResourceSniffer.resolveUrl("https://a.com/page", "https://a.com/v.mp4"),
        )
    }

    @Test
    fun `resolve rejects blank and unsupported schemes`() {
        assertNull(ResourceSniffer.resolveUrl("https://a.com", ""))
        assertNull(ResourceSniffer.resolveUrl("https://a.com", "javascript:alert(1)"))
        assertNull(ResourceSniffer.resolveUrl("https://a.com", "data:image/png;base64,xx"))
    }

    @Test
    fun `mime maps per kind and extension`() {
        assertEquals("video/mp4", ResourceSniffer.mimeFor(ResourceKind.VIDEO, "https://a.com/v.mp4"))
        assertEquals("video/x-flv", ResourceSniffer.mimeFor(ResourceKind.VIDEO, "https://a.com/v.flv"))
        assertEquals(
            "application/vnd.apple.mpegurl",
            ResourceSniffer.mimeFor(ResourceKind.VIDEO, "https://a.com/live.m3u8"),
        )
        assertEquals("audio/mpeg", ResourceSniffer.mimeFor(ResourceKind.AUDIO, "https://a.com/a.mp3"))
        assertEquals("image/svg+xml", ResourceSniffer.mimeFor(ResourceKind.IMAGE, "https://a.com/a.svg"))
    }
}
```

- [ ] **Step 2: 运行确认失败**

```powershell
$env:GRADLE_USER_HOME = "D:\gradle-home"; $env:JAVA_HOME = "C:\Program Files\Android\Android Studio\jbr"; $env:Path = "$env:JAVA_HOME\bin;$env:Path"; .\gradlew.bat :app:testDebugUnitTest --tests "com.baicaohui.lightweb.browser.ResourceSnifferTest"
```

预期：编译失败（ResourceSniffer 不存在）。

- [ ] **Step 3: 最小实现**

`ResourceSniffer.kt`：

```kotlin
package com.baicaohui.lightweb.browser

import java.net.URI
import java.net.URLDecoder

enum class ResourceKind { VIDEO, AUDIO, IMAGE }

data class SniffedResource(
    val url: String,
    val kind: ResourceKind,
    val name: String,
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
```

- [ ] **Step 4: 运行确认通过**

同上命令，预期全绿。

## Task 2：ResourceSniffController（TDD）

**Files:**
- Create: `app/src/main/java/com/baicaohui/lightweb/browser/ResourceSniffController.kt`
- Test: `app/src/test/java/com/baicaohui/lightweb/browser/ResourceSniffControllerTest.kt`

- [ ] **Step 1: 写失败测试**

```kotlin
package com.baicaohui.lightweb.browser

import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

class ResourceSniffControllerTest {

    @Test
    fun `add ignores when inactive`() = runTest {
        val controller = ResourceSniffController()
        controller.add("https://a.com/v.mp4", "https://a.com/")
        assertEquals(emptyList<SniffedResource>(), controller.resources.value)
    }

    @Test
    fun `start then add collects and dedupes`() = runTest {
        val controller = ResourceSniffController()
        controller.start()
        controller.add("https://a.com/v.mp4", "https://a.com/")
        controller.add("https://a.com/v.mp4", "https://a.com/")
        assertEquals(1, controller.resources.value.size)
        assertEquals(ResourceKind.VIDEO, controller.resources.value[0].kind)
        assertEquals("v.mp4", controller.resources.value[0].name)
    }

    @Test
    fun `add resolves relative urls`() = runTest {
        val controller = ResourceSniffController()
        controller.start()
        controller.add("img/photo.jpg", "https://a.com/page/index.html")
        assertEquals("https://a.com/page/img/photo.jpg", controller.resources.value[0].url)
    }

    @Test
    fun `add ignores unsupported`() = runTest {
        val controller = ResourceSniffController()
        controller.start()
        controller.add("https://a.com/page.html", "https://a.com/")
        assertEquals(emptyList<SniffedResource>(), controller.resources.value)
    }

    @Test
    fun `stop stops collecting`() = runTest {
        val controller = ResourceSniffController()
        controller.start()
        controller.stop()
        controller.add("https://a.com/a.mp3", "https://a.com/")
        assertEquals(emptyList<SniffedResource>(), controller.resources.value)
    }

    @Test
    fun `clear resets list`() = runTest {
        val controller = ResourceSniffController()
        controller.start()
        controller.add("https://a.com/a.png", "https://a.com/")
        controller.clear()
        assertEquals(emptyList<SniffedResource>(), controller.resources.value)
    }

    @Test
    fun `onPageStarted clears while active`() = runTest {
        val controller = ResourceSniffController()
        controller.start()
        controller.add("https://a.com/a.png", "https://a.com/")
        controller.onPageStarted()
        assertEquals(emptyList<SniffedResource>(), controller.resources.value)
        controller.add("https://a.com/b.jpg", "https://a.com/")
        assertEquals(1, controller.resources.value.size)
    }

    @Test
    fun `addDomResult parses evaluateJavascript payload`() = runTest {
        val controller = ResourceSniffController()
        controller.start()
        val raw = """"[{\"u\":\"https://a.com/v.mp4\",\"k\":\"video\"}]""""
        controller.addDomResult(raw, "https://a.com/")
        assertEquals(1, controller.resources.value.size)
        assertEquals("https://a.com/v.mp4", controller.resources.value[0].url)
    }

    @Test
    fun `addDomResult ignores null payload`() = runTest {
        val controller = ResourceSniffController()
        controller.start()
        controller.addDomResult("null", "https://a.com/")
        assertEquals(emptyList<SniffedResource>(), controller.resources.value)
    }
}
```

- [ ] **Step 2: 运行确认失败**（`--tests "com.baicaohui.lightweb.browser.ResourceSniffControllerTest"`，预期编译失败）
- [ ] **Step 3: 最小实现**

```kotlin
package com.baicaohui.lightweb.browser

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

@Serializable
private data class DomResource(val u: String, val k: String)

class ResourceSniffController(
    private val kindOf: (String) -> ResourceKind? = ResourceSniffer::kindOf,
    private val nameFor: (String) -> String = ResourceSniffer::nameFor,
    private val resolve: (String, String) -> String? = ResourceSniffer::resolveUrl,
) {
    private val _resources = MutableStateFlow<List<SniffedResource>>(emptyList())
    val resources: StateFlow<List<SniffedResource>> = _resources.asStateFlow()

    private val seen = linkedSetOf<String>()
    private var active = false

    fun start() {
        active = true
    }

    fun stop() {
        active = false
    }

    fun clear() {
        seen.clear()
        _resources.value = emptyList()
    }

    fun onPageStarted() {
        if (active) clear()
    }

    fun add(url: String, baseUrl: String) {
        if (!active) return
        val normalized = resolve(baseUrl, url) ?: return
        val kind = kindOf(normalized) ?: return
        if (seen.add(normalized)) {
            _resources.value = _resources.value +
                SniffedResource(normalized, kind, nameFor(normalized))
        }
    }

    fun addDomResult(raw: String?, baseUrl: String) {
        if (!active || raw.isNullOrBlank()) return
        val inner = runCatching { Json.decodeFromString<String>(raw) }.getOrNull() ?: return
        val list = runCatching { Json.decodeFromString<List<DomResource>>(inner) }.getOrNull() ?: return
        list.forEach { add(it.u, baseUrl) }
    }
}
```

- [ ] **Step 4: 运行确认通过**

## Task 3：PageContextMenus（TDD）

**Files:**
- Create: `app/src/main/java/com/baicaohui/lightweb/browser/PageContextMenus.kt`
- Test: `app/src/test/java/com/baicaohui/lightweb/browser/PageContextMenusTest.kt`

- [ ] **Step 1: 写失败测试**

```kotlin
package com.baicaohui.lightweb.browser

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

class PageContextMenusTest {

    @Test
    fun `parse selection info from evaluateJavascript payload`() {
        val raw = """"{\"text\":\"hello\",\"left\":10,\"top\":20,\"width\":30,\"height\":40}""""
        val info = PageContextMenus.parseSelectionInfo(raw)
        assertNotNull(info)
        assertEquals("hello", info!!.text)
        assertEquals(10f, info.left)
        assertEquals(20f, info.top)
        assertEquals(30f, info.width)
        assertEquals(40f, info.height)
    }

    @Test
    fun `parse selection info returns null for null payload`() {
        assertNull(PageContextMenus.parseSelectionInfo("null"))
        assertNull(PageContextMenus.parseSelectionInfo(null))
    }

    @Test
    fun `parse text decodes quoted string`() {
        assertEquals("hi", PageContextMenus.parseText(""""hi""""))
    }

    @Test
    fun `parse text returns empty for null`() {
        assertEquals("", PageContextMenus.parseText("null"))
        assertEquals("", PageContextMenus.parseText(null))
    }
}
```

- [ ] **Step 2: 运行确认失败**（`--tests "com.baicaohui.lightweb.browser.PageContextMenusTest"`）
- [ ] **Step 3: 最小实现**

```kotlin
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

    fun selectionTextScript(): String =
        "(function(){ var s = window.getSelection(); return JSON.stringify(s ? s.toString() : ''); })();"

    fun selectAllScript(): String =
        "(function(){ document.execCommand('selectAll'); })();"

    fun linkTextScript(): String = """
        (function() {
          var sel = window.getSelection();
          var node = sel && sel.anchorNode;
          if (node && node.nodeType === 3) node = node.parentNode;
          while (node && node.tagName !== 'A') node = node.parentNode;
          return JSON.stringify(node ? node.textContent : '');
        })();
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
```

- [ ] **Step 4: 运行确认通过**

## Task 4：WebView 集成（嗅探 + 长按拦截）

**Files:**
- Modify: `app/src/main/java/com/baicaohui/lightweb/browser/BrowserWebView.kt`
- Modify: `app/src/main/java/com/baicaohui/lightweb/browser/WebClientPolicy.kt`

- [ ] **Step 1: `WebClientPolicy.kt` 增加资源请求回调**

`BchWebViewClient` 构造参数追加：

```kotlin
private val onResourceRequest: (String) -> Unit = {},
```

`shouldInterceptRequest` 开头（非主框架）追加：

```kotlin
if (!request.isForMainFrame) onResourceRequest(url)
```

- [ ] **Step 2: `BrowserWebView.kt` 增加属性与拦截**

新增 import：`android.view.ActionMode`。

新增成员：

```kotlin
var onLongPressLink: ((url: String) -> Unit)? = null
var onLongPressImage: ((url: String) -> Unit)? = null
var onTextSelection: (() -> Unit)? = null

val resourceSniffing = ResourceSniffController()
```

`init` 中 `BchWebViewClient(...)` 传参追加：

```kotlin
onResourceRequest = { url -> resourceSniffing.add(url, this.url) },
```

新增方法：

```kotlin
fun startResourceSniffing() = resourceSniffing.start()

fun stopResourceSniffing() = resourceSniffing.stop()

fun clearSniffedResources() = resourceSniffing.clear()

fun scanPageResources() {
    val base = url ?: return
    evaluateJavascript(ResourceSniffer.domScanScript()) { raw ->
        resourceSniffing.addDomResult(raw, base)
    }
}

override fun startActionMode(callback: ActionMode.Callback?): ActionMode? =
    interceptActionMode(callback, ActionMode.TYPE_FLOATING)

override fun startActionMode(callback: ActionMode.Callback?, type: Int): ActionMode? =
    interceptActionMode(callback, type)

private fun interceptActionMode(callback: ActionMode.Callback?, type: Int): ActionMode? {
    if (callback == null) return null
    if (type != ActionMode.TYPE_FLOATING) return super.startActionMode(callback, type)
    return when (hitTestResult.type) {
        WebView.HitTestResult.SRC_ANCHOR_TYPE -> {
            hitTestResult.extra?.takeIf { it.isNotBlank() }?.let(onLongPressLink)
            null
        }
        WebView.HitTestResult.IMAGE_TYPE,
        WebView.HitTestResult.SRC_IMAGE_ANCHOR_TYPE -> {
            hitTestResult.extra?.takeIf { it.isNotBlank() }?.let(onLongPressImage)
            null
        }
        WebView.HitTestResult.UNKNOWN_TYPE -> {
            onTextSelection?.invoke()
            null
        }
        else -> super.startActionMode(callback, type)
    }
}
```

- [ ] **Step 3: 编译验证**

```powershell
$env:GRADLE_USER_HOME = "D:\gradle-home"; $env:JAVA_HOME = "C:\Program Files\Android\Android Studio\jbr"; $env:Path = "$env:JAVA_HOME\bin;$env:Path"; .\gradlew.bat :app:compileDebugKotlin
```

预期 BUILD SUCCESSFUL。

## Task 5：菜单、路由与文案

**Files:**
- Modify: `ui/navigation/BchRoute.kt`
- Modify: `ui/browser/MenuItems.kt`
- Modify: `ui/browser/MenuOrder.kt`
- Modify: `res/values/strings.xml`

- [ ] **Step 1: 路由**

`BchRoute` 增加：

```kotlin
SNIFFER("sniffer", R.string.menu_sniffer, Icons.Filled.Radar, false),
```

- [ ] **Step 2: 菜单**

`MenuItems.SPECS` 在 `downloads` 后增加：

```kotlin
Spec("sniffer", R.string.menu_sniffer, Icons.Filled.Radar),
```

`MenuOrder.DEFAULT_ORDER` 在 `downloads` 后增加 `"sniffer"`。

- [ ] **Step 3: 文案**

`strings.xml` 增加：

```xml
<string name="menu_sniffer">资源嗅探</string>
<string name="sniffer_title">资源嗅探</string>
<string name="sniffer_refresh">重新扫描</string>
<string name="sniffer_empty">未发现可嗅探的资源</string>
<string name="action_download">下载</string>
<string name="context_copy">复制</string>
<string name="context_select_all">全选</string>
<string name="context_search_new_tab">在新标签页中搜索</string>
<string name="context_open_new_tab">在新标签页中开启</string>
<string name="context_open_incognito">在无痕模式标签页中开启</string>
<string name="context_copy_link_address">复制链接地址</string>
<string name="context_copy_link_text">复制链接文字</string>
<string name="context_download_link">下载链接</string>
<string name="context_share_link">分享链接</string>
<string name="context_open_image">在新标签页中打开图像</string>
<string name="context_copy_image">复制图像</string>
<string name="context_download_image">下载图像</string>
<string name="context_copied">已复制</string>
<string name="context_image_copied">已复制图像</string>
<string name="context_image_copy_failed">复制图像失败</string>
<string name="context_share_title">分享链接</string>
```

## Task 6：嗅探结果页

**Files:**
- Create: `app/src/main/java/com/baicaohui/lightweb/ui/sniffer/ResourceSniffScreen.kt`
- Modify: `ui/BchAppRoot.kt`
- Modify: `ui/IncognitoAppRoot.kt`

- [ ] **Step 1: 页面实现**（完整文件）

```kotlin
package com.baicaohui.lightweb.ui.sniffer

import android.widget.Toast
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AudioFile
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.baicaohui.lightweb.BchApp
import com.baicaohui.lightweb.R
import com.baicaohui.lightweb.browser.DownloadHandler
import com.baicaohui.lightweb.browser.ResourceKind
import com.baicaohui.lightweb.browser.ResourceSniffer
import com.baicaohui.lightweb.browser.SniffedResource
import com.baicaohui.lightweb.data.prefs.DownloadMode

@Composable
fun ResourceSniffScreen(
    tabId: Long?,
    onBack: () -> Unit,
) {
    val context = LocalContext.current
    val app = context.applicationContext as BchApp
    val wv = remember(tabId) { tabId?.let { app.webViewStore.get(it) } }
    val controller = wv?.resourceSniffing
    val resources = controller?.resources
        ?.collectAsStateWithLifecycle(initialValue = emptyList())
        ?.value
        ?: emptyList()
    val downloadHandler = remember { DownloadHandler(context) }

    LaunchedEffect(wv) {
        val view = wv ?: return@LaunchedEffect
        val sniff = view.resourceSniffing
        sniff.start()
        sniff.clear()
        view.scanPageResources()
    }
    DisposableEffect(wv) {
        onDispose { wv?.resourceSniffing?.stop() }
    }

    fun download(resource: SniffedResource) {
        val view = wv ?: return
        val ua = view.settings.userAgentString
        val mime = ResourceSniffer.mimeFor(resource.kind, resource.url)
        if (app.currentBrowserPrefs.downloadMode == DownloadMode.SYSTEM) {
            downloadHandler.start(resource.url, ua, mime)
        } else {
            app.appDownloadManager.enqueue(resource.url, ua, mime, null)
        }
        Toast.makeText(context, R.string.download_started, Toast.LENGTH_SHORT).show()
    }

    Column(modifier = Modifier.fillMaxSize()) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp, vertical = 4.dp),
        ) {
            IconButton(onClick = onBack) {
                Icon(
                    Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = stringResource(R.string.action_back),
                )
            }
            Text(
                text = stringResource(R.string.sniffer_title),
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.weight(1f),
            )
            IconButton(onClick = {
                controller?.clear()
                wv?.scanPageResources()
            }) {
                Icon(
                    Icons.Filled.Refresh,
                    contentDescription = stringResource(R.string.sniffer_refresh),
                )
            }
        }
        if (resources.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(
                    text = stringResource(R.string.sniffer_empty),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        } else {
            LazyColumn(modifier = Modifier.fillMaxSize()) {
                items(resources, key = { it.url }) { resource ->
                    ListItem(
                        leadingContent = { ResourcePreview(resource) },
                        headlineContent = {
                            Text(resource.name, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        },
                        supportingContent = {
                            Text(resource.url, maxLines = 2, overflow = TextOverflow.Ellipsis)
                        },
                        trailingContent = {
                            IconButton(onClick = { download(resource) }) {
                                Icon(
                                    Icons.Filled.FileDownload,
                                    contentDescription = stringResource(R.string.action_download),
                                )
                            }
                        },
                    )
                }
            }
        }
    }
}

@Composable
private fun ResourcePreview(resource: SniffedResource) {
    if (resource.kind == ResourceKind.IMAGE) {
        AsyncImage(
            model = resource.url,
            contentDescription = resource.name,
            contentScale = ContentScale.Crop,
            modifier = Modifier.size(48.dp).clip(RoundedCornerShape(8.dp)),
        )
    } else {
        val icon: ImageVector = when (resource.kind) {
            ResourceKind.VIDEO -> Icons.Filled.Movie
            ResourceKind.AUDIO -> Icons.Filled.AudioFile
            ResourceKind.IMAGE -> Icons.Filled.Image
        }
        Icon(
            imageVector = icon,
            contentDescription = resource.name,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(48.dp).padding(12.dp),
        )
    }
}
```

- [ ] **Step 2: `BchAppRoot.kt` 接线**

`menuItemsById` 增加：

```kotlin
"sniffer" to MoreMenuItem(
    id = "sniffer",
    label = stringResource(R.string.menu_sniffer),
    icon = Icons.Filled.Radar,
    enabled = currentTab?.url?.isNotBlank() == true,
    onClick = {
        menuOpen = false
        navigateTo(BchRoute.SNIFFER.route)
    },
),
```

NavHost 增加：

```kotlin
composable(BchRoute.SNIFFER.route) {
    ResourceSniffScreen(tabId = currentTabId, onBack = { navController.popBackStack() })
}
```

import `com.baicaohui.lightweb.ui.sniffer.ResourceSniffScreen`、`Icons.Filled.Radar`。

- [ ] **Step 3: `IncognitoAppRoot.kt` 接线**

新增 `private const val SCREEN_SNIFFER = "sniffer"`；`sharedMenuItemsById` 增加 `sniffer`（跳 `screen = SCREEN_SNIFFER`）；`when(screen)` 增加 `SCREEN_SNIFFER -> ResourceSniffScreen(tabId = currentId, onBack = { goBrowser() })`；import 同上。

- [ ] **Step 4: 编译验证**（`:app:compileDebugKotlin`）

## Task 7：长按弹窗 UI 与动作

**Files:**
- Create: `app/src/main/java/com/baicaohui/lightweb/ui/browser/LongPressMenus.kt`
- Modify: `app/src/main/java/com/baicaohui/lightweb/ui/browser/BrowserScreen.kt`

- [ ] **Step 1: 弹窗组件**

`LongPressMenus.kt` 完整实现（含 `TextSelectionPopup`、`LinkContextDialog`、`ImageContextDialog`、`MenuActionItem`），核心结构：

- `TextSelectionPopup(info, density, onCopy, onSelectAll, onSearch, onDismiss)`：`Popup(alignment = TopStart, offset = IntOffset(x, y))`，三项 `MenuActionItem`。
- `LinkContextDialog(url, linkText, ...)`：`Dialog` + `Surface`；头部 `Row` = 左侧 `Icon(Icons.Filled.Link)`，右侧 `Text(linkText.ifBlank { url })` + `Text(url)`；六项动作。
- `ImageContextDialog(url, name, ...)`：头部 `AsyncImage(64.dp)` + 名字/URL；三项动作。
- `MenuActionItem(icon, labelRes, onClick)`：`ListItem` + `clickable`。

- [ ] **Step 2: `BrowserScreen.kt` 状态与动作**

新增 import：`FileProvider`、`ClipData`、`ClipboardManager`、`Context`、`File`、`Dispatchers`、`withContext`、`HttpDownloader`、`BrowserWebView`、`ResourceSniffer`、`PageContextMenus`、`SelectionInfo`、`IncognitoActivity`、`LocalDensity`、三个弹窗组件、`roundToInt`。

状态：

```kotlin
var textMenu by remember { mutableStateOf<SelectionInfo?>(null) }
var linkMenu by remember { mutableStateOf<LinkMenuState?>(null) }
var imageMenu by remember { mutableStateOf<ImageMenuState?>(null) }
val density = LocalDensity.current.density
```

私有数据类：

```kotlin
private data class LinkMenuState(val url: String, val text: String)
private data class ImageMenuState(val url: String, val name: String)
```

动作函数：

```kotlin
fun currentWv(): BrowserWebView? = viewModel.currentId.value?.let { webViewStore.get(it) }

fun copySelectionText() {
    val wv = currentWv() ?: return
    wv.evaluateJavascript(PageContextMenus.selectionTextScript()) { raw ->
        val text = PageContextMenus.parseText(raw)
        if (text.isNotBlank()) {
            clipboard.setText(AnnotatedString(text))
            Toast.makeText(context, R.string.context_copied, Toast.LENGTH_SHORT).show()
        }
        textMenu = null
    }
}

fun selectAllText() {
    val wv = currentWv() ?: return
    wv.evaluateJavascript(PageContextMenus.selectAllScript()) {
        wv.evaluateJavascript(PageContextMenus.selectionInfoScript()) { raw ->
            textMenu = PageContextMenus.parseSelectionInfo(raw)
        }
    }
}

fun searchSelectionInNewTab() {
    val wv = currentWv() ?: return
    wv.evaluateJavascript(PageContextMenus.selectionTextScript()) { raw ->
        val text = PageContextMenus.parseText(raw)
        if (text.isNotBlank()) {
            app.tabManager.newTab(UrlSecurity.toSearchUrl(text, browserPrefs.searchTemplate))
        }
        textMenu = null
    }
}

fun openInNewTab(url: String) {
    app.tabManager.newTab(url)
}

fun openInIncognito(url: String) {
    context.startActivity(
        Intent(context, IncognitoActivity::class.java)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            .putExtra(IncognitoActivity.EXTRA_URL, url),
    )
}

fun copyText(text: String, toastRes: Int) {
    if (text.isNotBlank()) {
        clipboard.setText(AnnotatedString(text))
        Toast.makeText(context, toastRes, Toast.LENGTH_SHORT).show()
    }
}

fun downloadFromMenu(url: String) {
    pendingDownload = DownloadRequest(
        url = url,
        userAgent = currentWv()?.settings?.userAgentString ?: BrowserWebView.ANDROID_UA,
        mimeType = null,
        contentDisposition = null,
    )
}

fun shareLink(url: String) {
    runCatching {
        context.startActivity(
            Intent.createChooser(
                Intent(Intent.ACTION_SEND).apply {
                    type = "text/plain"
                    putExtra(Intent.EXTRA_TEXT, url)
                },
                context.getString(R.string.context_share_title),
            ),
        )
    }
}

fun copyImage(url: String, name: String) {
    val ua = currentWv()?.settings?.userAgentString ?: BrowserWebView.ANDROID_UA
    scope.launch(Dispatchers.IO) {
        val dir = File(context.filesDir, "clipboard").apply { mkdirs() }
        val file = File(dir, name.replace(Regex("""[\\/:*?"<>|]"""), "_"))
        try {
            file.outputStream().use { out ->
                HttpDownloader.download(url, ua, out) { _, _ -> }
            }
            val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
            val clip = ClipData.newUri(context.contentResolver, "image", uri)
            withContext(Dispatchers.Main) {
                (context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager)
                    .setPrimaryClip(clip)
                Toast.makeText(context, R.string.context_image_copied, Toast.LENGTH_SHORT).show()
            }
        } catch (_: Exception) {
            withContext(Dispatchers.Main) {
                Toast.makeText(context, R.string.context_image_copy_failed, Toast.LENGTH_SHORT).show()
            }
        }
    }
}
```

- [ ] **Step 3: WebView 监听器**

`AndroidView` 的 `update` 中追加：

```kotlin
wv.onLongPressLink = { url ->
    wv.evaluateJavascript(PageContextMenus.linkTextScript()) { raw ->
        linkMenu = LinkMenuState(url, PageContextMenus.parseText(raw))
    }
}
wv.onLongPressImage = { url ->
    imageMenu = ImageMenuState(url, ResourceSniffer.nameFor(url))
}
wv.onTextSelection = {
    wv.evaluateJavascript(PageContextMenus.selectionInfoScript()) { raw ->
        textMenu = PageContextMenus.parseSelectionInfo(raw)
    }
}
```

`tabCallbacks.onPageStarted` 与 `LaunchedEffect(activeTab?.id)` 中清空三个菜单状态；`BackHandler(enabled = textMenu != null) { textMenu = null }`。

- [ ] **Step 4: 渲染弹窗**

`Column` 内（`pendingHttpsBlock` 之后）追加：

```kotlin
textMenu?.let { info ->
    TextSelectionPopup(
        info = info,
        density = density,
        onCopy = ::copySelectionText,
        onSelectAll = ::selectAllText,
        onSearch = ::searchSelectionInNewTab,
        onDismiss = { textMenu = null },
    )
}
linkMenu?.let { menu ->
    LinkContextDialog(
        url = menu.url,
        linkText = menu.text,
        onOpenNewTab = { openInNewTab(menu.url); linkMenu = null },
        onOpenIncognito = { openInIncognito(menu.url); linkMenu = null },
        onCopyAddress = { copyText(menu.url, R.string.context_copied); linkMenu = null },
        onCopyText = { copyText(menu.text, R.string.context_copied); linkMenu = null },
        onDownload = { downloadFromMenu(menu.url); linkMenu = null },
        onShare = { shareLink(menu.url); linkMenu = null },
        onDismiss = { linkMenu = null },
    )
}
imageMenu?.let { menu ->
    ImageContextDialog(
        url = menu.url,
        name = menu.name,
        onOpenNewTab = { openInNewTab(menu.url); imageMenu = null },
        onCopy = { copyImage(menu.url, menu.name); imageMenu = null },
        onDownload = { downloadFromMenu(menu.url); imageMenu = null },
        onDismiss = { imageMenu = null },
    )
}
```

- [ ] **Step 5: 编译验证**（`:app:compileDebugKotlin`）

## Task 8：全量验证

- [ ] **Step 1: 全量单测**

```powershell
$env:GRADLE_USER_HOME = "D:\gradle-home"; $env:JAVA_HOME = "C:\Program Files\Android\Android Studio\jbr"; $env:Path = "$env:JAVA_HOME\bin;$env:Path"; .\gradlew.bat :app:testDebugUnitTest
```

预期：原有 134 个 + 新增用例全部通过。

- [ ] **Step 2: Debug 构建**

```powershell
$env:GRADLE_USER_HOME = "D:\gradle-home"; $env:JAVA_HOME = "C:\Program Files\Android\Android Studio\jbr"; $env:Path = "$env:JAVA_HOME\bin;$env:Path"; .\gradlew.bat :app:assembleDebug
```

预期：`app/build/outputs/apk/debug/app-debug.apk` 生成，exit 0。

## 验收清单

- [ ] 三杠菜单含“资源嗅探”，打开后列出 mp4/flv/webm/m3u8、mp3/m4a/ogg/wav、jpg/png/gif/svg/webp
- [ ] 列表项有图片缩略图（图片类）、名字（带扩展名）、URL、右侧下载按钮
- [ ] 长按文字：选区处弹窗，可复制/全选/新标签页搜索
- [ ] 长按链接：居中弹窗，图标 + 链接文字 + 链接地址，六个动作齐全
- [ ] 长按图片：居中弹窗，小图 + 图片名 + 图片地址，三个动作齐全
- [ ] 无痕模式同样可打开嗅探页

---

## 追加需求（2026-08-07）：文件大小与下载控制

**Goal:** 嗅探列表与下载确认弹窗尽量显示文件大小；下载管理支持实时进度/速度、单条暂停/继续、全部暂停。
**Architecture:** 大小用 HEAD 请求取 `Content-Length`；下载暂停=协程取消并保留分片，继续=`Range: bytes=N-` 追加续传；速度在内存 `StateFlow` 中按字节/时间差计算，UI 合并 Room 数据展示。

### 文件结构
- 新建：`browser/DownloadFormat.kt`（formatBytes/formatSpeed，纯 Kotlin）；`browser/DownloadFormatTest.kt`
- 修改：`browser/HttpDownloader.kt`（HEAD contentLength + downloadResumable 断点续传）、`browser/DownloadStatus.kt`（PAUSED）、`browser/AppDownloadManager.kt`（任务表、暂停/继续/全部暂停、liveProgress）、`browser/ResourceSniffer.kt`（SniffedResource.sizeBytes）、`browser/ResourceSniffController.kt`（updateSize）、`data/db/Daos.kt`/`data/repo/DownloadRepository.kt`（DownloadStore.get）
- 修改 UI：`ui/sniffer/ResourceSniffScreen.kt`（HEAD 拉大小并显示）、`ui/downloads/DownloadsScreen.kt`（进度/速度/暂停按钮/全部暂停）、`ui/browser/BrowserScreen.kt`（确认弹窗显示大小）、`strings.xml`
- 测试：`HttpDownloaderTest`（HEAD/206/200 续传）、`AppDownloadManagerTest`（暂停保留分片、继续追加、全部暂停、liveProgress）、`ResourceSniffControllerTest`（updateSize）

### 关键设计
1. `AppDownloadManager` 注入的下载器签名改为 `(url, ua, file, startOffset, onProgress) -> Long`，默认实现走 `HttpDownloader.downloadResumable`；`startOffset>0` 时带 Range，服务端返回 200 则截断重下。
2. 暂停：`jobs[id].cancel()`，协程捕获 `CancellationException` 后把实体置 `PAUSED`、`downloadedBytes=file.length()`，保留分片；继续：按实体 fileName 打开同一文件，从文件长度续传。
3. 速度：progress 回调中用 `System.nanoTime()` 差值计算，写入 `liveProgress: StateFlow<Map<Long, LiveDownloadProgress>>`，下载结束/暂停时移除。
4. 全部暂停：遍历任务表逐个 cancel。
5. 嗅探大小：`ResourceSniffScreen` 对新增资源发起 HEAD，成功后 `controller.updateSize(url, size)` 更新列表项。

### 验证
- `:app:testDebugUnitTest` 全绿（新增用例先红后绿）
- `:app:assembleDebug` 成功
- 模拟器：嗅探列表显示大小；下载确认弹窗显示大小；下载管理实时进度/速度、单条暂停/继续、全部暂停
