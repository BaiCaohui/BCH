# BCH 阅读模式 + 全文离线缓存 实现计划

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 为 BCH 浏览器加入三杠菜单开关的阅读模式（Readability 提取、字号/主题调节）与全文离线缓存（Room 缓存，离线自动展示）。

**Architecture:** 纯 Kotlin `ReaderPage` 生成注入脚本/离线页 HTML；`ReaderModeController` 薄封装 WebView 交互；Room `reader_cache` 表存全文；`Tab` 增加 `readerMode`/`readerOffline` 会话状态；菜单项注册进 `MenuItems`/`MenuOrder`。

**Tech Stack:** Kotlin + Compose + Room + kotlinx.serialization；Mozilla Readability 0.6.0（Apache-2.0，`assets/reader/`）。

---

## 文件结构

### 新建
- `app/src/main/assets/reader/Readability.js`：第三方库（下载 0.6.0，保留许可头）
- `app/src/main/java/com/baicaohui/lightweb/browser/ReaderPage.kt`：脚本生成、结果解析、离线页渲染（纯 Kotlin）
- `app/src/main/java/com/baicaohui/lightweb/browser/ReaderModeController.kt`：WebView 交互薄封装
- `app/src/main/java/com/baicaohui/lightweb/data/repo/ReaderCacheRepository.kt`：`ReaderCacheStore` 接口 + Room 实现
- 测试：`app/src/test/java/com/baicaohui/lightweb/browser/ReaderPageTest.kt`

### 修改
- `app/src/main/java/com/baicaohui/lightweb/data/db/Entities.kt`：`ReaderCacheEntity`
- `app/src/main/java/com/baicaohui/lightweb/data/db/Daos.kt`：`ReaderCacheDao`
- `app/src/main/java/com/baicaohui/lightweb/data/db/AppDatabase.kt`：version 4 + `MIGRATION_3_4`
- `app/src/main/java/com/baicaohui/lightweb/browser/TabManager.kt`：`Tab.readerMode`/`readerOffline`
- `app/src/main/java/com/baicaohui/lightweb/ui/browser/BrowserViewModel.kt`：toggle/set/reset + 事件
- `app/src/main/java/com/baicaohui/lightweb/ui/browser/MenuItems.kt`、`MenuOrder.kt`：`reader` 项
- `app/src/main/java/com/baicaohui/lightweb/ui/BchAppRoot.kt`、`ui/IncognitoAppRoot.kt`：菜单接线
- `app/src/main/java/com/baicaohui/lightweb/ui/browser/BrowserScreen.kt`：事件处理、离线自动加载、导航守卫
- `app/src/main/java/com/baicaohui/lightweb/BchApp.kt`：`readerCacheRepository`/`readabilityJs`
- `app/src/main/java/com/baicaohui/lightweb/ui/settings/OtherSettingsScreens.kt`：清缓存时清 `reader_cache`
- `app/src/main/res/values/strings.xml`：菜单/Toast/离线徽标文案
- 测试：`BrowserViewModelTest`、`TabManagerTest`、`MenuOrderTest`

## 环境命令（每轮 TDD 前设置）

```powershell
$env:GRADLE_USER_HOME = "D:\gradle-home"
$env:JAVA_HOME = "C:\Program Files\Android\Android Studio\jbr"
$env:Path = "$env:JAVA_HOME\bin;$env:Path"
```

---

## Task 1：ReaderPage 纯逻辑（TDD）

**Files:**
- Create: `app/src/test/java/com/baicaohui/lightweb/browser/ReaderPageTest.kt`
- Create: `app/src/main/java/com/baicaohui/lightweb/browser/ReaderPage.kt`

### Step 1：写失败测试

`ReaderPageTest.kt`：

