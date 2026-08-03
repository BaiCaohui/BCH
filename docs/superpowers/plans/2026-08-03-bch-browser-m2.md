# BCH 浏览器 M2 实施计划（数据层、书签、历史、多 WebView Tab 与会话恢复）

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 为 BCH 浏览器加入 Room 数据层（书签/文件夹/历史/快捷拨号/站点设置五张表）、书签文件夹与 HTML 导入导出、按天分组的历史页、多 WebView 标签管理与冷启动会话恢复。

**Architecture:** `data/db/` 承载 Room（Entity/Dao/Database），`data/repo/` 承载仓库（含 `HistoryRecorder` 接口），`browser/` 新增 `WebViewStore`（每 Tab 一个 WebView，LRU 销毁）与 `TabManager` 快照/恢复，`ui/tabs|bookmarks|history/` 替换占位页。会话用 SharedPreferences 同步读写（小文件，冷启动同步恢复）。

**Tech Stack:** 新增 Room 2.6.1 + KSP、kotlinx-serialization-json 1.7.3；其余沿用 M0-M1。

**前置:** `docs/superpowers/specs/2026-08-03-bch-browser-design.md`；M0-M1 已交付。

---

## 0. 环境与执行约定

- 构建命令统一带 `GRADLE_USER_HOME=D:\gradle-home` + JBR（见 AGENTS.md）
- 本里程碑新增插件：`com.google.devtools.ksp` 2.0.20-1.0.25、`org.jetbrains.kotlin.plugin.serialization` 2.0.20（根 build.gradle.kts `apply false`，app 模块启用）
- 本里程碑新增依赖：room-runtime/room-ktx 2.6.1（KSP room-compiler）、kotlinx-serialization-json 1.7.3
- 每任务提交；提交信息 ASCII

## 1. 任务清单

### Task 2.1: 插件、依赖与 Room 数据层

**Files:**
- Update: `build.gradle.kts`、`app/build.gradle.kts`
- Create: `app/src/main/java/com/baicaohui/lightweb/data/db/Entities.kt`（Folder/Bookmark/History/Shortcut/SiteSetting 五张表，见设计文档 6.3）
- Create: `app/src/main/java/com/baicaohui/lightweb/data/db/Daos.kt`（BookmarkDao/FolderDao/HistoryDao/ShortcutDao/SiteSettingDao，Flow 查询 + suspend 写操作）
- Create: `app/src/main/java/com/baicaohui/lightweb/data/db/AppDatabase.kt`（version 1，五张表，单例工厂）

验证：`.\gradlew.bat :app:assembleDebug` BUILD SUCCESSFUL。
提交：`feat: add Room database layer`

### Task 2.2: 仓库与书签 HTML 导入导出

**Files:**
- Create: `app/src/main/java/com/baicaohui/lightweb/data/repo/BookmarkRepository.kt`（根/按文件夹 Flow、增删改、清空）
- Create: `app/src/main/java/com/baicaohui/lightweb/data/repo/HistoryRepository.kt`（record 事务：存在则 visitCount+1 否则插入；最近 N 条、按时间倒序、删除、清空）
- Create: `app/src/main/java/com/baicaohui/lightweb/data/repo/ShortcutRepository.kt`、`SiteSettingsRepository.kt`
- Create: `app/src/main/java/com/baicaohui/lightweb/data/repo/HistoryRecorder.kt`（`interface HistoryRecorder { suspend fun record(url: String, title: String) }`，HistoryRepository 实现）
- Create: `app/src/main/java/com/baicaohui/lightweb/util/BookmarkHtmlIO.kt`（export(folders, bookmarks): String 生成 Netscape 格式；import(html): List<Pair<folderName?, BookmarkData>> 解析 `H3` 与 `A HREF`）
- Create: `app/src/test/java/com/baicaohui/lightweb/util/BookmarkHtmlIOTest.kt`（导出包含 H3 与 A；导入解析中文标题/URL/文件夹；空输入返回空）

TDD：先写测试（红）→ 实现（绿）→ 提交：`feat: add repositories and bookmark HTML IO`

### Task 2.3: DI 接线与会话持久化

**Files:**
- Update: `app/src/main/java/com/baicaohui/lightweb/BchApp.kt`（database/repos/sessionStore 惰性单例；onCreate 同步 `tabManager.restore(sessionStore.load())`；appScope 收集 tabs 变化写回 session）
- Create: `app/src/main/java/com/baicaohui/lightweb/browser/TabSnapshot.kt`（`@Serializable`，字段 id/url/title/createdAt/status.name）
- Update: `app/src/main/java/com/baicaohui/lightweb/browser/TabManager.kt`（`fun snapshots(): List<TabSnapshot>`；`fun restore(snapshots: List<TabSnapshot>)`：清空、重建、nextId = maxId+1、currentId = 最后一个）
- Create: `app/src/main/java/com/baicaohui/lightweb/browser/SessionStore.kt`（SharedPreferences "session"，JSON 序列化/反序列化，损坏数据返回 null）
- Update: `app/src/test/java/com/baicaohui/lightweb/browser/TabManagerTest.kt`（新增 snapshots/restore 往返用例、restore 后 currentId 正确、nextId 递增不冲突）

