# BCH 无痕模式 · 宽栏菜单 · 浏览器标识快捷切换 · 内置下载器 实施计划

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 为 BCH 浏览器加入无痕模式、宽栏翻页菜单（可配置排数）、浏览器标识快捷切换与内置下载器（可切换系统下载器）。

**Architecture:** 纯逻辑（TabManager 双栈、下载核心、菜单分页）与 UI 分离；下载历史存 Room，配置存 DataStore；无痕模式不落盘会话、不记录历史、不生成缩略图。

**Tech Stack:** Kotlin + Compose（HorizontalPager/ModalBottomSheet）+ Room + DataStore + HttpURLConnection（JVM 可测）。

---

## 文件结构

### 新建
- `browser/DownloadNames.kt`：从 Content-Disposition/URL/MIME 推导文件名（纯 Kotlin）
- `browser/HttpDownloader.kt`：HttpURLConnection 下载核心（纯 Kotlin）
- `browser/DownloadStatus.kt`：QUEUED/RUNNING/COMPLETED/FAILED 枚举
- `browser/AppDownloadManager.kt`：任务编排（Repository + 协程 + 下载核心）
- `data/repo/DownloadRepository.kt`：DownloadStore 接口 + Room 实现
- `ui/browser/MoreMenuSheet.kt`：宽栏分页菜单 + 菜单项数据类
- `ui/browser/MenuLayout.kt`：菜单列数/页数纯函数
- `ui/downloads/DownloadsScreen.kt`：下载历史页
- `res/xml/file_paths.xml`：FileProvider 路径
- 测试：`browser/DownloadNamesTest.kt`、`browser/HttpDownloaderTest.kt`、`browser/AppDownloadManagerTest.kt`、`ui/browser/MenuLayoutTest.kt`

### 修改
- `data/db/Entities.kt` / `Daos.kt` / `AppDatabase.kt`：新增 downloads 表（版本 2 + Migration）
- `data/prefs/BrowserPrefs.kt` / `BrowserPrefsStore.kt`：`menuRows`、`downloadMode`，迁移 v2
- `browser/TabManager.kt`：无痕双栈（enterIncognito/exitIncognito/allTabIds）
- `BchApp.kt`：sessionStore 提升为字段；enterIncognito 前落盘普通标签；下载依赖
- `ui/BchAppRoot.kt`：三杠菜单替换为宽栏弹窗；UA 二级弹窗；无痕入口；下载入口
- `ui/browser/BrowserViewModel.kt`：无痕不记录历史
- `ui/browser/BrowserScreen.kt`：无痕下不截图、隐藏最近搜索；下载事件按配置路由
- `ui/browser/StartPage.kt`：无痕主页样式
- `ui/navigation/BchRoute.kt`：DOWNLOADS 路由
- `ui/settings/OtherSettingsScreens.kt`：菜单排数、下载方式设置
- `AndroidManifest.xml`：FileProvider
- `app/build.gradle.kts`：material-icons-extended、core-ktx
- `res/values/strings.xml`：新增文案

## 关键设计决策

1. **无痕边界**：单进程 WebView 无法真正隔离 Cookie/存储；无痕 = 独立标签栈 + 不写历史 + 不落盘会话 + 不截图 + 退出时销毁无痕 WebView。进入无痕前将普通标签快照写入 SessionStore，保证进程被杀后普通标签可恢复。
2. **宽栏菜单**：ModalBottomSheet + HorizontalPager；每页 = 排数 × 列数（列数按屏宽 72dp/项自适应 3-5 列）；默认 2 排。排数存 DataStore，设置页可改 1/2/3。
3. **UA 切换**：菜单项弹二级 AlertDialog（安卓/苹果/桌面），选择后写 BrowserPrefs.uaMode 并返回浏览器页；现有 `appliedSettingsKey` 机制会自动 reload 生效。
4. **下载器**：默认 `downloadMode=APP` 走内置 HttpURLConnection 下载到 `getExternalFilesDir(Download)`，Room 记录进度/状态；`SYSTEM` 走原 DownloadManager。内置下载支持失败重试、打开文件（FileProvider）、删除/清空记录。

## 任务清单

- [ ] Task 1：TDD 红——为 TabManager/ViewModel/Prefs/下载核心/菜单分页写失败测试
- [ ] Task 2：TabManager 无痕双栈 + 会话策略 + BrowserViewModel 历史门控
- [ ] Task 3：Prefs 扩展（menuRows/downloadMode）与迁移
- [ ] Task 4：下载核心（DownloadNames/HttpDownloader/AppDownloadManager）+ Room 表
- [ ] Task 5：宽栏菜单 + UA 二级弹窗 + 排数设置
- [ ] Task 6：无痕入口与无痕主页 + 下载页 + 设置切换
- [ ] Task 7：全量单测 + assembleDebug/Release 验证