```kotlin
package com.baicaohui.lightweb.browser

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonPrimitive
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ReaderPageTest {

    private val json = Json { ignoreUnknownKeys = true }

    private fun jsResult(payload: String): String =
        json.encodeToString(JsonPrimitive.serializer(), JsonPrimitive(payload))

    @Test
    fun `enter script embeds readability source and initial theme`() {
        val script = ReaderPage.enterScript("var marker = 42;", "dark")
        assertTrue(script.contains("var marker = 42;"))
        assertTrue(script.contains("\"dark\""))
        assertTrue(script.contains("new Readability("))
    }

    @Test
    fun `enter script keeps dollar template literals intact`() {
        val source = "var s = `\${x}`;"
        val script = ReaderPage.enterScript(source, "light")
        assertTrue(script.contains("var s = `\${x}`;"))
    }

    @Test
    fun `parse result extracts article`() {
        val payload = """{"ok":true,"title":"标题","byline":"作者","content":"<p>正文</p>"}"""
        val article = ReaderPage.parseResult(jsResult(payload))
        assertEquals("标题", article?.title)
        assertEquals("作者", article?.byline)
        assertEquals("<p>正文</p>", article?.contentHtml)
    }

    @Test
    fun `parse result null when not ok`() {
        val payload = """{"ok":false,"reason":"no-content"}"""
        assertNull(ReaderPage.parseResult(jsResult(payload)))
    }

    @Test
    fun `parse result null when content blank`() {
        val payload = """{"ok":true,"title":"t","byline":"","content":""}"""
        assertNull(ReaderPage.parseResult(jsResult(payload)))
    }

    @Test
    fun `parse result null on garbage`() {
        assertNull(ReaderPage.parseResult("not json"))
        assertNull(ReaderPage.parseResult(null))
    }

    @Test
    fun `parse exit true when ok`() {
        assertTrue(ReaderPage.parseExit(jsResult("""{"ok":true}""")))
        assertFalse(ReaderPage.parseExit(jsResult("""{"ok":false}""")))
        assertFalse(ReaderPage.parseExit(null))
    }

    @Test
    fun `exit script restores body and scroll`() {
        val script = ReaderPage.exitScript()
        assertTrue(script.contains("__bchReaderState"))
        assertTrue(script.contains("scrollTo"))
        assertTrue(script.contains("bodyChildren"))
    }

    @Test
    fun `offline html escapes text but keeps content html`() {
        val html = ReaderPage.offlineHtml(
            url = "https://a.com/x",
            title = "<b>标题</b> & \"引号\"",
            byline = "作者 <script>",
            contentHtml = "<p>正文 <img src='x.png'></p>",
            theme = "sepia",
            offlineBadge = "离线缓存",
        )
        assertTrue(html.contains("&lt;b&gt;标题&lt;/b&gt; &amp; &quot;引号&quot;"))
        assertTrue(html.contains("作者 &lt;script&gt;"))
        assertTrue(html.contains("<p>正文 <img src='x.png'></p>"))
        assertTrue(html.contains("data-theme=\"sepia\""))
        assertTrue(html.contains("离线缓存"))
        assertTrue(html.contains("data-bch-font=\"-1\""))
    }

    @Test
    fun `offline html sanitizes unknown theme`() {
        val html = ReaderPage.offlineHtml("https://a.com", "t", "", "<p>x</p>", "neon", "b")
        assertTrue(html.contains("data-theme=\"light\""))
    }

    @Test
    fun `html escape handles all special chars`() {
        assertEquals("&amp;&lt;&gt;&quot;&#39;", ReaderPage.htmlEscape("&<>\"'"))
    }
}
```

### Step 2：运行确认失败

```powershell
.\gradlew.bat :app:testDebugUnitTest --tests "com.baicaohui.lightweb.browser.ReaderPageTest" -i
```
预期：编译失败（`ReaderPage` 不存在）。

### Step 3：实现 `ReaderPage.kt`

```kotlin
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

    private const val READER_CSS = """
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

    private const val READER_JS = """
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

    private const val ENTER_TEMPLATE = """
        (function() {
          if (window.__bchReaderState) {
            return JSON.stringify({ok:false, reason:'already-active'});
          }
          /*__BCH_READABILITY_SOURCE__*/
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

    private const val EXIT_TEMPLATE = """
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

    private const val OFFLINE_TEMPLATE = """
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
        url: String,
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
```

### Step 4：运行确认通过

```powershell
.\gradlew.bat :app:testDebugUnitTest --tests "com.baicaohui.lightweb.browser.ReaderPageTest"
```
预期：8 个测试全部 PASS。

### Step 5：Commit（按项目惯例暂不自动提交，改为最后统一说明）

---

## Task 2：Room 缓存表 + Repository

