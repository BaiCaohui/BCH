# BCH 阅读模式 + 全文离线缓存 设计规格

> 日期：2026-08-06　范围：v1（M5 后功能）

## 1. 目标

1. 三杠菜单新增「阅读模式」开关，在当前标签页的「阅读视图 ↔ 原网页」之间切换。
2. 阅读视图使用本地打包的 Mozilla Readability 提取正文，提供字号（A−/A+）与主题（浅色/米黄/深色）调节。
3. 进入阅读模式时把提取的全文（标题、作者、正文 HTML）缓存到本地 Room；离线打开已缓存网址时自动展示缓存全文。
4. 隐私设置「清除缓存 / 清除全部」一并清除阅读缓存。

## 2. 非目标（v1 明确不做）

- 图片等子资源离线打包：缓存保留图片 URL，离线时图片不加载、显示占位。
- 目录（TOC）、语音朗读、阅读进度续读、按站点自动开启阅读模式。
- 无痕模式写缓存 / 读缓存（无痕保持「不落盘」语义）。
- 自定义字体文件、标注高亮、云同步。

## 3. 关键设计决策

### 3.1 原地 DOM 切换，不做页面跳转

进入阅读模式 = 向当前 WebView 注入本地 JS：克隆文档 → `Readability.parse()` → 隐藏原 body 子节点（保留引用）→ 渲染阅读视图。退出 = 恢复原 body 子节点并还原标题与滚动位置。**不产生新的历史记录，URL、前进后退、历史记录均不被破坏。**

任何真实页面导航（`onPageStarted`）都会把该标签的 `readerMode` 重置为 `false`。

### 3.2 离线缓存走 `loadDataWithBaseURL`

离线且当前网址存在缓存时，在主框架加载失败后用 `loadDataWithBaseURL(url, 离线阅读页 HTML, "text/html", "UTF-8", url)` 展示缓存全文（`url` 作为 base URL，相对链接仍可解析）。标签的 `url`/标题仍保持原网址。

离线缓存视图不可直接退出（提示「网络恢复后可退出」），避免「退出 → 重载失败 → 再次自动加载缓存」的死循环；网络恢复后可正常退出并加载原网页。

### 3.3 主题与字号

- 阅读视图初始主题由应用当前明暗模式决定（跟随系统 / 强制浅色 / 强制深色）。
- 阅读视图内 A−/A+ 与主题按钮由页面内 JS 处理，字号与主题保存到该站点 localStorage（带 try/catch 容错，离线页 origin 异常时仅会话内生效）。
- 不新增 BrowserPrefs 字段，不要求 DataStore 迁移（v6 不需要）。

## 4. 架构与组件

### 4.1 纯 Kotlin（JVM 可测）

**`browser/ReaderPage.kt`**

- `enterScript(readabilityJs: String, theme: String): String`：注入脚本，返回 `{ok, title, byline, content, reason}` JSON。
- `exitScript(): String`：恢复脚本，返回 `{ok}` JSON。
- `parseResult(raw: String?): ReaderArticle?`：解析 enter 结果。
- `parseExit(raw: String?): Boolean`：解析 exit 结果。
- `offlineHtml(url, title, byline, contentHtml, theme, offlineBadge): String`：完整离线阅读页（内联 CSS/JS）。
- `htmlEscape(...)`：标题/作者等纯文本转义，正文 HTML 原样插入。

**`browser/ReaderModeController.kt`**（薄封装，Android WebView 交互）

- `suspend enter(wv, theme): ReaderArticle?`（`suspendCancellableCoroutine` 包 `evaluateJavascript`）
- `exit(wv, onResult)` 
- `loadOffline(wv, url, article, theme, offlineBadge)`

### 4.2 数据层（Room）

`ReaderCacheEntity(url PK, title, byline, contentHtml, savedAt)` + `ReaderCacheDao`（get/upsert/delete/clear）+ `ReaderCacheRepository`（实现 `ReaderCacheStore` 接口，便于测试注入 fake）。数据库版本 3 → 4，`MIGRATION_3_4` 建表。

### 4.3 状态与 UI