## 验证
- `:app:testDebugUnitTest` 全部通过（新增用例须先红后绿）
- `:app:assembleDebug`、`:app:assembleRelease` 构建成功
- 不提交现有脏工作区内容；本次改动不自动 commit

---

## 修订：无痕模式真正隔离 Cookie 与网站数据（2026-08-06）

原实现（同进程双栈）只能做到“不记史/不落盘”，Cookie 与 DOM 存储在单进程 WebView 中全局共享，无法按 WebView 隔离。

### 新方案
- 新增 `IncognitoActivity`，`android:process=":incognito"` 独立进程承载全部无痕 WebView。
- 无痕进程在 `BchApp.onCreate` 中先调用 `WebView.setDataDirectorySuffix("incognito")`（API 28+），Cookie、DOM 存储、缓存全部落到独立目录 `app_webview_incognito`，与普通模式 `app_webview` 完全隔离。
- 退出无痕：销毁全部无痕 WebView → flush 无痕 Cookie → 删除 `app_webview_incognito` 目录 → `Process.killProcess` 结束无痕进程。普通模式的 Cookie/网站数据全程不被读写。
- 兜底：主进程每次启动无痕前、无痕进程每次启动时都会再次删除无痕数据目录。
- DataStore：普通 DataStore 官方不支持跨进程共用同一文件，无痕进程改用 `*_incognito.preferences_pb` 快照副本（进入时从主进程文件复制），主题/主页/浏览偏好沿用用户设置，但无痕内的修改不会回写主进程。
- API 26/27 不支持 `setDataDirectorySuffix`，菜单入口在 Android 9 以下提示不支持。

### 新增/修改文件
- 新建：`IncognitoActivity.kt`、`ui/IncognitoAppRoot.kt`、`browser/IncognitoProcess.kt`、`data/prefs/IncognitoPrefsFiles.kt`
- 修改：`AndroidManifest.xml`（incognito 进程 Activity）、`BchApp.kt`（进程分流/数据目录/DataStore 快照）、`TabManager.kt`（`initialIncognito`）、四个 prefs store（`createIncognito`）、`BchAppRoot.kt`（入口改为启动独立 Activity）、`BrowserScreen.kt`（无痕进程跳过缩略图 retain）
- 新增测试：`IncognitoProcessTest`、`IncognitoPrefsFilesTest`、TabManager 初始无痕用例

---

## 修订：首页搜索框智能联想 + 历史推荐（2026-08-06）

### 行为
- 未输入文字：聚焦搜索框时显示最近历史记录（前 8 条）。
- 输入文字后：显示当前搜索引擎的联想建议（按搜索模板主机名映射 Bing/Baidu/Google 联想接口，250ms 防抖）+ 匹配历史（标题/网址包含关键字，前 5 条）。
- 历史条目统一用历史图标标注；联想条目用搜索图标标注；点联想按搜索打开，点历史直接打开网址。
- 自定义搜索引擎无联想接口时仅显示历史匹配；无痕主页不使用该组件，避免泄露历史。

### 新增/修改文件
- 新建：`browser/SuggestionEngine.kt`（端点映射 + Bing/Baidu/Google 响应解析 + 网络获取）、`ui/home/HistorySuggestions.kt`（历史匹配规则）、`ui/home/HomeSearchBox.kt`（搜索框 + 联想/历史面板）
- 修改：`ui/components/SearchPill.kt`（暴露焦点回调）、`ui/browser/StartPage.kt`（普通模式使用 HomeSearchBox）、`strings.xml`
- 新增测试：`SuggestionEngineTest`（8 个）、`HistorySuggestionsTest`（5 个）

### 补充：输入后的匹配历史条数可配置（2026-08-06）
- 新增偏好 `historySuggestionLimit`，默认 2；DataStore 迁移至 v3。
- 输入文字后的匹配历史按该值显示（0 = 关闭，仅保留联想建议）；未输入时的最近历史展示不变。
- 设置入口：设置 → 浏览 →「历史推荐条数」（关闭 / 1~5 条）。
- 新增测试：`BrowserPrefsStoreTest` 默认值、v2→v3 迁移、持久化（2 个）。

---

## 修订：收藏夹（书签）增强 + 控制台（2026-08-06）

### 收藏夹
- 术语统一：文件夹 → 收藏夹（新建/管理/重命名/删除均按收藏夹命名）。
- 三杠菜单「添加到书签」改为完整弹窗：标题、网址、图标、收藏夹。
  - 图标默认取当前网页 favicon（WebChromeClient.onReceivedIcon → 私有目录），可手动「选择图片」替换或「清除图标」。
  - 收藏夹二级弹窗：默认收藏夹 + 用户自建收藏夹单选，或直接输入名称新建。