**Files:**
- Modify: `app/src/main/java/com/baicaohui/lightweb/data/db/Entities.kt`
- Modify: `app/src/main/java/com/baicaohui/lightweb/data/db/Daos.kt`
- Modify: `app/src/main/java/com/baicaohui/lightweb/data/db/AppDatabase.kt`
- Create: `app/src/main/java/com/baicaohui/lightweb/data/repo/ReaderCacheRepository.kt`
- Modify: `app/src/main/java/com/baicaohui/lightweb/BchApp.kt`

### Step 1：实现（Room 无 JVM 单测，靠编译 + 模拟器验证）

`Entities.kt` 追加：

```kotlin
@Entity(tableName = "reader_cache")
data class ReaderCacheEntity(
    @PrimaryKey val url: String,
    val title: String,
    val byline: String = "",
    val contentHtml: String,
    val savedAt: Long = System.currentTimeMillis(),
)
```

`Daos.kt` 追加：

```kotlin
@Dao
interface ReaderCacheDao {
    @Query("SELECT * FROM reader_cache WHERE url = :url LIMIT 1")
    suspend fun get(url: String): ReaderCacheEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(entity: ReaderCacheEntity)

    @Query("DELETE FROM reader_cache WHERE url = :url")
    suspend fun delete(url: String)

    @Query("DELETE FROM reader_cache")
    suspend fun clear()
}
```

`AppDatabase.kt`：

- `version = 4`，entities 列表加 `ReaderCacheEntity::class`，新增 `abstract fun readerCacheDao(): ReaderCacheDao`。
- 新增并注册：

```kotlin
private val MIGRATION_3_4 = object : Migration(3, 4) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS `reader_cache` (
                `url` TEXT NOT NULL PRIMARY KEY,
                `title` TEXT NOT NULL,
                `byline` TEXT NOT NULL,
                `contentHtml` TEXT NOT NULL,
                `savedAt` INTEGER NOT NULL
            )
            """.trimIndent(),
        )
    }
}
```

`.addMigrations(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4)`

`ReaderCacheRepository.kt`：

```kotlin
package com.baicaohui.lightweb.data.repo

import com.baicaohui.lightweb.data.db.ReaderCacheDao
import com.baicaohui.lightweb.data.db.ReaderCacheEntity

interface ReaderCacheStore {
    suspend fun get(url: String): ReaderCacheEntity?
    suspend fun put(entity: ReaderCacheEntity)
    suspend fun delete(url: String)
    suspend fun clear()
}

class ReaderCacheRepository(private val dao: ReaderCacheDao) : ReaderCacheStore {
    override suspend fun get(url: String): ReaderCacheEntity? = dao.get(url)
    override suspend fun put(entity: ReaderCacheEntity) = dao.upsert(entity)
    override suspend fun delete(url: String) = dao.delete(url)
    override suspend fun clear() = dao.clear()
}
```

`BchApp.kt` 追加：

```kotlin
val readerCacheRepository by lazy { ReaderCacheRepository(database.readerCacheDao()) }

val readabilityJs: String by lazy {
    assets.open("reader/Readability.js").bufferedReader(Charsets.UTF_8).use { it.readText() }
}
```

### Step 2：编译验证

```powershell
.\gradlew.bat :app:compileDebugKotlin
```
预期：BUILD SUCCESSFUL。

---

## Task 3：下载 Readability.js 资产

**Files:**
- Create: `app/src/main/assets/reader/Readability.js`

### Step 1：下载

```powershell
New-Item -ItemType Directory -Force "app\src\main\assets\reader" | Out-Null
Invoke-WebRequest -Uri "https://raw.githubusercontent.com/mozilla/readability/0.6.0/Readability.js" -OutFile "app\src\main\assets\reader\Readability.js"
```

### Step 2：校验

```powershell
Get-Content "app\src\main\assets\reader\Readability.js" -TotalCount 30
Select-String -Path "app\src\main\assets\reader\Readability.js" -Pattern "global.Readability|module.exports" | Select-Object -Last 5
```
预期：文件头含 Apache-2.0 许可注释，末尾有浏览器全局导出。

---

## Task 4：Tab 状态 + ViewModel（TDD）

