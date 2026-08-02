# BCH（白草灰）—— Android 浏览器设计文档

- 日期：2026-08-03
- 状态：v2 设计已确认，待用户审阅本文档后进入实施计划
- 包名：`com.baicaohui.lightweb`
- 应用名：BCH（中文名：白草灰；桌面图标显示 BCH，关于页显示「BCH 白草灰」）
- 工程位置：`E:\Opencode workspace\BaiCaohui workspace`（全新独立工程，非现有 EPUB 工程内）
- 定位：**轻量内核 + 高可定制**，Material 3 设计，内置可自定义主页

## 1. 背景与目标

从零开发一款 Android 浏览器。核心目标：

- 使用系统 WebView 内核，release APK < 12MB
- Material 3 设计，Android 12+ 支持动态取色
- 全维度自定义：主题、主页、工具栏、浏览行为、数据
- 冷启动 < 1s（中端机），内存可控（Tab 上限 + LRU 回收）

## 2. 设计原则

- **配置驱动**：功能默认精简，用户按需开启；不用就不占资源
- **内容优先**：网页内容占据最大可用面积，工具栏可自动隐藏
- **安全默认**：WebView 默认锁死文件访问、混合内容与危险 scheme
- **分层清晰**：`browser/` 内核与 `ui/` 完全隔离，可独立测试
- **即时生效**：所有个性化设置以 Flow 驱动，改动无需重启应用

## 3. 技术选型

| 决策点 | 选择 | 理由 |
|---|---|---|
| 渲染内核 | 系统 WebView | 零包体增量、系统自动更新；GeckoView/内置 Chromium 与轻量冲突 |
| UI | Jetpack Compose + Material 3 | 运行时 ColorScheme/Shape/字体替换、主页组件化布局；XML 方案无法做到同等级运行时换肤 |
| 架构 | 单 Activity + Compose Navigation + MVVM | WebView 生命周期集中管理；StateFlow 驱动 UI |
| 数据 | Room（书签/历史/站点设置）+ DataStore（主题/主页/偏好） | 定制项全部 Flow 化，改设置即时生效 |
| 图片 | Coil | 轻量 Kotlin 库；favicon 失败回落首字母圆形头像 |
| DI | 手工容器（Application 单例） | v1 模块可控，不引入注解处理器 |
| 下载 | 系统 DownloadManager | 免存储权限、系统通知、省内存 |
| 网络库 | 不引入 | WebView 自带 HTTP 栈；订阅规则更新（v2）时再评估轻量方案 |

Compose 的代价是 APK 增加约 3–4MB，换来运行时换肤与主页组件化，是本需求的核心投入点。

## 4. 功能范围

### 4.1 v1 包含

- 核心浏览：地址栏、加载进度、返回/前进/刷新、多 Tab、下载、网页权限、错误页
- 主页：组件化（搜索/快捷拨号/最近访问/书签/时钟）、背景、编辑模式
- 书签：单层文件夹、多选管理、HTML 导入/导出（Netscape 标准格式）
- 历史：按天分组、搜索、清空
- 设置：主题、主页、工具栏、搜索引擎、UA、广告拦截、隐私开关、清除数据、站点级设置
- 会话恢复：保存 URL + 标题 + 顺序，冷启动按需重新加载页面

### 4.2 v1 明确不做

真正隔离的无痕模式、阅读模式、扩展/油猴脚本、自研下载器、多进程、云同步、自定义字体文件、工具栏按钮自由排序（v1.5）、订阅式广告规则更新（v2）。

## 5. 自定义能力设计（核心）

### 5.1 主题自定义

- **取色来源三级优先**：用户自定义颜色 → Android 12+ 动态壁纸色（可开关）→ 默认青蓝 `#2B7FFF`
- **预设色板**：10 个（蓝/青/绿/紫/粉/橙/红/棕/灰/黑金）
- **自定义颜色**：HSV 色轮 + 亮度滑杆，选中后由 `TonalPaletteGenerator` 实时生成整套 MD3 tonal palette，应用内即时换色，无需重启
- **深色模式**：跟随系统 / 浅色 / 深色三态
- **字号**：小/标准/大/特大四档（映射 Compose Typography），支持跟随系统字号
- **圆角风格**：标准（M3 默认）/ 圆润（更大 corner radius），改 `Shapes` 令牌
- **密度**：标准 / 紧凑（工具栏 52/48dp、列表行距压缩）