- 书签列表每条前置图标（Favicon：优先已存图标，回退域名 favicon.ico/字母头像）；「全部」显示所有收藏夹的书签。
- 整理功能：长按条目 → 编辑/删除/移动到收藏夹；多选模式支持批量移动到收藏夹；「管理收藏夹」支持重命名与删除（删除时连同其中书签）。
- 网页图标链路：`WebCallbacks.onIconChanged` → `BookmarkIconStore` 落盘 → `BchApp.pageIcons` → 添加书签默认图标。

### 控制台（三杠菜单入口，普通模式与无痕模式均可用）
- 源码：`evaluateJavascript(document.documentElement.outerHTML)` 展示当前页 DOM 源码，支持刷新/复制。
- 控制台：输入任意 JavaScript 命令并执行（`eval` 包裹，字符串原样返回，对象 JSON 格式化，异常显示 Error），带运行/清空/快捷命令（document.title、location.href 等）。
- 纯逻辑：`browser/ConsoleCommands.kt`（JS 字符串转义、表达式构造、结果 JSON 解码）。

### 新增/修改文件
- 新建：`ui/bookmarks/BookmarkAddDialog.kt`（添加/编辑书签、收藏夹选择、收藏夹管理）、`util/BookmarkIconStore.kt`、`browser/ConsoleCommands.kt`、`ui/console/ConsoleScreen.kt`
- 修改：`BookmarksScreen.kt`（图标/移动/管理收藏夹/全部）、`BookmarkRepository.kt`（iconUrl、renameFolder、allBookmarks）、`Daos.kt`（observeAll）、`BchAppRoot.kt`（添加书签弹窗、控制台入口）、`IncognitoAppRoot.kt`（控制台入口）、`WebClientPolicy.kt`/`BrowserScreen.kt`（favicon 捕获）、`Favicon.kt`（iconUrl）、`BchRoute.kt`（CONSOLE）、Manifest（usesCleartextTraffic）
- 新增测试：`ConsoleCommandsTest`（7 个）

### 模拟器验收（bch_api37，Android 17）
- 本地 http 站点（10.0.2.2:8000）加载正常（需 `usesCleartextTraffic`）。
- 控制台：源码显示、`1+1` → 2、`document.title` → 页面标题，均通过。
- 添加书签弹窗：标题/网址预填、网页图标默认捕获、收藏夹二级弹窗新建 myfav 成功。
- 整理：移动到默认收藏夹（DB folderId 变 null）、重命名 myfav→favorites1、删除收藏夹，全部通过；「全部」显示所有收藏夹书签。

---

## 修订：三杠菜单图标排序与显隐自定义（2026-08-06）

### 行为
- 设置 → 工具栏 →「菜单图标」：列出全部 9 个三杠菜单图标，支持上移/下移排序与开关显隐。
- 关闭的图标变灰并排到列表末尾；至少保留一个可见项（全部隐藏时回退默认）；空配置 = 默认全量顺序。
- 主进程三杠菜单按配置渲染；无痕模式菜单对共有的图标（刷新/浏览器标识/下载管理/控制台）同样生效，新建标签与退出无痕固定展示。

### 新增/修改文件
- 新建：`ui/browser/MenuOrder.kt`（顺序解析/移动纯函数）、`ui/browser/MenuItems.kt`（图标元数据注册表）
- 修改：`BrowserPrefs.kt`（`menuItemOrder`）、`ToolbarSettingsScreen`（菜单图标设置区）、`BchAppRoot.kt`/`IncognitoAppRoot.kt`（按配置构建菜单）、`strings.xml`
- 新增测试：`MenuOrderTest`（7 个）、`BrowserPrefsStoreTest` 菜单顺序持久化（2 个）

### 模拟器抽样验收
- 关闭「历史」开关 → 三杠菜单不再显示历史。
- 上移「设置」→ 菜单第二行变为 添加到书签 → 设置 → 书签。

---

## 修订：站点设置落实 + Safe Browsing 加强（2026-08-06）

### 站点设置
- `SiteSettingEntity` 新增 `safeBrowsing`、`thirdPartyCookies`（Boolean? = 跟随全局），Room 升级 v3（ALTER TABLE 迁移）。
- 站点级广告拦截正式生效：`SiteSettingsPolicy.resolve` 合并全局/站点设置（JS、广告级别、安全浏览、第三方 Cookie），`BrowserWebView` 按合并结果应用，站点广告级别不再只存不用；设置变更自动触发 reload。
- 站点设置页新增「添加站点」入口（输入域名创建默认配置）；编辑弹窗补齐：安全浏览（跟随全局/开启/关闭）、第三方 Cookie（跟随全局/开启/关闭），列表描述同步展示。