**Files:**
- Modify: `app/src/main/java/com/baicaohui/lightweb/browser/TabManager.kt`
- Modify: `app/src/main/java/com/baicaohui/lightweb/ui/browser/BrowserViewModel.kt`
- Modify: `app/src/test/java/com/baicaohui/lightweb/ui/browser/BrowserViewModelTest.kt`
- Modify: `app/src/test/java/com/baicaohui/lightweb/browser/TabManagerTest.kt`

### Step 1：写失败测试

`BrowserViewModelTest.kt` 追加：

```kotlin
@Test
fun `toggle reader emits enter when not active`() = runTest {
    val vm = newViewModel()
    val tab = vm.newTab("https://a.com")
    val events = mutableListOf<BrowserEvent>()
    val job = launch { vm.events.collect { events += it } }
    vm.toggleReaderMode()
    dispatcher.scheduler.advanceUntilIdle()
    assertEquals(listOf<BrowserEvent>(BrowserEvent.EnterReader), events)
    assertEquals(false, vm.tabs.first().first { it.id == tab.id }.readerMode)
    job.cancel()
}

@Test
fun `toggle reader emits exit when active`() = runTest {
    val vm = newViewModel()
    val tab = vm.newTab("https://a.com")
    vm.setReaderMode(tab.id, true)
    val events = mutableListOf<BrowserEvent>()
    val job = launch { vm.events.collect { events += it } }
    vm.toggleReaderMode()
    dispatcher.scheduler.advanceUntilIdle()
    assertEquals(listOf<BrowserEvent>(BrowserEvent.ExitReader), events)
    job.cancel()
}

@Test
fun `toggle reader ignores blank url`() = runTest {
    val vm = newViewModel()
    vm.newTab("")
    val events = mutableListOf<BrowserEvent>()
    val job = launch { vm.events.collect { events += it } }
    vm.toggleReaderMode()
    dispatcher.scheduler.advanceUntilIdle()
    assertEquals(emptyList<BrowserEvent>(), events)
    job.cancel()
}

@Test
fun `page started resets reader flags`() = runTest {
    val vm = newViewModel()
    val tab = vm.newTab("https://a.com")
    vm.setReaderMode(tab.id, true)
    vm.setReaderOffline(tab.id, true)
    vm.onPageStarted(tab.id, "https://b.com")
    val updated = vm.tabs.first().first { it.id == tab.id }
    assertEquals(false, updated.readerMode)
    assertEquals(false, updated.readerOffline)
    assertEquals("https://b.com", updated.url)
}
```

`TabManagerTest.kt` 追加：

```kotlin
@Test
fun `restored snapshot resets reader flags`() {
    val manager = TabManager()
    val tab = manager.newTab("https://a.com")
    manager.update(tab.id) { it.copy(readerMode = true, readerOffline = true) }
    val restored = TabManager()
    restored.restore(manager.snapshots())
    val tab2 = restored.tabs.value.single()
    assertEquals(false, tab2.readerMode)
    assertEquals(false, tab2.readerOffline)
}
```

### Step 2：运行确认失败

```powershell
.\gradlew.bat :app:testDebugUnitTest --tests "com.baicaohui.lightweb.ui.browser.BrowserViewModelTest" --tests "com.baicaohui.lightweb.browser.TabManagerTest"
```
预期：编译失败（字段/方法/事件不存在）。

### Step 3：实现

`TabManager.kt` 的 `Tab`：

```kotlin
data class Tab(
    val id: Long,
    val url: String = "",
    val title: String = "",
    val status: TabStatus = TabStatus.EMPTY,
    val progress: Int = 0,
    val readerMode: Boolean = false,
    val readerOffline: Boolean = false,
    val createdAt: Long = System.currentTimeMillis(),
)
```

`BrowserViewModel.kt`：

- sealed interface 追加：

```kotlin
data object EnterReader : BrowserEvent
data object ExitReader : BrowserEvent
```

- 新方法：

```kotlin
fun toggleReaderMode() {
    val id = tabManager.currentId.value ?: return
    val tab = tabManager.tabs.value.firstOrNull { it.id == id } ?: return
    if (tab.url.isBlank()) return
    emit(if (tab.readerMode) BrowserEvent.ExitReader else BrowserEvent.EnterReader)
}

fun setReaderMode(tabId: Long, enabled: Boolean) =
    tabManager.update(tabId) { it.copy(readerMode = enabled) }

fun setReaderOffline(tabId: Long, enabled: Boolean) =
    tabManager.update(tabId) { it.copy(readerOffline = enabled) }

fun onOfflineCacheLoaded(tabId: Long) =
    tabManager.update(tabId) { it.copy(status = TabStatus.READY, progress = 100) }
```