主题项存 DataStore `ThemeConfig`，`ThemeManager` 暴露 `Flow<ThemeConfig>`，`MaterialTheme` 整体重组。

### 5.2 主页自定义

主页由组件列表驱动，用户可开关、排序、逐项配置：

| 组件 | 可配置项 |
|---|---|
| 搜索框 | 显示/隐藏、占位文字、是否显示搜索建议 |
| 快捷拨号 | 显示/隐藏、每行数量（3/4/5）、图标样式（favicon/首字母圆块/自定义色）、最多 24 个、长按拖动排序 |
| 最近访问 | 显示/隐藏、数量（4/8/12）、逐条删除 |
| 书签快捷 | 显示/隐藏、显示前 N 个书签 |
| 时钟 | 显示/隐藏、12/24 小时制 |

- **主页背景**：纯色（MD3 色板）/ 预设渐变 / 本地图片（Photo Picker，API 33+ 免权限）；可调遮罩模糊度保证前景可读
- **编辑模式**：主页右上进入编辑；组件右上角删除 X；长按拖动排序；底部「添加组件」
- 配置存 DataStore `HomeConfig`（JSON，结构可版本化迁移）

### 5.3 工具栏自定义

- **位置**：顶部（默认）/ 底部两种模式
- **按钮**：返回、前进、刷新/停止、标签页计数，逐个可显示/隐藏
- **自动隐藏**：页面滚动时隐藏工具栏（开关）
- **底栏模式**：MD3 NavigationBar（主页/标签/书签/历史/设置）**默认开启**，可关闭；地址栏可固定顶部或底部
- **地址栏行为**：点击全选（开关）、显示完整 URL 或仅域名+安全锁

### 5.4 浏览与隐私自定义

- 搜索引擎：Bing / 百度 / Google + **自定义模板**（`https://...?q=%s`）
- UA：默认 / 桌面 / **自定义字符串**
- 广告拦截：关 / 基础 / 严格（host 级黑名单分级）
- 第三方 Cookie、Safe Browsing、SSL 警告、页面缩放均独立开关
- **站点级覆盖**：任意站点单独设置 JS 开关、桌面 UA、广告级别（存 `SiteSetting`）
- 最大标签页数：6 / 12 / 24

### 5.5 数据自定义

- 书签：单层文件夹、多选批量删除、HTML 导入/导出（可迁移到 Chrome/Edge）
- 清除浏览数据：Cookie / 缓存 / 历史 / 站点设置 / 全部，一键或分项

## 6. 底层架构

### 6.1 包结构

```text
com.baicaohui.lightweb
├── BchApp.kt                   // Application：手工 DI 容器
├── MainActivity.kt             // 单 Activity + NavHost
├── browser/                    // 浏览器内核（不依赖 UI）
│   ├── BrowserWebView.kt       // WebView 封装：配置锁/生命周期/状态
│   ├── WebClientPolicy.kt      // WebViewClient + WebChromeClient 策略
│   ├── TabManager.kt           // Tab 模型、LRU、上限、会话恢复
│   ├── PermissionHandler.kt    // 网页权限 → 运行时权限
│   ├── DownloadHandler.kt      // DownloadListener → DownloadManager
│   ├── AdBlocker.kt            // 三档 host 级拦截（内置资源规则）
│   └── UrlSecurity.kt          // scheme 白名单 + URL 规范化
├── ui/
│   ├── theme/                  // Color/Type/Shape/ThemeConfig/ThemeManager/TonalPaletteGenerator
│   ├── navigation/             // Routes + NavGraph
│   ├── browser/                // BrowserScreen/地址栏/工具栏/进度条
│   ├── home/                   // HomeScreen + 组件 + HomeEditScreen
│   ├── tabs/ bookmarks/ history/ settings/
│   └── components/             // EmptyState/ErrorPage/ConfirmDialog/ColorPickerDialog…
├── model/                      // Tab/Bookmark/HistoryEntry/Shortcut/SiteSetting/SearchEngine/HomeConfig/ThemeConfig
├── data/
│   ├── db/                     // Room Entity/Dao/Database
│   ├── prefs/                  // DataStore：ThemePrefs/HomePrefs/BrowserPrefs
│   └── repo/                   // Bookmark/History/Shortcut/Settings/SiteSettings Repository
└── util/                       // UrlUtils/MimeUtils/BookmarkHtmlIO
```