### Safe Browsing（安全浏览）
- 隐私页安全浏览开关保留并补充说明文案（拦截恶意软件/钓鱼/有害软件，需 Google Play 服务）。
- 接管 `onSafeBrowsingHit`（API 27+）：命中危险站点时弹出警告对话框，展示威胁类型（恶意软件/钓鱼/有害软件/计费欺诈/未知）与网址，提供「返回安全页面」与「仍然访问」；站点可单独覆盖安全浏览开关。
- 安全加固：`saveFormData = false`（不保存表单数据）。

### 新增/修改文件
- 新建：`browser/SiteSettingsPolicy.kt`（全局/站点合并纯逻辑）、`browser/SafeBrowsingThreats.kt`（威胁类型→文案映射）
- 修改：`Entities.kt`/`AppDatabase.kt`（v3 迁移）、`SiteSettingsRepository.kt`、`BrowserWebView.kt`、`WebClientPolicy.kt`、`BrowserViewModel.kt`/`BrowserScreen.kt`（警告对话框）、`OtherSettingsScreens.kt`（站点设置/隐私 UI）、`strings.xml`
- 新增测试：`SiteSettingsPolicyTest`（5 个）、`SafeBrowsingThreatsTest`（2 个）

### 模拟器抽样验收
- 隐私页：Safe Browsing 开关开启 + 说明文字、第三方 Cookie、清除数据列表正常。
- 站点设置：添加站点 example.com 成功；编辑弹窗含 JavaScript/桌面版/安全浏览/第三方 Cookie/广告级别，保存后列表描述更新。
- 旧数据库（v2）升级到 v3 迁移后正常读写。

---

## 修订：下载确认弹窗 + 可配置下载目录（2026-08-06）

### 行为
- 网页触发下载（DownloadListener）时先弹出确认弹窗：文件名、来源 URL，点「下载」才执行；取消则放弃。
- 默认下载位置 = Download 文件夹内的本应用目录（`Android/data/<包名>/files/Download`），真实落盘验证通过。
- 下载位置可配置：应用目录（默认）/ 系统下载目录（Android 10+ 经 MediaStore 写入公共 `Download/BCH`，免存储权限；低版本选择时提示不支持）。
- 设置入口：下载管理界面「下载设置」按钮 + 设置 → 浏览（共用 `DownloadSettingsSection`），含下载方式（内置/系统）与下载位置。
- 内置下载器支持 finalizer：下载到暂存文件后移动到目标位置（MediaStore 或直接落盘），下载记录保存最终 destination；打开文件兼容 content:// 与本地文件。

### 新增/修改文件
- 新建：`ui/downloads/DownloadSettings.kt`（下载设置弹窗/区块）
- 修改：`BrowserPrefs.kt`/`BrowserPrefsStore.kt`（`downloadLocation`，迁移 v4）、`AppDownloadManager.kt`（stagingDir + finalizer）、`BchApp.kt`（MediaStore 公共目录落盘）、`BrowserScreen.kt`（下载确认弹窗）、`DownloadsScreen.kt`（下载设置入口、content:// 打开）、`OtherSettingsScreens.kt`（浏览设置改用共享区块）、`strings.xml`
- 新增测试：`BrowserPrefsStoreTest`（默认值/迁移/持久化，3 个）、`AppDownloadManagerTest`（finalizer 移动文件，1 个）

### 模拟器实测
- 本地站点下载链接 → 弹「下载文件？」（sample.txt + 来源 URL）→ 点「下载」→ 下载管理显示 sample.txt · 已完成 → 数据库 destination =
  `/storage/emulated/0/Android/data/com.baicaohui.lightweb/files/Download/sample.txt`（66 字节）真实存在。
- 下载管理「下载设置」弹窗：下载方式（内置/系统）、下载位置（应用目录/系统下载目录）、说明文字，均正常。

### 修订：默认改为保存至公共 Download 文件夹（2026-08-06）
- `downloadLocation` 默认值从 APP 改为 PUBLIC（系统下载目录），DataStore 迁移至 v5（旧数据自动迁移）。
- 默认下载经 MediaStore 写入公共 `Download/BCH`（Android 10+，免存储权限）；Android 9 及以下回退应用目录。
- 模拟器实测：v4→v5 迁移后下载 sample2.txt，destination = `content://media/external/downloads/140`，
  实际文件 `/storage/emulated/0/Download/BCH/sample2.txt`（40 字节），确认默认公共目录生效。