- `onPageStarted` 的 copy 追加：

```kotlin
it.copy(url = url, status = TabStatus.LOADING, progress = 10, readerMode = false, readerOffline = false)
```

### Step 4：运行确认通过

```powershell
.\gradlew.bat :app:testDebugUnitTest --tests "com.baicaohui.lightweb.ui.browser.BrowserViewModelTest" --tests "com.baicaohui.lightweb.browser.TabManagerTest"
```
预期：全部 PASS（含原有用例）。

---

## Task 5：菜单注册与接线

**Files:**
- Modify: `app/src/main/res/values/strings.xml`
- Modify: `app/src/main/java/com/baicaohui/lightweb/ui/browser/MenuItems.kt`
- Modify: `app/src/main/java/com/baicaohui/lightweb/ui/browser/MenuOrder.kt`
- Modify: `app/src/main/java/com/baicaohui/lightweb/ui/browser/BrowserViewModel.kt`（companion factory）
- Modify: `app/src/main/java/com/baicaohui/lightweb/ui/browser/BrowserScreen.kt`（复用 factory）
- Modify: `app/src/main/java/com/baicaohui/lightweb/ui/BchAppRoot.kt`
- Modify: `app/src/main/java/com/baicaohui/lightweb/ui/IncognitoAppRoot.kt`
- Modify: `app/src/test/java/com/baicaohui/lightweb/ui/browser/MenuOrderTest.kt`

### Step 1：写失败测试

`MenuOrderTest.kt` 追加：

```kotlin
@Test
fun `default order includes reader after reload`() {
    assertEquals("reader", MenuOrder.DEFAULT_ORDER[1])
}
```

### Step 2：运行确认失败

```powershell
.\gradlew.bat :app:testDebugUnitTest --tests "com.baicaohui.lightweb.ui.browser.MenuOrderTest"
```

### Step 3：实现

`strings.xml` 追加：

```xml
<string name="menu_reader">阅读模式</string>
<string name="menu_reader_exit">退出阅读模式</string>
<string name="reader_no_content">未找到适合阅读的内容</string>
<string name="reader_offline_badge">离线缓存内容</string>
<string name="reader_offline_exit_blocked">离线模式下暂不能退出阅读模式</string>
```

`MenuItems.kt`：`SPECS` 中 `"reload"` 后插入：

```kotlin
Spec("reader", R.string.menu_reader, Icons.Filled.MenuBook),
```

`MenuOrder.kt`：`DEFAULT_ORDER` 中 `"reload"` 后插入 `"reader"`。

`BrowserViewModel.kt` 增加 companion：

```kotlin
companion object {
    fun factory(tabManager: TabManager, history: HistoryRecorder, search: SearchRecorder): ViewModelProvider.Factory =
        viewModelFactory {
            initializer { BrowserViewModel(tabManager, history, search) }
        }
}
```

（import `androidx.lifecycle.ViewModelProvider`、`viewModelFactory`、`initializer`。）

`BrowserScreen.kt` 的 viewModel 创建改为：

```kotlin
val viewModel: BrowserViewModel = viewModel(
    factory = BrowserViewModel.factory(app.tabManager, app.historyRepository, app.recentSearchStore),
)
```

`BchAppRoot.kt`：顶部获取同一 ViewModel：

```kotlin
val browserViewModel: BrowserViewModel = viewModel(
    factory = BrowserViewModel.factory(app.tabManager, app.historyRepository, app.recentSearchStore),
)
```

菜单 map 增加：

```kotlin
"reader" to MoreMenuItem(
    id = "reader",
    label = stringResource(
        if (currentTab?.readerMode == true) R.string.menu_reader_exit else R.string.menu_reader,
    ),
    icon = Icons.Filled.MenuBook,
    enabled = currentTab?.url?.isNotBlank() == true,
    highlighted = currentTab?.readerMode == true,
    onClick = {
        menuOpen = false
        browserViewModel.toggleReaderMode()
    },
),
```

`IncognitoAppRoot.kt`：同样获取 `browserViewModel`，在 `sharedMenuItemsById` 增加相同 `"reader"` 项。

