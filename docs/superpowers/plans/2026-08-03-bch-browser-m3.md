# BCH 浏览器 M3 实施计划（组件化主页）

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 把浏览器起始页升级为可配置的组件化主页：搜索/快捷拨号/最近访问/书签/时钟五个组件可开关、排序、调参；支持纯色/渐变/本地图片背景与遮罩；快捷拨号可增删改并显示站点图标。

**Architecture:** `ui/home/` 承载 `HomeConfig`（DataStore JSON）与组件渲染；`StartPage` 按配置渲染组件并支持进入编辑页；快捷拨号数据来自 `ShortcutRepository`（Room）；背景图片经 Photo Picker 取 URI 存配置，Coil 渲染。

**Tech Stack:** 新增 `io.coil-kt:coil-compose:2.7.0`。

---

## 任务清单

### Task 3.1: HomeConfig 与 HomePrefs

**Files:**
- Update: `app/build.gradle.kts`（coil-compose 2.7.0）
- Create: `app/src/main/java/com/baicaohui/lightweb/ui/home/HomeConfig.kt`（`HomeWidgetType` 五枚举；`HomeWidgetConfig(type/enabled/columns/limit)`；`BackgroundType`；`HomeBackground(type/color/gradientStart/gradientEnd/imageUri)`；`HomeConfig(widgets/background/overlayAlpha/showSearchSuggestions)`，`@Serializable`，默认值：全部组件开启、COLOR 背景、遮罩 0.15）
- Create: `app/src/main/java/com/baicaohui/lightweb/data/prefs/HomePrefs.kt`（DataStore "home"，配置以 JSON 字符串存取；`config: Flow<HomeConfig>`、`update(transform)`，结构损坏回退默认值）
- Create: `app/src/test/java/com/baicaohui/lightweb/data/prefs/HomePrefsTest.kt`（空存储返回默认值；update 后 roundtrip；写入含图片 URI 与自定义顺序后读取一致）

TDD：先测试 → 红 → 实现 → 绿。提交：`feat: add home config persistence`

### Task 3.2: 组件化 StartPage

**Files:**
- Create: `app/src/main/java/com/baicaohui/lightweb/ui/home/HomeWidgets.kt`（各组件 Composable：`SearchWidget`、`SpeedDialWidget`（快捷拨号网格，长按编辑/删除，空态显示添加按钮）、`RecentWidget`（历史最近 N 条）、`BookmarksWidget`（根书签前 N 条）、`ClockWidget`（每分钟刷新））
- Create: `app/src/main/java/com/baicaohui/lightweb/ui/home/ShortcutDialog.kt`（添加/编辑对话框：标题/URL/颜色预设）
- Create: `app/src/main/java/com/baicaohui/lightweb/ui/home/Favicon.kt`（Coil `AsyncImage` 加载 `https://{host}/favicon.ico`，失败回落主题色首字母圆块）
- Update: `app/src/main/java/com/baicaohui/lightweb/ui/browser/StartPage.kt`（读 HomePrefs + 仓库；按配置顺序渲染组件；背景渲染 + 遮罩；右上「编辑」按钮 → HomeEditScreen）
- Update: `app/src/main/java/com/baicaohui/lightweb/ui/BchAppRoot.kt`（HOME 语义保留：底部「主页」= 新建空标签；新增 `homeEdit` 目的地）

验证：assembleDebug。提交：`feat: render configurable home widgets`

### Task 3.3: HomeEditScreen（主页编辑）

**Files:**
- Create: `app/src/main/java/com/baicaohui/lightweb/ui/home/HomeEditScreen.kt`（组件开关 + 上移/下移排序；快捷拨号列数 3/4/5；背景类型选择（纯色/渐变/图片）；纯色与渐变取色（预设色板）；图片经 `PickVisualMedia` 选择；遮罩滑杆；改动即时写入 HomePrefs）
- Update: `app/src/main/java/com/baicaohui/lightweb/ui/BchAppRoot.kt`（homeEdit 接入）

验证：assembleDebug。提交：`feat: add home edit screen`

### Task 3.4: 集成验收

Run: `:app:testDebugUnitTest`、`:app:assembleDebug`、`:app:assembleRelease`
手工冒烟：主页组件开关/排序、快捷拨号增删改与图标、背景切图与遮罩、设置持久化。
提交（如有修复）：`chore: verify M3 integration`

## 验收标准

- HomePrefs 单测通过；主页按配置渲染五个组件
- 快捷拨号增删改、图标回落正常；编辑页改动即时生效且重启保留
- debug/release 构建成功