### 6.2 浏览器内核

#### WebView 安全配置锁（每个 WebView 创建时强制）

- 仅放行 `http` / `https` / `about:blank`；拦截 `intent://`、`javascript:`、`file:`、`content:`
- `allowFileAccess = false`、`allowContentAccess = false`
- `allowUniversalAccessFromFileURLs = false`、`allowFileAccessFromFileURLs = false`
- `mixedContentMode = MIXED_CONTENT_NEVER_ALLOW`
- `safeBrowsingEnabled = true`
- `domStorageEnabled = true`；JS 开关按站点设置
- 缩放：`builtInZoomControls = true`、`displayZoomControls = false`

#### 生命周期纪律

- Tab 关闭顺序：`stopLoading()` → `removeView()` → `removeJavascriptInterface()` → `destroy()`
- Activity 暂停/恢复：向当前可见 WebView 转发 `onPause()/onResume()`；后台 Tab 全部 `pauseTimers()`
- `onTrimMemory`：按等级回收缓存、销毁超过上限的后台 WebView
- 配置变更（旋转）：`saveState()/restoreState()` 保留当前页

#### Tab 管理

- `TabManager` 持有轻量模型（id/url/title/status/loadProgress/createdAt），WebView 按需创建挂载
- 上限默认 12（可设 6/12/24），超限按 LRU 销毁后台 WebView，位置保留为占位，再点开时重新加载
- 会话恢复仅持久化 URL + 标题 + 顺序，冷启动按需重载；**v1 不做页面缩略图**（内存优先，标签页为文字卡片）
- 单进程模型；真正隔离的无痕/多 Profile 需多进程 + `setDataDirectorySuffix()`，留待 v2

#### 导航与 URL 安全

- `UrlSecurity.normalize()`：地址栏输入 → 合法 URL 或搜索引擎兜底
- `shouldOverrideUrlLoading`：站内链接当前页；`target=_blank`/外链按用户设置走新 Tab；外部 scheme（`mailto:`/`tel:`/第三方 app）弹 MD3 确认对话框
- `onReceivedSslError`：默认 `cancel()`；设置开启后显示警告页再放行
- `onReceivedError`：主帧错误显示内置错误页（图标 + 文案 + 重试），子资源错误忽略

#### 权限模型

| 权限 | 用途 | 时机 |
|---|---|---|
| `INTERNET` | 网络 | Manifest 常驻 |
| `ACCESS_NETWORK_STATE` | 离线提示 | Manifest 常驻 |
| 摄像头/麦克风 | 网页视频会议等 | 网页 `onPermissionRequest` 时按需申请 |
| 定位 | 网页定位 | 同上，按需申请 |
| 存储 | 不需要 | 下载走 DownloadManager，由系统写入 |
| 通知 | 不需要 | 下载通知由系统 UI 展示 |

权限记忆按站点存 `SiteSetting`（允许/拒绝/每次询问）。

#### 广告拦截

`AdBlocker` 接口 + 内置两级 host 黑名单（基础/严格），资源文件打包、纯字符串匹配请求 host，零网络请求；v2 支持订阅列表更新。

#### 下载

`DownloadListener` → `DownloadManager.Request`（解析 Content-Disposition/MIME、传 UA、文件名安全化、冲突由系统处理）。

#### 深链

`MainActivity` 声明 `ACTION_VIEW` + `BROWSABLE` 的 `http/https` intent filter；首次外部打开询问「新标签页 / 当前页」，可记住偏好；「设为默认浏览器」跳系统设置页。

### 6.3 数据模型与持久化

**Room 表：**