### Step 4：运行确认通过

```powershell
.\gradlew.bat :app:testDebugUnitTest --tests "com.baicaohui.lightweb.ui.browser.MenuOrderTest"
```

---

## Task 6：ReaderModeController + BrowserScreen 事件与离线流程

**Files:**
- Create: `app/src/main/java/com/baicaohui/lightweb/browser/ReaderModeController.kt`
- Modify: `app/src/main/java/com/baicaohui/lightweb/ui/browser/BrowserScreen.kt`

### Step 1：实现 `ReaderModeController.kt`

```kotlin
package com.baicaohui.lightweb.browser

import android.webkit.WebView
import com.baicaohui.lightweb.data.db.ReaderCacheEntity
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume

/** 阅读模式 WebView 交互薄封装：注入提取、恢复原文、加载离线缓存页。 */
class ReaderModeController(private val readabilityJs: () -> String) {

    suspend fun enter(wv: WebView, theme: String): ReaderArticle? =
        suspendCancellableCoroutine { cont ->
            wv.evaluateJavascript(ReaderPage.enterScript(readabilityJs(), theme)) { raw ->
                if (cont.isActive) cont.resume(ReaderPage.parseResult(raw))
            }
        }

    fun exit(wv: WebView, onResult: (Boolean) -> Unit) {
        wv.evaluateJavascript(ReaderPage.exitScript()) { raw ->
            onResult(ReaderPage.parseExit(raw))
        }
    }

    fun loadOffline(
        wv: WebView,
        url: String,
        article: ReaderCacheEntity,
        theme: String,
        offlineBadge: String,
    ) {
        wv.loadDataWithBaseURL(
            url,
            ReaderPage.offlineHtml(
                url = url,
                title = article.title,
                byline = article.byline,
                contentHtml = article.contentHtml,
                theme = theme,
                offlineBadge = offlineBadge,
            ),
            "text/html",
            "UTF-8",
            url,
        )
    }
}
```

### Step 2：修改 `BrowserScreen.kt`

新增 imports：

```kotlin
import androidx.compose.foundation.isSystemInDarkTheme
import com.baicaohui.lightweb.browser.ReaderModeController
import com.baicaohui.lightweb.data.db.ReaderCacheEntity
import com.baicaohui.lightweb.ui.theme.DarkMode
import com.baicaohui.lightweb.ui.theme.ThemeConfig
```

组合内新增状态与控制器：

```kotlin
val themeConfig by app.themePrefs.config.collectAsStateWithLifecycle(initialValue = ThemeConfig.DEFAULT)
val readerTheme = when (themeConfig.darkMode) {
    DarkMode.SYSTEM -> if (isSystemInDarkTheme()) "dark" else "light"
    DarkMode.LIGHT -> "light"
    DarkMode.DARK -> "dark"
}
val readerController = remember { ReaderModeController { app.readabilityJs } }
var offlineCacheLoadPending by remember { mutableStateOf(false) }
```

`tabCallbacks(tabId)` 内新增守卫函数：

```kotlin
fun isReaderInternalNavigation(url: String): Boolean {
    val tab = viewModel.tabs.value.firstOrNull { it.id == tabId } ?: return false
    return tab.readerOffline && (url == tab.url || !UrlSecurity.isSafeUrl(url))
}
```

`onPageStarted` 开头：

```kotlin
override fun onPageStarted(url: String) {
    if (isReaderInternalNavigation(url)) return
    viewModel.onPageStarted(tabId, url)
    ...
}
```

`onPageFinished` 开头：

```kotlin
override fun onPageFinished(url: String) {
    if (isReaderInternalNavigation(url)) return
    viewModel.onPageFinished(tabId, url)
    ...
}
```

`onMainFrameError` 改为：

```kotlin
override fun onMainFrameError(failingUrl: String, code: Int, description: String) {
    viewModel.onError(tabId, failingUrl)
    val id = tabId
    scope.launch {
        if (!incognito && !online) {
            val cached = app.readerCacheRepository.get(failingUrl)
            if (cached != null) {
                val wv = webViewStore.get(id) ?: return@launch
                offlineCacheLoadPending = true
                readerController.loadOffline(
                    wv = wv,
                    url = failingUrl,
                    article = cached,
                    theme = readerTheme,
                    offlineBadge = context.getString(R.string.reader_offline_badge),
                )
                viewModel.setReaderMode(id, true)
                viewModel.setReaderOffline(id, true)
                viewModel.onOfflineCacheLoaded(id)
            }
        }
    }
}
```

