# BCH 隐私与安全加固设计（智能反追踪 / 站点权限 / HTTPS / Cookie 管理 / 风险防护 / 广告拦截增强）

> 日期：2026-08-06 · 状态：待审阅 · 范围：M5+ 隐私安全功能包

## 目标

在 BCH 浏览器中加入六类隐私/安全能力，全部可在设置中关闭、调整或删除：

1. 智能反追踪：拦截已知跨站跟踪器请求，防止用户行为被跨站分析。
2. 网站权限精细控制：位置、摄像头、麦克风、通知、弹窗、自动播放的单站点管理。
3. 安全连接强制（HTTPS）：优先/强制升级 HTTP→HTTPS，不安全页面给出明确警告。
4. Cookie 与站点数据管理：查看/删除特定网站数据；支持“关闭浏览器即清除”（全局+单站点，默认关闭）。
5. 内置风险防护：Safe Browsing（恶意网站/钓鱼）+ 危险下载警告（需用户确认）。
6. 广告拦截增强：内置 BASIC/STRICT 两级列表 + 用户自添加屏蔽规则。

## 现状（已存在，不重复建设）

- 广告拦截：`AdBlocker`（BASIC/STRICT 域名列表）+ 全局/单站点 `adLevel`。
- 风险防护：WebView Safe Browsing（含钓鱼）+ 下载确认弹窗 + SSL 错误弹窗。
- 站点设置：`SiteSettingEntity`（JS/广告/桌面/安全浏览/第三方 Cookie）+ 设置页列表。
- 隐私设置：Safe Browsing 开关、第三方 Cookie 开关、清除 Cookie/缓存/历史/站点设置。

## 架构决策

### 纯逻辑层（browser/，JVM 可测）

- `TrackerBlocker`：读取 `res/raw/trackers.txt`（内置已知跟踪器域名），`isTracker(url)` 主机/子域匹配。
- `AdBlocker` 扩展：`isBlocked(url, level, customRules)`；`CustomAdRules.matches(url, rule)` 支持
  `||host`、纯域名、含 `://` 的 URL 子串、`*` 通配符。
- `HttpsPolicy`：`HttpsMode.OFF/PREFER/STRICT`；`shouldUpgrade`、`upgrade`、`shouldFallback`、`isInsecure`。
- `SitePermissionPolicy`：`PermissionDecision.ASK/ALLOW/BLOCK`；六类权限（LOCATION/CAMERA/MICROPHONE/
  NOTIFICATIONS/POPUPS/AUTOPLAY）的“全局默认 + 站点覆盖”解析；`permissionPromptEnabled=false` 时一律 BLOCK。
- `NotificationPolicy`：通知被禁时注入覆盖 `Notification.requestPermission` 的脚本。
- `DownloadRiskPolicy`：按扩展名/MIME 判定 `DownloadRisk.HIGH/LOW`（apk/exe/bat/cmd/scr/msi/jar/vbs/ps1 等）。
- `CookieDataManager`：解析 Cookie 头为名称列表；生成“过期删除”的 `setCookie` 键值对（http/https 双写）。

### 数据层

- `BrowserPrefs` 新增（默认值见括号）：
  `antiTracking=true`、`httpsMode=PREFER`、`clearCookiesOnExit=false`、`downloadRiskWarnings=true`、
  `permissionPromptEnabled=true`、`autoplayAllowed=false`、`customAdRules=[]`、`trackedHosts=[]`、`prefsVersion=6`。
- `SiteSettingEntity` 新增可空布尔列（null=跟随全局）：`location`、`camera`、`microphone`、
  `notifications`、`popups`、`autoplay`、`httpsUpgrade`、`clearOnExit`、`antiTracking`。
- Room 升 v5，`MIGRATION_4_5` 为 site_settings 补列。
- `ReaderCacheDao` 增加按主机删除；站点数据清理由 Repository 组合（Cookie + WebStorage + 阅读缓存 + 历史 + 站点设置）。

### 运行时接线

- `BchWebViewClient.shouldInterceptRequest`：子资源按“站点广告级别 + 自定义规则 + 反追踪开关”拦截；主框架永不拦截。
- HTTPS：导航入口（地址栏提交、`ensureLoaded`）与 `onPageStarted`（表单/JS 导航兜底）升级；
  PREFER 失败回退 http，STRICT 失败弹“已阻止不安全连接”对话框；顶部横幅显示“此连接不安全”。
- 权限：`PermissionRequest`（摄像头/麦克风）与 `onGeolocationPermissionsShowPrompt`（位置）共用策略；
  弹窗走 `onCreateWindow`，允许时取 `WebViewTransport.getUrl()` 在新标签打开；自动播放按站点设置
  `mediaPlaybackRequiresUserGesture`；通知禁止时注入抑制脚本。
- Cookie 管理：`onPageStarted` 记录访问主机（`trackedHosts`，去重上限 200）；
  `BchApp.onCreate` 主进程启动时执行“关闭浏览器即清除”（全局 `removeAllCookies` + 逐站点过期删除）。
- 清单新增权限：`ACCESS_FINE_LOCATION`、`ACCESS_COARSE_LOCATION`、`CAMERA`、`RECORD_AUDIO`。

## 设置入口

- 隐私设置页：反追踪、HTTPS 模式（关闭/优先/强制）、关闭浏览器即清除、危险下载警告、权限请求主开关、自动播放。
- 站点设置编辑：新增六类权限 + HTTPS 升级 + 反追踪 + 关闭即清除（均“跟随全局/开启/关闭”）。
- 新增“广告拦截”设置页：级别 + 内置列表说明 + 自定义规则增删。
- 新增“站点数据”设置页：按主机查看 Cookie 数量/名称、删除该站 Cookie、清除该站全部数据、单站点“关闭即清除”。

## 边界与取舍

- 反追踪/广告拦截基于内置静态列表 + 用户规则，不依赖云端；列表可后续扩充。
- WebView 不支持按域名精确删 Cookie（公共 API 限制），采用“读取名称→过期 setCookie”方式覆盖主机级 Cookie；
  带 `Domain=` 属性的第三方域 Cookie 仅能全量清除（设置页提供“清除全部”）。
- 通知权限为“尽力而为”：允许时不再注入抑制脚本；WebView 是否真正展示通知取决于系统 WebView 能力。
- 弹窗“允许”等价于在新标签打开目标 URL（不创建原生子窗口）。

## 验收标准

- 新增纯逻辑单测全部先红后绿；全量单测保持全绿。
- Debug/Release 构建成功。
- 模拟器验证：反追踪拦截跟踪器请求、站点权限记忆（位置/摄像头/弹窗）、HTTPS 升级与不安全横幅、
  站点数据查看/删除/关闭即清除、危险下载警告、自定义广告规则生效、各开关可关闭。