```text
Bookmark   (id PK, folderId?, title, url UNIQUE, iconUrl, createdAt, orderIndex)
Folder     (id PK, name, createdAt)
History    (id PK, url, title, visitTime, visitCount; 索引 (url, visitTime))
Shortcut   (id PK, title, url, color?, iconUrl, position, createdAt)   // 快捷拨号
SiteSetting(host PK, jsEnabled?, adLevel?, desktopMode?, cookiePref?, zoom)
```

**DataStore 配置（全部 Flow 化、结构可版本迁移）：**

- `ThemeConfig`：seed 色、动态取色开关、深色模式、字号、圆角、密度
- `HomeConfig`：组件开关与顺序、每行列数、背景类型与参数、遮罩
- `BrowserPrefs`：搜索引擎、主页、UA、工具栏位置与按钮、自动隐藏、底栏开关、广告级别、隐私开关、最大标签数

## 7. 界面设计（MD3）

### 7.1 主题令牌

| 令牌 | 浅色 | 深色 |
|---|---|---|
| 背景 | `#FFFFFF` | `#121316` |
| 表面（工具栏/列表） | `#F7F8FA` | `#1C1E22` |
| 主文字 | `#141414` | `#E8EAED` |
| 次要文字 | `#8A8F98` | `#9AA0A8` |
| 分割线 | `#EEF0F3` | `#2A2D33` |
| 强调色（默认 seed） | `#2B7FFF` | 同左 |

规范：地址栏 20dp 胶囊圆角；弹窗/底部弹层 16dp；列表平铺；触控目标 ≥ 48dp；字号用系统默认（地址栏 16sp、列表标题 16sp/600、正文 14sp）。深色模式不做 OLED 纯黑变体。

### 7.2 页面清单

| 页面 | 核心内容 |
|---|---|
| BrowserScreen | MD3 TopAppBar（按钮可隐藏）+ 胶囊地址栏（内嵌 2dp 进度条）+ WebView；底部默认显示 MD3 NavigationBar（可关闭）；更多菜单：新标签/刷新/书签/历史/分享/桌面版/设置 |
| HomeScreen | 组件化主页：搜索胶囊 → 快捷拨号网格 → 最近访问/书签/时钟；右上编辑按钮；背景按配置渲染 |
| HomeEditScreen | 组件管理：开关、拖动排序、列数、背景选择器（色板/渐变/相册）、遮罩滑杆 |
| TabSwitcherScreen | 2 列卡片网格（favicon+标题+域名）、当前页主题色描边、右滑关闭、长按菜单、FAB「+ 新建标签」 |
| BookmarksScreen | 文件夹入口 + 平铺列表、多选删除、添加/编辑对话框、导入/导出菜单 |
| HistoryScreen | 按今天/昨天/更早分组、搜索、清空（确认弹窗） |
| SettingsScreen | 分组列表：个性化 / 主页 / 浏览 / 隐私 / 数据 / 关于，进入各子页 |
| 子设置页 | 外观（色盘+字号+圆角+密度）、主页、工具栏、搜索引擎、站点管理、清除数据、关于（版本/WebView 版本/开源许可） |
| 通用对话框 | 外部链接确认、SSL 警告、权限请求、颜色选择、删除确认（MD3 AlertDialog/BottomSheet） |

### 7.3 关键交互

- 物理返回：`canGoBack()` → 返回上一页；已到首页时回主页，再按一次退出
- 外部链接：首次弹「新标签页/当前页」并记住，后续按偏好执行 + Toast
- 网页权限：`onPermissionRequest` → MD3 弹窗（可记忆）→ 系统运行时授权 → grant/deny
- 下载：Toast「开始下载」+ 系统通知进度
- 主题/主页/工具栏改动即时生效；UA、JS、Cookie、广告级别改动提示「重新加载生效」
- 添加快捷拨号：主页编辑模式 → 输入标题/URL/颜色，favicon 自动抓取，失败用首字母圆块
- 书签导出：标准 HTML 存 Download 目录；导入同名去重

### 7.4 空态 / 错误态 / 无障碍

- 空书签/空历史：居中图标 + 一行灰字
- 错误页：白底居中，图标 + 「无法访问此网站」+ 错误码 + 「重试」
- 离线：地址栏下方提示条「当前处于离线状态」
- 所有触控目标 ≥ 48dp；支持系统字体缩放与深色；图标按钮带 contentDescription；列表项 TalkBack 合并朗读（标题 + 域名）

