# BCH 浏览器 M5 实施计划（打磨与发布准备）

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 发布前打磨：离线状态提示、自适应图标（前景/背景/圆形）、内存压力处理、无障碍补全、最终全量验收与发布清单。

---

## 任务清单

### Task 5.1: 离线提示

**Files:**
- Create: `app/src/main/java/com/baicaohui/lightweb/util/NetworkMonitor.kt`（ConnectivityManager NetworkCallback → `StateFlow<Boolean> online`；start/stop）
- Update: `app/src/main/java/com/baicaohui/lightweb/BchApp.kt`（networkMonitor 惰性单例，onCreate start）
- Update: `app/src/main/java/com/baicaohui/lightweb/ui/browser/BrowserScreen.kt`（离线时地址栏下方显示提示条「当前处于离线状态」）
- Update: `app/src/main/java/com/baicaohui/lightweb/BchApp.kt`（`onTrimMemory`：TRIM_MEMORY_MODERATE 起 flush Cookie）

验证：assembleDebug。提交：`feat: add offline banner and trim memory handling`

### Task 5.2: 自适应图标

**Files:**
- Create: `app/src/main/res/mipmap-anydpi-v26/ic_launcher.xml`、`ic_launcher_round.xml`（adaptive-icon：背景色 + 前景矢量）
- Create: `app/src/main/res/drawable/ic_launcher_foreground.xml`（白色 B 字形，居中安全区）
- Create: `app/src/main/res/values/colors.xml`（`ic_launcher_background` = #2B7FFF）
- Update: `app/src/main/AndroidManifest.xml`（icon/roundIcon 指向 mipmap）

验证：assembleDebug。提交：`feat: add adaptive launcher icons`

### Task 5.3: 最终验收

Run: `:app:testDebugUnitTest`、`:app:assembleDebug`、`:app:assembleRelease`
- 统计 release APK 体积并记录
- 更新 `AGENTS.md` 发布清单（签名、版本号、真机冒烟）
- 提交：`chore: verify M5 release readiness`

## 验收标准

- 全部单测通过；debug/release 构建成功；release APK 体积达标（< 12MB）
- 离线提示、自适应图标、onTrimMemory 就位