事件收集 `when` 中追加：

```kotlin
is BrowserEvent.EnterReader -> {
    val id = viewModel.currentId.value ?: return@collect
    val url = viewModel.tabs.value.firstOrNull { it.id == id }?.url.orEmpty()
    if (url.isBlank()) return@collect
    val wv = webViewStore.get(id) ?: return@collect
    scope.launch {
        val article = readerController.enter(wv, readerTheme)
        if (article == null) {
            Toast.makeText(context, R.string.reader_no_content, Toast.LENGTH_SHORT).show()
            return@launch
        }
        viewModel.setReaderMode(id, true)
        if (!incognito) {
            app.readerCacheRepository.put(
                ReaderCacheEntity(
                    url = url,
                    title = article.title,
                    byline = article.byline,
                    contentHtml = article.contentHtml,
                ),
            )
        }
    }
}
is BrowserEvent.ExitReader -> {
    val id = viewModel.currentId.value ?: return@collect
    val wv = webViewStore.get(id) ?: return@collect
    readerController.exit(wv) { restored ->
        if (restored) {
            viewModel.setReaderMode(id, false)
            viewModel.setReaderOffline(id, false)
        } else if (online) {
            viewModel.setReaderMode(id, false)
            viewModel.setReaderOffline(id, false)
            wv.loadUrl(viewModel.tabs.value.firstOrNull { it.id == id }?.url.orEmpty())
        } else {
            Toast.makeText(
                context,
                R.string.reader_offline_exit_blocked,
                Toast.LENGTH_SHORT,
            ).show()
        }
    }
}
```

### Step 3：编译验证

```powershell
.\gradlew.bat :app:compileDebugKotlin
```

---

## Task 7：隐私清理接入

**Files:**
- Modify: `app/src/main/java/com/baicaohui/lightweb/ui/settings/OtherSettingsScreens.kt`

### Step 1：实现

`clearCache()` 函数内追加：

```kotlin
scope.launch { app.readerCacheRepository.clear() }
```

### Step 2：编译验证

```powershell
.\gradlew.bat :app:compileDebugKotlin
```

---

## Task 8：全量验证（单元测试 + 构建）

```powershell
$env:GRADLE_USER_HOME = "D:\gradle-home"
$env:JAVA_HOME = "C:\Program Files\Android\Android Studio\jbr"
$env:Path = "$env:JAVA_HOME\bin;$env:Path"
.\gradlew.bat :app:testDebugUnitTest
.\gradlew.bat :app:assembleDebug
```

预期：全部单测 PASS（原 134 例 + 新增），`app-debug.apk` 生成。

---

## Task 9：模拟器验证（emulator-5554）

1. 安装：
   ```powershell
   adb install -r app\build\outputs\apk\debug\app-debug.apk
   ```
2. 启动本地文章站点：`python -m http.server 8000`（工作区 `docs/reader-test/`，含长文 HTML）。
3. 打开 `http://10.0.2.2:8000/article.html` → 三杠菜单 → 阅读模式 → `adb exec-out screencap -p > reader.png`。
4. 点 A+、主题按钮 → 再次截图；退出阅读模式 → 确认恢复原文与滚动位置。
5. `adb shell svc wifi disable; adb shell svc data disable` → 重开同一网址 → 自动显示离线缓存全文 → 截图。
6. 无痕模式打开同一网址（离线）→ 应显示错误页而非缓存。
7. `adb shell svc wifi enable; adb shell svc data enable` 恢复网络。
8. 设置 → 隐私 → 清除缓存 → 再次离线打开 → 应显示错误页（缓存已清）。

---

## 自审结论

- 规格覆盖：三杠开关（Task 5）、字号/主题（Task 1/6）、离线缓存（Task 2/6/7）、无痕不读写（Task 6 条件）、退出/恢复（Task 1/6）。
- 无占位符：全部步骤含真实代码与命令。
- 类型一致：`ReaderArticle`/`ReaderCacheEntity`/`readerMode`/`readerOffline` 在 Task 1/2/4/6 中命名一致。