## 8. 构建配置与依赖

### 8.1 版本组合

| 项 | 版本 |
|---|---|
| AGP | 8.5.2 |
| Gradle | 8.14.3（wrapper） |
| Kotlin | 2.0.20 + Compose 编译器插件 `org.jetbrains.kotlin.plugin.compose` |
| KSP | 与 Kotlin 2.0.20 匹配（2.0.20-1.0.25） |
| Java | 源码/字节码 17（本机 JDK 22 编译） |
| compileSdk / targetSdk | 35 |
| minSdk | 26（Android 8.0） |
| 构建特性 | ViewBinding 不启用（Compose）；release 开启 R8 minify + shrinkResources |

### 8.2 依赖清单

- Compose BOM、material3、material-icons（R8 裁剪）、navigation-compose、activity-compose、lifecycle-viewmodel-compose、runtime-ktx
- Room（runtime/ktx/compiler，KSP）、datastore-preferences、kotlinx-coroutines-android 1.8.1、coil-compose
- 测试：JUnit、Room 测试、Compose UI Test、Espresso（WebView 冒烟）

### 8.3 构建环境注意

- `local.properties` 机器相关（sdk.dir=D:\AndroidSDK），不入库
- 本机 Gradle wrapper 下载受 Java 22 信任库影响时，使用 Android Studio JBR 作为 `JAVA_HOME` 直接调用缓存的 Gradle 发行版

## 9. 性能目标

- release APK < 12MB
- 冷启动 < 1s（中端机）：首屏即主页 + 地址栏，书签/历史/设置懒加载
- 内存：Tab 上限 + LRU 销毁 + `onTrimMemory` 分级回收；后台 Tab `pauseTimers()`
- 列表全部 Lazy 化；Coil 磁盘缓存复用 favicon

## 10. 测试策略

- **单元测试**：UrlSecurity（scheme 拦截/恶意 URL）、AdBlocker 分级规则、TabManager（LRU/上限）、TonalPaletteGenerator、Room DAO（内存库）、BookmarkHtmlIO（导入导出往返）、Prefs 序列化与迁移
- **Compose UI 测试**：主页编辑（开关/排序）、换肤即时生效、空态
- **仪器测试**：WebView 冒烟（加载/JS 开关/下载回调）、intent:// 拦截、权限拒绝流程
- **真机兼容清单**：华为/小米/三星/原生，覆盖系统 WebView 版本差异

## 11. 版本规划

| 里程碑 | 内容 | 验收 |
|---|---|---|
| M0 | 工程骨架：Compose+MD3 主题系统（动态取色+预设色板）、导航框架 | 换肤即时生效、主题单测通过 |
| M1 | 核心浏览：WebView 安全封装、地址栏、进度、返回栈、下载、权限 | 安全用例通过、基础浏览闭环可用 |
| M2 | 数据：书签（文件夹+导入导出）、历史、Tab 管理与会话恢复 | DAO/IO 单测通过 |
| M3 | 主页系统：组件化主页、编辑模式、背景、快捷拨号 | 主页 UI 测试通过 |
| M4 | 全量自定义：工具栏双模式、站点级设置、搜索引擎、隐私与清除数据 | 设置即时生效验证 |
| M5 | 打磨发布：无障碍、空态/错误态、性能调优、图标与上架材料 | 兼容清单测试、性能达标 |

## 12. 风险与取舍

- **系统 WebView 碎片化**：关于页展示 WebView 版本；真机兼容清单覆盖主流厂商
- **Compose + WebView 互操作**：WebView 经 `AndroidView` 封装，生命周期由 BrowserWebView 严格管理，防 Activity 引用泄漏
- **动态取色仅 Android 12+**：低版本回退 seed 色生成的 tonal palette
- **无缩略图**：标签页文字卡片，省内存换速度
- **自定义项增长**：配置结构带版本号，DataStore 迁移函数逐版本升级

## 13. 待确认项

- 默认强调色 `#2B7FFF`（青蓝）是否采用（尚未收到明确确认，暂定此色）