TDD（snapshots/restore）：先写测试 → 红 → 实现 → 绿。提交：`feat: persist tab session`

### Task 2.4: 多 WebView 与历史记录

**Files:**
- Create: `app/src/main/java/com/baicaohui/lightweb/browser/WebViewStore.kt`（`Map<Long, BrowserWebView>`；getOrCreate(tab, callbacks, adBlocker, adLevel)；destroy(id)：stopLoading/removeView/destroy；destroyRemoved(activeIds)）
- Update: `app/src/main/java/com/baicaohui/lightweb/ui/browser/BrowserViewModel.kt`（构造增加 `historyRecorder: HistoryRecorder`；onPageFinished 中调用 record(url, title)；`webViewStore` 访问入口；工厂参数带 repo）
- Update: `app/src/main/java/com/baicaohui/lightweb/ui/browser/BrowserScreen.kt`（`key(activeTab?.id)` 包裹 AndroidView，factory 从 store 取/建 WebView；tabs 变化时 destroyRemoved；初始/恢复 URL 加载逻辑保持）
- Update: `app/src/test/java/com/baicaohui/lightweb/ui/browser/BrowserViewModelTest.kt`（FakeHistoryRecorder：onPageFinished 记录 url/title；submitInput 不变）

验证：全量单测 + assembleDebug。提交：`feat: per-tab webviews and history recording`

### Task 2.5: 标签页总览界面

**Files:**
- Update: `app/src/main/java/com/baicaohui/lightweb/ui/tabs/TabSwitcherScreen.kt`（新建：LazyVerticalGrid 2 列；卡片=标题+域名，当前页主题色描边；点击 select+返回 BROWSER；右滑或长按菜单关闭；FAB 新建空标签回 BROWSER）
- Update: `app/src/main/java/com/baicaohui/lightweb/ui/BchAppRoot.kt`（TABS 目的地接入真实页面，回调含 navController）

验证：assembleDebug。提交：`feat: add tab switcher screen`

### Task 2.6: 书签页（文件夹/CRUD/多选/导入导出）

**Files:**
- Create: `app/src/main/java/com/baicaohui/lightweb/ui/bookmarks/BookmarksScreen.kt`（文件夹横向 Chips + 当前文件夹书签列表；FAB 添加；长按菜单：编辑/删除/移动到根；顶栏菜单：新建文件夹/导入/导出/清空；多选模式批量删除）
- Create: `app/src/main/java/com/baicaohui/lightweb/ui/bookmarks/BookmarkDialogs.kt`（添加/编辑对话框：标题+URL；文件夹对话框；确认删除）
- Update: `app/src/main/java/com/baicaohui/lightweb/ui/BchAppRoot.kt`（BOOKMARKS 接入）
- 导出用 `FileUtils` 风格：ACTION_CREATE_DOCUMENT（text/html）写入；导入用 ACTION_OPEN_DOCUMENT（text/html）读取后 `BookmarkHtmlIO.import` 入库（去重：已存在 URL 跳过）

验证：assembleDebug。提交：`feat: add bookmarks with folders and import/export`

### Task 2.7: 历史页

**Files:**
- Create: `app/src/main/java/com/baicaohui/lightweb/ui/history/HistoryScreen.kt`（按 今天/昨天/更早 分组 LazyColumn；顶部搜索框按 url/title 过滤；右上清空带确认；条目点击打开）
- Update: `app/src/main/java/com/baicaohui/lightweb/ui/BchAppRoot.kt`（HISTORY 接入，点击历史项 → pendingUrl + BROWSER）

验证：assembleDebug。提交：`feat: add history screen`

### Task 2.8: 集成验收

Run: `:app:testDebugUnitTest`（全量）、`:app:assembleDebug`、`:app:assembleRelease`
Expected: BUILD SUCCESSFUL；手工冒烟：书签增删改/导入导出、历史记录与搜索、多 Tab 切换与关闭、冷启动恢复标签。
提交（如有修复）：`chore: verify M2 integration`

## 2. 验收标准

- 全部单测通过（新增约 10+ 用例）
- debug/release 构建成功
- 冷启动恢复上次标签列表；多 Tab 各自独立 WebView，关闭/超限销毁
- 书签支持文件夹、增删改、多选删除、HTML 导入导出
- 历史按天分组、可搜索、可清空；每次页面加载完成记录一条
