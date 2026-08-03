# BCH 浏览器 M4 实施计划（设置页全量自定义）

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 实现完整设置体系：主题外观（预设/自定义色/动态取色/深色/字号/圆角/密度）、工具栏（位置/按钮/自动隐藏）、搜索引擎（内置+自定义模板）、浏览（UA/广告级别/最大标签数/JS 默认）、隐私（Safe Browsing/第三方 Cookie/清除数据）、站点级设置、关于页。

**Architecture:** `data/prefs/BrowserPrefsStore.kt`（DataStore "browser"，JSON 序列化 `BrowserPrefs`）；BchApp 维护 `currentBrowserPrefs` 供 WebView/AdBlocker 即时读取；`ui/settings/` 多页面设置 UI；站点设置复用 `SiteSettingsRepository`。

---

## 任务清单

### Task 4.1: BrowserPrefs 数据层与应用

**Files:**
- Create: `app/src/main/java/com/baicaohui/lightweb/data/prefs/BrowserPrefs.kt`（`@Serializable BrowserPrefs`：searchTemplate、uaMode(DEFAULT/DESKTOP/CUSTOM)、customUa、adLevel、maxTabs=12、defaultJsEnabled、safeBrowsing、thirdPartyCookies、toolbarPosition(TOP/BOTTOM)、showBack/showForward/showReload、autoHideToolbar；DEFAULT）
- Create: `app/src/main/java/com/baicaohui/lightweb/data/prefs/BrowserPrefsStore.kt`（DataStore "browser"；`prefs: Flow<BrowserPrefs>`、`update(transform)`）
- Update: `app/src/main/java/com/baicaohui/lightweb/browser/TabManager.kt`（`setMaxTabs(n)`：改上限并立即超限驱逐）
- Update: `app/src/main/java/com/baicaohui/lightweb/browser/BrowserWebView.kt`（`applySiteSettings(url, prefs, site)`：UA（默认/桌面/自定义）、JS 开关、桌面模式；`setSafeBrowsingEnabled` 由 prefs 控制）
- Update: `app/src/main/java/com/baicaohui/lightweb/BchApp.kt`（browserPrefsStore；appScope 收集 prefs → `currentBrowserPrefs` + CookieManager 第三方 Cookie + tabManager.setMaxTabs）
- Update: `app/src/main/java/com/baicaohui/lightweb/ui/browser/BrowserScreen.kt`（onPageStarted/切换 Tab 时调用 `wv.applySiteSettings`；工具栏按 prefs 显隐按钮；adLevel 读 currentBrowserPrefs）
- Update: `app/src/test/java/.../TabManagerTest.kt`（setMaxTabs 用例）
- Create: `app/src/test/java/.../prefs/BrowserPrefsStoreTest.kt`（默认值/roundtrip）

提交：`feat: add browser prefs and apply to webview`

### Task 4.2: 设置页 UI

**Files:**
- Create: `app/src/main/java/com/baicaohui/lightweb/ui/settings/SettingsScreen.kt`（分组列表入口：个性化/主页/工具栏/搜索引擎/浏览/隐私/站点设置/关于）
- Create: `app/src/main/java/com/baicaohui/lightweb/ui/settings/AppearanceScreen.kt`（预设色板 + HSV 滑杆自定义 + 动态取色开关 + 深色模式 + 字号滑杆 + 圆角 + 密度）
- Create: `app/src/main/java/com/baicaohui/lightweb/ui/settings/OtherSettingsScreens.kt`（工具栏/搜索引擎/浏览/隐私/站点设置/关于）
- Update: `app/src/main/java/com/baicaohui/lightweb/ui/theme/TonalPaletteGenerator.kt`（公开 `fromHsvColor(h,s,v)`）
- Update: `app/src/main/java/com/baicaohui/lightweb/ui/navigation/BchRoute.kt`、`BchAppRoot.kt`（7 个子路由）
- Update: `app/src/main/res/values/strings.xml`

提交：`feat: add settings screens`

### Task 4.3: 集成验收

Run: `:app:testDebugUnitTest`、`:app:assembleDebug`、`:app:assembleRelease`
手工冒烟：换色/深色即时生效、UA 桌面模式、广告级别、最大标签数、清除数据、站点设置。
提交：`chore: verify M4 integration`

## 验收标准

- BrowserPrefs 单测通过；全部设置即时生效或提示重载生效
- 清除数据覆盖 Cookie/缓存/历史/站点设置
- debug/release 构建成功