- `Tab` 增加 `readerMode: Boolean = false`：会话内状态，不随 `TabSnapshot` 落盘（重启后为 false）。
- `BrowserViewModel`：`toggleReaderMode()`（发出 `EnterReader`/`ExitReader` 事件，不直接翻转）、`setReaderMode(tabId, Boolean)`（成功后翻转）、`onPageStarted` 重置 `readerMode`/`readerOffline`。主题由 BrowserScreen 依据应用明暗模式计算，无需跨层传参。
- `Tab` 增加 `readerOffline: Boolean = false`：标记当前阅读视图来自离线缓存，用于区分「DOM 阅读视图」与「缓存页」，导航守卫据此跳过缓存页自身的加载事件。
- `MenuItems.SPECS` 新增 `reader`（图标 MenuBook）；`MenuOrder.DEFAULT_ORDER` 新增 `reader`（第二位）；设置页「菜单图标」自动同步。
- `BchAppRoot` / `IncognitoAppRoot`：菜单项接线；激活时高亮、文案切换为「退出阅读模式」；无痕模式可用但不读写缓存。
- `BrowserScreen`：收集 `EnterReader/ExitReader` 事件执行 WebView 操作与缓存写入；`onMainFrameError` 时若 `!incognito && !online && 有缓存` 则自动加载缓存全文。
- `PrivacyScreen.clearCache()`：同时清空 `reader_cache` 表。

## 5. 数据流

### 在线进入阅读模式

1. 三杠菜单点击「阅读模式」→ `toggleReaderMode(theme)` → 事件 `EnterReader(theme)`。
2. BrowserScreen 调 `ReaderModeController.enter(wv, theme)`。
3. JS 克隆文档 → Readability 提取 → 渲染阅读视图 → 回调 JSON。
4. 成功：`setReaderMode(tabId, true)`；非无痕时 `ReaderCacheRepository.put(...)` 保存全文。
5. 失败（未检测到正文）：Toast「未找到适合阅读的内容」，状态保持关闭。

### 退出阅读模式

1. 菜单点击（高亮状态）→ `ExitReader` 事件。
2. 执行 `exitScript()`：恢复原 body、标题、滚动位置。
3. 恢复成功 → `setReaderMode(false)`；无状态（离线缓存视图）→ 在线则重载原网址，离线则 Toast 提示。

### 离线阅读缓存

1. 打开网址 → 网络离线 → 主框架加载失败（`onMainFrameError`）。
2. 查询 `reader_cache`：命中 → `loadDataWithBaseURL` 展示离线阅读页 + `setReaderMode(true)`；未命中 → 原错误页。

## 6. 错误处理与边界

- 页面无正文 / 提取异常：不进入阅读模式，Toast 提示。
- 重复进入（脚本状态已存在）：返回 `already-active`，忽略。
- 网页重定向：缓存键为最终 URL（`tab.url`）；离线打开原短链接可能未命中，属可接受边界。
- 阅读视图内点击链接：真实导航，`onPageStarted` 自动退出阅读模式。
- 阅读缓存页加载的 `onPageStarted/onPageFinished` 通过一次性守卫跳过，避免覆盖原 URL 与误写历史。
- 已知边界：WebView 被 LRU 回收（超过 8 个标签）后重新打开该标签会回到原文并退出阅读模式（缓存仍保留，可再次进入）。
- Readability.js 为 Apache-2.0 第三方库（Mozilla Readability 0.6.0），保留许可头，随 APK 打包于 `assets/reader/`。

## 7. 测试与验证

### 单元测试（JVM，TDD）

- `ReaderPageTest`：脚本含主题/可读性库、结果解析成功/失败/畸形、离线页转义与结构、退出解析。
- `BrowserViewModelTest` 扩展：toggle 事件、blank URL 忽略、setReaderMode、onPageStarted 重置。
- `TabManagerTest` 扩展：恢复快照后 `readerMode=false`。
- 全量单测 + `assembleDebug` 构建通过。

### 模拟器验证（emulator-5554）

1. 本地 HTTP 文章页（`10.0.2.2:8000`）→ 进入阅读模式 → 截图确认正文/字号/主题。
2. 退出恢复原页与滚动位置。
3. `svc wifi/data disable` → 重开已缓存网址 → 自动展示离线缓存全文；无痕模式不读缓存。
4. 恢复网络、清理隐私数据后缓存被清除。
