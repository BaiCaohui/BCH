# BCH 浏览器 M0-M1 实施计划（工程骨架 + 核心浏览）

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 在空工作区中搭建 BCH（白草灰）浏览器工程骨架（Compose + Material 3 + 可换肤主题系统 + 底部导航），并完成 M1 核心浏览：安全 WebView 封装、地址栏、加载进度、Tab 模型、下载、网页权限、外部链接与 SSL 确认、广告拦截基础实现。

**Architecture:** 单 Activity + Compose Navigation 的 MVVM 结构；`browser/` 包承载与 UI 无关的浏览器内核（WebView 封装、URL 安全、Tab 管理、广告拦截、下载、权限映射），`data/prefs/` 承载 DataStore 配置，`ui/` 承载 Compose 界面。主题配置经 DataStore 以 Flow 驱动，`MaterialTheme` 整体重组实现即时换肤。

**Tech Stack:** Kotlin 2.0.20、AGP 8.5.2、Gradle 8.14.3、Compose BOM 2024.09.03（Material 3）、Navigation Compose、DataStore Preferences、Coroutines、JUnit 4 + kotlinx-coroutines-test。

**前置规格:** `docs/superpowers/specs/2026-08-03-bch-browser-design.md`

---

## 0. 计划范围与执行约定

本计划覆盖设计文档中的 M0 与 M1，交付一个可运行、可测试的浏览器骨架。M2（书签/历史/Tab 会话恢复）、M3（主页系统）、M4（全量自定义）、M5（打磨发布）各自独立成计划，不在本文件范围内。

### 环境约定（本机已验证）

- 本机 Java 22，项目编译目标 Java 17；Gradle 使用 Android Studio JBR 避免 PKIX 证书问题
- Android SDK：`D:\AndroidSDK`（写入 `local.properties`，不入库）
- Gradle 8.14.3 发行版已缓存于 `C:\Users\杨镇豪\.gradle\wrapper\dists\gradle-8.14.3-all\10utluxaxniiv4wxiphsi49nj\gradle-8.14.3`
- 所有构建命令在本计划中统一使用以下 PowerShell 前缀（除非任务另有说明）：

```powershell
$env:JAVA_HOME = "C:\Program Files\Android\Android Studio\jbr"
$env:Path = "$env:JAVA_HOME\bin;$env:Path"
& "C:\Users\杨镇豪\.gradle\wrapper\dists\gradle-8.14.3-all\10utluxaxniiv4wxiphsi49nj\gradle-8.14.3\bin\gradle.bat" <任务>
```

生成 wrapper 后（任务 0.3），日常构建可用 `.\gradlew.bat`，若 wrapper 因证书问题下载失败，始终回退到上面的缓存 Gradle 直调方式。

### 提交约定

每个任务结束提交一次；提交信息遵循 `feat: / test: / chore:` 前缀。全部提交使用 ASCII 提交信息，避免 Windows 控制台编码问题。

### 文件结构总览（本计划创建/修改的全部文件）

```text
settings.gradle.kts                          // 工程名 BCH、:app 模块、仓库
build.gradle.kts                             // 根插件版本（apply false）
gradle.properties                            // JVM 参数、AndroidX、compileSdk 35 抑制
local.properties                             // sdk.dir（机器相关，不入库）
AGENTS.md                                    // 本工程构建说明
gradle/wrapper/* + gradlew(.bat)             // 任务 0.3 生成
app/build.gradle.kts                         // 应用模块配置与依赖
app/proguard-rules.pro                       // release R8 规则（空壳）
app/src/main/AndroidManifest.xml             // 权限、单 Activity、深链 intent-filter
app/src/main/res/values/strings.xml          // 全部 UI 字符串
app/src/main/res/values/themes.xml           // 平台启动主题
app/src/main/res/drawable/ic_launcher.xml    // 向量启动图标
app/src/main/res/raw/adblock_basic.txt       // 广告拦截基础黑名单（种子）
app/src/main/res/raw/adblock_strict.txt      // 广告拦截严格黑名单（种子）
app/src/main/java/com/baicaohui/lightweb/BchApp.kt
app/src/main/java/com/baicaohui/lightweb/MainActivity.kt
app/src/main/java/com/baicaohui/lightweb/ui/theme/ThemeConfig.kt
app/src/main/java/com/baicaohui/lightweb/ui/theme/Color.kt
app/src/main/java/com/baicaohui/lightweb/ui/theme/TonalPaletteGenerator.kt
app/src/main/java/com/baicaohui/lightweb/ui/theme/Type.kt
app/src/main/java/com/baicaohui/lightweb/ui/theme/Shape.kt
app/src/main/java/com/baicaohui/lightweb/ui/theme/Theme.kt
app/src/main/java/com/baicaohui/lightweb/data/prefs/ThemePrefs.kt
app/src/main/java/com/baicaohui/lightweb/ui/navigation/BchRoute.kt
app/src/main/java/com/baicaohui/lightweb/ui/BchAppRoot.kt
app/src/main/java/com/baicaohui/lightweb/ui/components/PlaceholderScreen.kt
app/src/main/java/com/baicaohui/lightweb/ui/components/ErrorPage.kt
app/src/main/java/com/baicaohui/lightweb/ui/home/HomeScreen.kt
app/src/main/java/com/baicaohui/lightweb/browser/UrlSecurity.kt
app/src/main/java/com/baicaohui/lightweb/browser/TabManager.kt
app/src/main/java/com/baicaohui/lightweb/browser/AdBlocker.kt
app/src/main/java/com/baicaohui/lightweb/browser/WebClientPolicy.kt
app/src/main/java/com/baicaohui/lightweb/browser/DownloadHandler.kt
app/src/main/java/com/baicaohui/lightweb/browser/PermissionMapping.kt
app/src/main/java/com/baicaohui/lightweb/browser/BrowserWebView.kt
app/src/main/java/com/baicaohui/lightweb/ui/browser/BrowserViewModel.kt
app/src/main/java/com/baicaohui/lightweb/ui/browser/AddressBar.kt
app/src/main/java/com/baicaohui/lightweb/ui/browser/BrowserToolbar.kt
app/src/main/java/com/baicaohui/lightweb/ui/browser/BrowserScreen.kt
app/src/test/java/com/baicaohui/lightweb/browser/UrlSecurityTest.kt
app/src/test/java/com/baicaohui/lightweb/browser/TabManagerTest.kt
app/src/test/java/com/baicaohui/lightweb/browser/AdBlockerTest.kt
app/src/test/java/com/baicaohui/lightweb/browser/PermissionMappingTest.kt
app/src/test/java/com/baicaohui/lightweb/ui/theme/TonalPaletteGeneratorTest.kt
app/src/test/java/com/baicaohui/lightweb/data/prefs/ThemePrefsTest.kt
app/src/test/java/com/baicaohui/lightweb/ui/browser/BrowserViewModelTest.kt
app/src/androidTest/java/com/baicaohui/lightweb/NavigationSmokeTest.kt
```

---

## Phase 0：工程脚手架

### Task 0.1: Gradle 根工程文件

**Files:**
- Create: `settings.gradle.kts`
- Create: `build.gradle.kts`
- Create: `gradle.properties`
- Create: `local.properties`

- [ ] **Step 1: 创建 `settings.gradle.kts`**

```kotlin
pluginManagement {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
    }
}

rootProject.name = "BCH"
include(":app")
```

- [ ] **Step 2: 创建根 `build.gradle.kts`**

```kotlin
plugins {
    id("com.android.application") version "8.5.2" apply false
    id("org.jetbrains.kotlin.android") version "2.0.20" apply false
    id("org.jetbrains.kotlin.plugin.compose") version "2.0.20" apply false
}
```

- [ ] **Step 3: 创建 `gradle.properties`**

```properties
org.gradle.jvmargs=-Xmx2048m -Dfile.encoding=UTF-8
android.useAndroidX=true
android.nonTransitiveRClass=true
android.suppressUnsupportedCompileSdk=35
```

- [ ] **Step 4: 创建 `local.properties`**（已 gitignore，机器相关）

```properties
sdk.dir=D:/AndroidSDK
```

- [ ] **Step 5: 验证 Gradle 可解析工程**

Run:
```powershell
$env:JAVA_HOME = "C:\Program Files\Android\Android Studio\jbr"
$env:Path = "$env:JAVA_HOME\bin;$env:Path"
& "C:\Users\杨镇豪\.gradle\wrapper\dists\gradle-8.14.3-all\10utluxaxniiv4wxiphsi49nj\gradle-8.14.3\bin\gradle.bat" projects
```
Expected: `BUILD SUCCESSFUL`，输出包含 root project `BCH`（`:app` 暂时因为目录不存在会报错——先创建 `app/` 空目录再执行本步）。

补充：`include(":app")` 需要 `app/` 目录存在，请先执行 `New-Item -ItemType Directory -Path "app\src\main\java\com\baicaohui\lightweb" -Force` 再运行验证。

- [ ] **Step 6: 提交**

```powershell
git add settings.gradle.kts build.gradle.kts gradle.properties local.properties app
git commit -m "chore: scaffold Gradle root project for BCH"
```

### Task 0.2: app 模块、清单与资源

**Files:**
- Create: `app/build.gradle.kts`
- Create: `app/proguard-rules.pro`
- Create: `app/src/main/AndroidManifest.xml`
- Create: `app/src/main/res/values/strings.xml`
- Create: `app/src/main/res/values/themes.xml`
- Create: `app/src/main/res/drawable/ic_launcher.xml`

- [ ] **Step 1: 创建 `app/build.gradle.kts`**

```kotlin
plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
}

android {
    namespace = "com.baicaohui.lightweb"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.baicaohui.lightweb"
        minSdk = 26
        targetSdk = 35
        versionCode = 1
        versionName = "0.1.0"
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro",
            )
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlinOptions {
        jvmTarget = "17"
    }

    buildFeatures {
        compose = true
    }
}

dependencies {
    val composeBom = platform("androidx.compose:compose-bom:2024.09.03")
    implementation(composeBom)
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.activity:activity-compose:1.9.2")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.8.6")
    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.8.6")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.8.6")
    implementation("androidx.navigation:navigation-compose:2.8.2")
    implementation("androidx.datastore:datastore-preferences:1.1.1")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.8.1")

    testImplementation("junit:junit:4.13.2")
    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.8.1")

    androidTestImplementation(composeBom)
    androidTestImplementation("androidx.test.ext:junit:1.2.1")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.6.1")
    androidTestImplementation("androidx.compose.ui:ui-test-junit4")

    debugImplementation("androidx.compose.ui:ui-tooling")
    debugImplementation("androidx.compose.ui:ui-test-manifest")
}
```

- [ ] **Step 2: 创建 `app/proguard-rules.pro`**

```proguard
# 本里程碑无自定义混淆规则；保留默认规则即可。
```

- [ ] **Step 3: 创建 `app/src/main/AndroidManifest.xml`**

```xml
<?xml version="1.0" encoding="utf-8"?>
<manifest xmlns:android="http://schemas.android.com/apk/res/android">

    <uses-permission android:name="android.permission.INTERNET" />
    <uses-permission android:name="android.permission.ACCESS_NETWORK_STATE" />

    <application
        android:name=".BchApp"
        android:allowBackup="true"
        android:icon="@drawable/ic_launcher"
        android:label="@string/app_name"
        android:supportsRtl="true"
        android:theme="@style/Theme.BCH">
        <activity
            android:name=".MainActivity"
            android:exported="true"
            android:launchMode="singleTask"
            android:configChanges="orientation|screenSize|keyboardHidden">
            <intent-filter>
                <action android:name="android.intent.action.MAIN" />
                <category android:name="android.intent.category.LAUNCHER" />
            </intent-filter>
            <intent-filter>
                <action android:name="android.intent.action.VIEW" />
                <category android:name="android.intent.category.DEFAULT" />
                <category android:name="android.intent.category.BROWSABLE" />
                <data android:scheme="http" />
                <data android:scheme="https" />
            </intent-filter>
        </activity>
    </application>

</manifest>
```

- [ ] **Step 4: 创建 `app/src/main/res/values/strings.xml`**

```xml
<resources>
    <string name="app_name">BCH</string>
    <string name="app_name_full">BCH 白草灰</string>
    <string name="nav_home">主页</string>
    <string name="nav_tabs">标签页</string>
    <string name="nav_bookmarks">书签</string>
    <string name="nav_history">历史</string>
    <string name="nav_settings">设置</string>
    <string name="search_hint">搜索或输入网址</string>
    <string name="action_back">后退</string>
    <string name="action_forward">前进</string>
    <string name="action_reload">刷新</string>
    <string name="action_tabs">标签页</string>
    <string name="action_open_browser">打开浏览器</string>
    <string name="download_started">开始下载</string>
    <string name="error_title">无法访问此网站</string>
    <string name="error_retry">重试</string>
    <string name="external_scheme_title">打开外部应用？</string>
    <string name="external_scheme_message">%1$s 请求打开外部应用链接。是否继续？</string>
    <string name="permission_dialog_title">网页请求权限</string>
    <string name="permission_dialog_message">%1$s 请求使用：%2$s</string>
    <string name="ssl_warning_title">连接不安全</string>
    <string name="ssl_warning_message">此网站的 SSL 证书验证失败：%1$s。仍要继续访问吗？</string>
    <string name="dialog_allow">允许</string>
    <string name="dialog_continue">继续</string>
    <string name="dialog_cancel">取消</string>
    <string name="empty_tabs">没有打开的标签页</string>
    <string name="empty_bookmarks">暂无书签</string>
    <string name="empty_history">暂无浏览历史</string>
    <string name="settings_placeholder">设置（后续里程碑实现）</string>
    <string name="home_subtitle">轻量内核 · 高可定制</string>
</resources>
```

- [ ] **Step 5: 创建 `app/src/main/res/values/themes.xml`**

```xml
<resources>
    <style name="Theme.BCH" parent="android:Theme.Material.Light.NoActionBar">
        <item name="android:windowBackground">#FFFFFF</item>
    </style>
</resources>
```

- [ ] **Step 6: 创建 `app/src/main/res/drawable/ic_launcher.xml`**（简易矢量图标：蓝底白 B）

```xml
<vector xmlns:android="http://schemas.android.com/apk/res/android"
    android:width="108dp"
    android:height="108dp"
    android:viewportWidth="108"
    android:viewportHeight="108">
    <path
        android:fillColor="#2B7FFF"
        android:pathData="M54,4 A50,50 0 1,1 53.99,4 Z" />
    <path
        android:fillColor="#FFFFFF"
        android:pathData="M34,30 L44,30 L44,82 L34,82 Z" />
    <path
        android:fillColor="#FFFFFF"
        android:pathData="M44,30 L68,30 A12,12 0 0 1 68,54 L44,54 Z" />
    <path
        android:fillColor="#FFFFFF"
        android:pathData="M44,58 L68,58 A12,12 0 0 1 68,82 L44,82 Z" />
</vector>
```

- [ ] **Step 7: 验证 debug 构建**

Run:
```powershell
$env:JAVA_HOME = "C:\Program Files\Android\Android Studio\jbr"
$env:Path = "$env:JAVA_HOME\bin;$env:Path"
& "C:\Users\杨镇豪\.gradle\wrapper\dists\gradle-8.14.3-all\10utluxaxniiv4wxiphsi49nj\gradle-8.14.3\bin\gradle.bat" :app:assembleDebug
```
Expected: `BUILD SUCCESSFUL`（首次会下载 AGP/Compose 依赖，耗时数分钟属正常）。失败时检查网络/JAVA_HOME，必要时升级执行权限重跑。

- [ ] **Step 8: 提交**

```powershell
git add app
git commit -m "chore: add app module manifest and resources"
```

### Task 0.3: 生成 Gradle Wrapper

**Files:**
- Create: `gradle/wrapper/gradle-wrapper.properties`、`gradle/wrapper/gradle-wrapper.jar`、`gradlew`、`gradlew.bat`（由 Gradle 生成）

- [ ] **Step 1: 用缓存发行版生成 wrapper**

Run:
```powershell
$env:JAVA_HOME = "C:\Program Files\Android\Android Studio\jbr"
$env:Path = "$env:JAVA_HOME\bin;$env:Path"
& "C:\Users\杨镇豪\.gradle\wrapper\dists\gradle-8.14.3-all\10utluxaxniiv4wxiphsi49nj\gradle-8.14.3\bin\gradle.bat" wrapper --gradle-version 8.14.3 --distribution-type all
```
Expected: `BUILD SUCCESSFUL`，生成 `gradlew.bat`、`gradlew`、`gradle/wrapper/*`。

- [ ] **Step 2: 验证 wrapper 可运行**

Run: `.\gradlew.bat --version`
Expected: 输出 `Gradle 8.14.3`。

注意：如果 wrapper 首次运行因 Java 22 信任库报 `PKIX path building failed`，不要修 wrapper，直接沿用任务 0.2 的缓存 Gradle 直调方式；`gradle-wrapper.properties` 保留 `-all` 发行版以命中缓存。

- [ ] **Step 3: 提交**

```powershell
git add gradle gradlew gradlew.bat
git commit -m "chore: add Gradle wrapper 8.14.3"
```

---

## Phase 1：M0 主题系统

### Task 1.1: TonalPaletteGenerator（seed 色 → MD3 ColorScheme）

**Files:**
- Create: `app/src/test/java/com/baicaohui/lightweb/ui/theme/TonalPaletteGeneratorTest.kt`
- Create: `app/src/main/java/com/baicaohui/lightweb/ui/theme/TonalPaletteGenerator.kt`

- [ ] **Step 1: 写失败测试**

```kotlin
package com.baicaohui.lightweb.ui.theme

import androidx.compose.ui.graphics.Color
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Test

class TonalPaletteGeneratorTest {

    private val seed = Color(0xFF2B7FFF)

    @Test
    fun `light scheme primary equals seed`() {
        assertEquals(seed, TonalPaletteGenerator.lightScheme(seed).primary)
    }

    @Test
    fun `light and dark background differ`() {
        assertNotEquals(
            TonalPaletteGenerator.lightScheme(seed).background,
            TonalPaletteGenerator.darkScheme(seed).background,
        )
    }

    @Test
    fun `primary and container are distinct in light scheme`() {
        assertNotEquals(
            TonalPaletteGenerator.lightScheme(seed).primary,
            TonalPaletteGenerator.lightScheme(seed).primaryContainer,
        )
    }
}
```

- [ ] **Step 2: 运行测试确认失败**

Run: `.\gradlew.bat :app:testDebugUnitTest --tests "com.baicaohui.lightweb.ui.theme.TonalPaletteGeneratorTest"`
Expected: FAIL，报 `Unresolved reference: TonalPaletteGenerator`。

- [ ] **Step 3: 实现 `TonalPaletteGenerator.kt`**

```kotlin
package com.baicaohui.lightweb.ui.theme

import android.graphics.Color as AndroidColor
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb

/**
 * 从 seed 颜色生成近似 MD3 tonal palette 的 ColorScheme。
 * 实现为 HSV 变换（饱和度缩放 + 明度阶梯），Android 12+ 动态取色生效时不会走到这里。
 */
object TonalPaletteGenerator {

    fun lightScheme(seed: Color): ColorScheme {
        val hsv = FloatArray(3)
        AndroidColor.colorToHSV(seed.toArgb(), hsv)
        fun tone(satScale: Float, value: Float): Color =
            Color(AndroidColor.HSVToColor(floatArrayOf(hsv[0], hsv[1] * satScale, value)))

        return lightColorScheme(
            primary = seed,
            onPrimary = Color.White,
            primaryContainer = tone(0.8f, 0.92f),
            onPrimaryContainer = tone(0.9f, 0.18f),
            secondary = tone(0.5f, 0.55f),
            onSecondary = Color.White,
            secondaryContainer = tone(0.4f, 0.90f),
            onSecondaryContainer = tone(0.5f, 0.20f),
            tertiary = tone(0.7f, 0.62f),
            onTertiary = Color.White,
            tertiaryContainer = tone(0.6f, 0.90f),
            onTertiaryContainer = tone(0.7f, 0.20f),
            error = Color(0xFFB3261E),
            onError = Color.White,
            errorContainer = Color(0xFFF9DEDC),
            onErrorContainer = Color(0xFF410E0B),
            background = Color(0xFFFDFBFF),
            onBackground = Color(0xFF1A1B20),
            surface = Color(0xFFFDFBFF),
            onSurface = Color(0xFF1A1B20),
            surfaceVariant = Color(0xFFE7E0EC),
            onSurfaceVariant = Color(0xFF49454F),
            outline = Color(0xFF79747E),
        )
    }

    fun darkScheme(seed: Color): ColorScheme {
        val hsv = FloatArray(3)
        AndroidColor.colorToHSV(seed.toArgb(), hsv)
        fun tone(satScale: Float, value: Float): Color =
            Color(AndroidColor.HSVToColor(floatArrayOf(hsv[0], hsv[1] * satScale, value)))

        return darkColorScheme(
            primary = tone(0.9f, 0.85f),
            onPrimary = Color(0xFF00315A),
            primaryContainer = tone(0.9f, 0.35f),
            onPrimaryContainer = tone(0.8f, 0.92f),
            secondary = tone(0.5f, 0.80f),
            onSecondary = Color(0xFF00332B),
            secondaryContainer = tone(0.5f, 0.30f),
            onSecondaryContainer = tone(0.4f, 0.90f),
            tertiary = tone(0.7f, 0.80f),
            onTertiary = Color(0xFF3F0030),
            tertiaryContainer = tone(0.7f, 0.30f),
            onTertiaryContainer = tone(0.6f, 0.90f),
            error = Color(0xFFF2B8B5),
            onError = Color(0xFF601410),
            errorContainer = Color(0xFF8C1D18),
            onErrorContainer = Color(0xFFF9DEDC),
            background = Color(0xFF121316),
            onBackground = Color(0xFFE6E1E5),
            surface = Color(0xFF121316),
            onSurface = Color(0xFFE6E1E5),
            surfaceVariant = Color(0xFF49454F),
            onSurfaceVariant = Color(0xFFCAC4D0),
            outline = Color(0xFF938F99),
        )
    }
}
```

- [ ] **Step 4: 运行测试确认通过**

Run: `.\gradlew.bat :app:testDebugUnitTest --tests "com.baicaohui.lightweb.ui.theme.TonalPaletteGeneratorTest"`
Expected: PASS（3 个用例）。

- [ ] **Step 5: 提交**

```powershell
git add app/src/main/java/com/baicaohui/lightweb/ui/theme/TonalPaletteGenerator.kt app/src/test/java/com/baicaohui/lightweb/ui/theme/TonalPaletteGeneratorTest.kt
git commit -m "feat: add MD3 tonal palette generator"
```

### Task 1.2: ThemeConfig 与 ThemePrefs（DataStore 持久化）

**Files:**
- Create: `app/src/main/java/com/baicaohui/lightweb/ui/theme/ThemeConfig.kt`
- Create: `app/src/main/java/com/baicaohui/lightweb/data/prefs/ThemePrefs.kt`
- Create: `app/src/test/java/com/baicaohui/lightweb/data/prefs/ThemePrefsTest.kt`

- [ ] **Step 1: 创建 `ThemeConfig.kt`（模型先于测试，纯数据类无行为）**

```kotlin
package com.baicaohui.lightweb.ui.theme

enum class DarkMode { SYSTEM, LIGHT, DARK }

enum class ShapeStyle { STANDARD, ROUNDED }

data class ThemeConfig(
    val seedColor: Long = 0xFF2B7FFF,
    val useDynamicColor: Boolean = true,
    val darkMode: DarkMode = DarkMode.SYSTEM,
    val fontScale: Float = 1f,
    val shapeStyle: ShapeStyle = ShapeStyle.STANDARD,
    val compact: Boolean = false,
) {
    companion object {
        val DEFAULT = ThemeConfig()
    }
}
```

- [ ] **Step 2: 写失败测试 `ThemePrefsTest.kt`**

```kotlin
package com.baicaohui.lightweb.data.prefs

import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import com.baicaohui.lightweb.ui.theme.DarkMode
import com.baicaohui.lightweb.ui.theme.ShapeStyle
import com.baicaohui.lightweb.ui.theme.ThemeConfig
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

class ThemePrefsTest {

    @get:Rule
    val tmp = TemporaryFolder()

    private fun newPrefs() = ThemePrefs(
        PreferenceDataStoreFactory.create { tmp.newFile("test-${System.nanoTime()}.preferences_pb") },
    )

    @Test
    fun `defaults when store is empty`() = runTest {
        assertEquals(ThemeConfig.DEFAULT, newPrefs().config.first())
    }

    @Test
    fun `update persists and flows new value`() = runTest {
        val prefs = newPrefs()
        prefs.update {
            it.copy(
                seedColor = 0xFFE91E63,
                useDynamicColor = false,
                darkMode = DarkMode.DARK,
                fontScale = 1.2f,
                shapeStyle = ShapeStyle.ROUNDED,
                compact = true,
            )
        }
        val config = prefs.config.first()
        assertEquals(0xFFE91E63, config.seedColor)
        assertEquals(false, config.useDynamicColor)
        assertEquals(DarkMode.DARK, config.darkMode)
        assertEquals(1.2f, config.fontScale, 0.001f)
        assertEquals(ShapeStyle.ROUNDED, config.shapeStyle)
        assertEquals(true, config.compact)
    }
}
```

- [ ] **Step 3: 运行测试确认失败**

Run: `.\gradlew.bat :app:testDebugUnitTest --tests "com.baicaohui.lightweb.data.prefs.ThemePrefsTest"`
Expected: FAIL，报 `Unresolved reference: ThemePrefs`。

- [ ] **Step 4: 实现 `ThemePrefs.kt`**

```kotlin
package com.baicaohui.lightweb.data.prefs

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.baicaohui.lightweb.ui.theme.DarkMode
import com.baicaohui.lightweb.ui.theme.ShapeStyle
import com.baicaohui.lightweb.ui.theme.ThemeConfig
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.themeDataStore by preferencesDataStore(name = "theme")

class ThemePrefs(private val dataStore: DataStore<Preferences>) {

    val config: Flow<ThemeConfig> = dataStore.data.map { it.toThemeConfig() }

    suspend fun update(transform: (ThemeConfig) -> ThemeConfig) {
        dataStore.edit { prefs ->
            val next = transform(prefs.toThemeConfig())
            prefs[Keys.SEED] = next.seedColor
            prefs[Keys.DYNAMIC] = next.useDynamicColor
            prefs[Keys.DARK_MODE] = next.darkMode.name
            prefs[Keys.FONT_SCALE] = next.fontScale
            prefs[Keys.SHAPE] = next.shapeStyle.name
            prefs[Keys.COMPACT] = next.compact
        }
    }

    private fun Preferences.toThemeConfig(): ThemeConfig = ThemeConfig(
        seedColor = this[Keys.SEED] ?: ThemeConfig.DEFAULT.seedColor,
        useDynamicColor = this[Keys.DYNAMIC] ?: ThemeConfig.DEFAULT.useDynamicColor,
        darkMode = this[Keys.DARK_MODE]
            ?.let { runCatching { DarkMode.valueOf(it) }.getOrDefault(ThemeConfig.DEFAULT.darkMode) }
            ?: ThemeConfig.DEFAULT.darkMode,
        fontScale = this[Keys.FONT_SCALE] ?: ThemeConfig.DEFAULT.fontScale,
        shapeStyle = this[Keys.SHAPE]
            ?.let { runCatching { ShapeStyle.valueOf(it) }.getOrDefault(ThemeConfig.DEFAULT.shapeStyle) }
            ?: ThemeConfig.DEFAULT.shapeStyle,
        compact = this[Keys.COMPACT] ?: ThemeConfig.DEFAULT.compact,
    )

    private object Keys {
        val SEED = longPreferencesKey("seed_color")
        val DYNAMIC = booleanPreferencesKey("dynamic_color")
        val DARK_MODE = stringPreferencesKey("dark_mode")
        val FONT_SCALE = floatPreferencesKey("font_scale")
        val SHAPE = stringPreferencesKey("shape_style")
        val COMPACT = booleanPreferencesKey("compact")
    }

    companion object {
        fun create(context: Context): ThemePrefs = ThemePrefs(context.themeDataStore)
    }
}
```

- [ ] **Step 5: 运行测试确认通过**

Run: `.\gradlew.bat :app:testDebugUnitTest --tests "com.baicaohui.lightweb.data.prefs.ThemePrefsTest"`
Expected: PASS（2 个用例）。

- [ ] **Step 6: 提交**

```powershell
git add app/src/main/java/com/baicaohui/lightweb/ui/theme/ThemeConfig.kt app/src/main/java/com/baicaohui/lightweb/data/prefs/ThemePrefs.kt app/src/test/java/com/baicaohui/lightweb/data/prefs/ThemePrefsTest.kt
git commit -m "feat: persist theme config in DataStore"
```

### Task 1.3: 主题令牌与 BchTheme

**Files:**
- Create: `app/src/main/java/com/baicaohui/lightweb/ui/theme/Color.kt`
- Create: `app/src/main/java/com/baicaohui/lightweb/ui/theme/Type.kt`
- Create: `app/src/main/java/com/baicaohui/lightweb/ui/theme/Shape.kt`
- Create: `app/src/main/java/com/baicaohui/lightweb/ui/theme/Theme.kt`

- [ ] **Step 1: 创建 `Color.kt`（预设色板，M4 设置页直接复用）**

```kotlin
package com.baicaohui.lightweb.ui.theme

val PRESET_SEEDS: List<Pair<String, Long>> = listOf(
    "青蓝" to 0xFF2B7FFF,
    "青绿" to 0xFF00A87E,
    "蓝紫" to 0xFF7C4DFF,
    "玫红" to 0xFFE91E63,
    "橙色" to 0xFFFF6D00,
    "红色" to 0xFFD32F2F,
    "棕色" to 0xFF795548,
    "灰色" to 0xFF607D8B,
    "墨黑" to 0xFF263238,
    "金色" to 0xFFB8860B,
)
```

- [ ] **Step 2: 创建 `Type.kt`（字号四档缩放）**

```kotlin
package com.baicaohui.lightweb.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.unit.sp

fun bchTypography(scale: Float): Typography {
    val base = Typography()
    fun scaled(sp: Int) = (sp * scale).sp
    return Typography(
        headlineMedium = base.headlineMedium.copy(fontSize = scaled(28)),
        titleLarge = base.titleLarge.copy(fontSize = scaled(22)),
        titleMedium = base.titleMedium.copy(fontSize = scaled(16)),
        bodyLarge = base.bodyLarge.copy(fontSize = scaled(16)),
        bodyMedium = base.bodyMedium.copy(fontSize = scaled(14)),
        labelLarge = base.labelLarge.copy(fontSize = scaled(14)),
    )
}
```

- [ ] **Step 3: 创建 `Shape.kt`（标准/圆润两档）**

```kotlin
package com.baicaohui.lightweb.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Shapes
import androidx.compose.ui.unit.dp

fun bchShapes(style: ShapeStyle): Shapes {
    val medium = if (style == ShapeStyle.ROUNDED) 20.dp else 12.dp
    val large = if (style == ShapeStyle.ROUNDED) 28.dp else 16.dp
    val extraLarge = if (style == ShapeStyle.ROUNDED) 32.dp else 24.dp
    return Shapes(
        extraSmall = RoundedCornerShape(4.dp),
        small = RoundedCornerShape(8.dp),
        medium = RoundedCornerShape(medium),
        large = RoundedCornerShape(large),
        extraLarge = RoundedCornerShape(extraLarge),
    )
}
```

- [ ] **Step 4: 创建 `Theme.kt`**

```kotlin
package com.baicaohui.lightweb.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

@Composable
fun BchTheme(
    config: ThemeConfig,
    content: @Composable () -> Unit,
) {
    val darkTheme = when (config.darkMode) {
        DarkMode.SYSTEM -> isSystemInDarkTheme()
        DarkMode.LIGHT -> false
        DarkMode.DARK -> true
    }
    val context = LocalContext.current
    val colorScheme = when {
        config.useDynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S ->
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        darkTheme -> TonalPaletteGenerator.darkScheme(Color(config.seedColor))
        else -> TonalPaletteGenerator.lightScheme(Color(config.seedColor))
    }
    MaterialTheme(
        colorScheme = colorScheme,
        typography = bchTypography(config.fontScale),
        shapes = bchShapes(config.shapeStyle),
        content = content,
    )
}
```

- [ ] **Step 5: 编译验证**

Run: `.\gradlew.bat :app:assembleDebug`
Expected: `BUILD SUCCESSFUL`。

- [ ] **Step 6: 提交**

```powershell
git add app/src/main/java/com/baicaohui/lightweb/ui/theme/Color.kt app/src/main/java/com/baicaohui/lightweb/ui/theme/Type.kt app/src/main/java/com/baicaohui/lightweb/ui/theme/Shape.kt app/src/main/java/com/baicaohui/lightweb/ui/theme/Theme.kt
git commit -m "feat: add BchTheme tokens and composable"
```

---

## Phase 2：M0 导航骨架

> **执行顺序**：本 Phase 之前须先完成 Phase 3 的 Task 3.1–3.3（`BchApp` 依赖 `TabManager` 与 `AdBlocker`）。Task 2.2 会创建 `BrowserScreen` 占位实现，Task 4.3 将其替换为完整实现。

### Task 2.1: BchApp 与 MainActivity

**Files:**
- Create: `app/src/main/java/com/baicaohui/lightweb/BchApp.kt`
- Create: `app/src/main/java/com/baicaohui/lightweb/MainActivity.kt`

- [ ] **Step 1: 创建 `BchApp.kt`（手工 DI 容器）**

```kotlin
package com.baicaohui.lightweb

import android.app.Application
import com.baicaohui.lightweb.browser.TabManager
import com.baicaohui.lightweb.data.prefs.ThemePrefs

class BchApp : Application() {

    lateinit var themePrefs: ThemePrefs
        private set

    val tabManager: TabManager = TabManager()

    val adBlocker: AdBlocker by lazy { AdBlocker.fromResources(this) }

    override fun onCreate() {
        super.onCreate()
        themePrefs = ThemePrefs.create(this)
    }
}
```

顶部 imports 需包含：

```kotlin
import com.baicaohui.lightweb.browser.AdBlocker
import com.baicaohui.lightweb.browser.TabManager
```

（`TabManager` 与 `AdBlocker` 分别在 Task 3.2、Task 3.3 创建，执行本 Phase 前须先完成这两个任务。）

- [ ] **Step 2: 创建 `MainActivity.kt`**

```kotlin
package com.baicaohui.lightweb

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.baicaohui.lightweb.ui.BchAppRoot
import com.baicaohui.lightweb.ui.theme.BchTheme
import com.baicaohui.lightweb.ui.theme.ThemeConfig

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val app = application as BchApp
            val themeConfig by app.themePrefs.config.collectAsStateWithLifecycle(
                initialValue = ThemeConfig.DEFAULT,
            )
            BchTheme(config = themeConfig) {
                BchAppRoot()
            }
        }
    }
}
```

- [ ] **Step 3: 提交（编译验证合并到 Task 2.2 之后）**

```powershell
git add app/src/main/java/com/baicaohui/lightweb/BchApp.kt app/src/main/java/com/baicaohui/lightweb/MainActivity.kt
git commit -m "feat: add Application container and MainActivity"
```

### Task 2.2: 路由、底部导航与占位页

**Files:**
- Create: `app/src/main/java/com/baicaohui/lightweb/ui/navigation/BchRoute.kt`
- Create: `app/src/main/java/com/baicaohui/lightweb/ui/components/PlaceholderScreen.kt`
- Create: `app/src/main/java/com/baicaohui/lightweb/ui/home/HomeScreen.kt`
- Create: `app/src/main/java/com/baicaohui/lightweb/ui/BchAppRoot.kt`

- [ ] **Step 1: 创建 `BchRoute.kt`**

```kotlin
package com.baicaohui.lightweb.ui.navigation

import androidx.annotation.StringRes
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Star
import androidx.compose.ui.graphics.vector.ImageVector
import com.baicaohui.lightweb.R

enum class BchRoute(
    val route: String,
    @StringRes val labelRes: Int,
    val icon: ImageVector? = null,
    val inBottomBar: Boolean = false,
) {
    HOME("home", R.string.nav_home, Icons.Filled.Home, true),
    BROWSER("browser", R.string.app_name, null, false),
    TABS("tabs", R.string.nav_tabs, Icons.Filled.List, true),
    BOOKMARKS("bookmarks", R.string.nav_bookmarks, Icons.Filled.Star, true),
    HISTORY("history", R.string.nav_history, Icons.Filled.DateRange, true),
    SETTINGS("settings", R.string.nav_settings, Icons.Filled.Settings, true),
}

/** M1 用到的工具栏图标，集中声明避免散落 import。 */
object BchIcons {
    val Back = Icons.AutoMirrored.Filled.ArrowBack
    val Forward = Icons.AutoMirrored.Filled.ArrowForward
    val Refresh = Icons.Filled.Refresh
    val Search = Icons.Filled.Search
}
```

- [ ] **Step 2: 创建 `PlaceholderScreen.kt`**

```kotlin
package com.baicaohui.lightweb.ui.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign

@Composable
fun PlaceholderScreen(text: String) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center,
    ) {
        Text(text = text, textAlign = TextAlign.Center)
    }
}
```

- [ ] **Step 3: 创建 `HomeScreen.kt`（M0 占位，M3 替换为组件化主页）**

```kotlin
package com.baicaohui.lightweb.ui.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.baicaohui.lightweb.R
import com.baicaohui.lightweb.ui.navigation.BchRoute

@Composable
fun HomeScreen(onNavigate: (String) -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize().padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = stringResource(R.string.app_name_full),
            style = MaterialTheme.typography.headlineMedium,
        )
        Spacer(Modifier.height(8.dp))
        Text(
            text = stringResource(R.string.home_subtitle),
            style = MaterialTheme.typography.bodyMedium,
        )
        Spacer(Modifier.height(32.dp))
        Button(onClick = { onNavigate(BchRoute.BROWSER.route) }) {
            Text(stringResource(R.string.action_open_browser))
        }
    }
}
```

- [ ] **Step 4: 创建 `BchAppRoot.kt`**

```kotlin
package com.baicaohui.lightweb.ui

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.baicaohui.lightweb.R
import com.baicaohui.lightweb.ui.browser.BrowserScreen
import com.baicaohui.lightweb.ui.components.PlaceholderScreen
import com.baicaohui.lightweb.ui.home.HomeScreen
import com.baicaohui.lightweb.ui.navigation.BchRoute

@Composable
fun BchAppRoot() {
    val navController = rememberNavController()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route
    val bottomRoutes = BchRoute.entries.filter { it.inBottomBar }

    Scaffold(
        bottomBar = {
            if (currentRoute in bottomRoutes.map { it.route }) {
                NavigationBar {
                    bottomRoutes.forEach { dest ->
                        NavigationBarItem(
                            selected = currentRoute == dest.route,
                            onClick = {
                                navController.navigate(dest.route) {
                                    popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            },
                            icon = {
                                Icon(
                                    imageVector = dest.icon!!,
                                    contentDescription = stringResource(dest.labelRes),
                                )
                            },
                            label = { Text(stringResource(dest.labelRes)) },
                        )
                    }
                }
            }
        },
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = BchRoute.HOME.route,
            modifier = Modifier.padding(innerPadding),
        ) {
            composable(BchRoute.HOME.route) {
                HomeScreen(onNavigate = { route -> navController.navigate(route) })
            }
            composable(BchRoute.BROWSER.route) { BrowserScreen() }
            composable(BchRoute.TABS.route) {
                PlaceholderScreen(text = stringResource(R.string.empty_tabs))
            }
            composable(BchRoute.BOOKMARKS.route) {
                PlaceholderScreen(text = stringResource(R.string.empty_bookmarks))
            }
            composable(BchRoute.HISTORY.route) {
                PlaceholderScreen(text = stringResource(R.string.empty_history))
            }
            composable(BchRoute.SETTINGS.route) {
                PlaceholderScreen(text = stringResource(R.string.settings_placeholder))
            }
        }
    }
}
```

- [ ] **Step 5: 创建 `app/src/main/java/com/baicaohui/lightweb/ui/browser/BrowserScreen.kt` 占位实现**（Task 4.3 替换为完整实现）

```kotlin
package com.baicaohui.lightweb.ui.browser

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.baicaohui.lightweb.R
import com.baicaohui.lightweb.ui.components.PlaceholderScreen

@Composable
fun BrowserScreen(initialUrl: String? = null) {
    PlaceholderScreen(text = stringResource(R.string.app_name_full))
}
```

- [ ] **Step 6: 编译验证**

Run: `.\gradlew.bat :app:assembleDebug`
Expected: `BUILD SUCCESSFUL`。

- [ ] **Step 7: 提交**

```powershell
git add app/src/main/java/com/baicaohui/lightweb/ui
git commit -m "feat: add navigation skeleton with bottom bar"
```

### Task 2.3: 导航冒烟测试（androidTest）

**Files:**
- Create: `app/src/androidTest/java/com/baicaohui/lightweb/NavigationSmokeTest.kt`

- [ ] **Step 1: 创建测试**

```kotlin
package com.baicaohui.lightweb

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import org.junit.Rule
import org.junit.Test

class NavigationSmokeTest {

    @get:Rule
    val composeRule = createAndroidComposeRule<MainActivity>()

    @Test
    fun bottomBarSwitchesDestinations() {
        composeRule.onNodeWithText("历史").performClick()
        composeRule.onNodeWithText("暂无浏览历史").assertIsDisplayed()

        composeRule.onNodeWithText("书签").performClick()
        composeRule.onNodeWithText("暂无书签").assertIsDisplayed()
    }
}
```

- [ ] **Step 2: 编译 androidTest 验证语法**

Run: `.\gradlew.bat :app:compileDebugAndroidTestKotlin`
Expected: `BUILD SUCCESSFUL`。

说明：真正运行 `connectedDebugAndroidTest` 需要连接设备/模拟器；本计划 M1 验收阶段如无设备，此用例列入手动清单。

- [ ] **Step 3: 提交**

```powershell
git add app/src/androidTest/java/com/baicaohui/lightweb/NavigationSmokeTest.kt
git commit -m "test: add navigation smoke test"
```

---

## Phase 3：M1 浏览器内核

> **执行顺序约束**：本 Phase 的 **Task 3.1–3.3 必须先于 Phase 2 执行**（`BchApp` 依赖 `TabManager` 与 `AdBlocker`，`AdBlocker` 依赖 `UrlSecurity`）。其余内核任务（3.4–3.7）在 Phase 2 之后按顺序执行；全部依赖 Phase 2 中创建的 `BrowserScreen` 占位实现，保证每个任务都能独立编译。

### Task 3.1: UrlSecurity（URL 规范化与 scheme 白名单）

**Files:**
- Create: `app/src/test/java/com/baicaohui/lightweb/browser/UrlSecurityTest.kt`
- Create: `app/src/main/java/com/baicaohui/lightweb/browser/UrlSecurity.kt`

- [ ] **Step 1: 写失败测试 `UrlSecurityTest.kt`**

```kotlin
package com.baicaohui.lightweb.browser

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class UrlSecurityTest {

    @Test
    fun `normalize adds https to bare domain`() {
        assertEquals("https://example.com", UrlSecurity.normalize("example.com"))
    }

    @Test
    fun `normalize keeps full url`() {
        assertEquals(
            "https://example.com/a?b=1",
            UrlSecurity.normalize("https://example.com/a?b=1"),
        )
    }

    @Test
    fun `normalize keeps search phrase`() {
        assertEquals("hello world", UrlSecurity.normalize("hello world"))
    }

    @Test
    fun `normalize blank returns empty`() {
        assertEquals("", UrlSecurity.normalize("  "))
    }

    @Test
    fun `safe schemes allowed`() {
        assertTrue(UrlSecurity.isSafeUrl("https://a.com"))
        assertTrue(UrlSecurity.isSafeUrl("http://a.com"))
        assertTrue(UrlSecurity.isSafeUrl("about:blank"))
    }

    @Test
    fun `dangerous schemes blocked`() {
        assertFalse(UrlSecurity.isSafeUrl("intent://scan/#Intent;scheme=zxing;end"))
        assertFalse(UrlSecurity.isSafeUrl("javascript:alert(1)"))
        assertFalse(UrlSecurity.isSafeUrl("file:///etc/passwd"))
        assertFalse(UrlSecurity.isSafeUrl("content://settings"))
    }

    @Test
    fun `search url uses template and encodes query`() {
        val result = UrlSecurity.toSearchUrl("你好 world", "https://www.bing.com/search?q=%s")
        assertTrue(result.startsWith("https://www.bing.com/search?q="))
        assertTrue(result.contains("%E4%BD%A0%E5%A5%BD"))
    }

    @Test
    fun `extractHost returns host or null`() {
        assertEquals("m.example.com", UrlSecurity.extractHost("https://m.example.com/p?q=1"))
        assertNull(UrlSecurity.extractHost("not a url"))
    }
}
```

- [ ] **Step 2: 运行测试确认失败**

Run: `.\gradlew.bat :app:testDebugUnitTest --tests "com.baicaohui.lightweb.browser.UrlSecurityTest"`
Expected: FAIL，报 `Unresolved reference: UrlSecurity`。

- [ ] **Step 3: 实现 `UrlSecurity.kt`**

```kotlin
package com.baicaohui.lightweb.browser

import android.net.Uri
import java.net.URLEncoder

object UrlSecurity {

    private val ALLOWED_SCHEMES = setOf("http", "https", "about")

    fun normalize(input: String): String {
        val trimmed = input.trim()
        if (trimmed.isEmpty()) return ""
        return if (looksLikeDomain(trimmed)) "https://$trimmed" else trimmed
    }

    fun isSafeUrl(url: String): Boolean = schemeOf(url) in ALLOWED_SCHEMES

    fun schemeOf(url: String): String? =
        url.substringBefore(":", missingDelimiterValue = "").lowercase().ifEmpty { null }

    fun isHttpUrl(url: String): Boolean = schemeOf(url) in setOf("http", "https")

    fun toSearchUrl(query: String, template: String): String =
        template.replace("%s", URLEncoder.encode(query.trim(), "UTF-8"))

    fun extractHost(url: String): String? = runCatching { Uri.parse(url).host }.getOrNull()

    private fun looksLikeDomain(input: String): Boolean =
        input.contains(".") &&
            !input.contains(" ") &&
            schemeOf(input) == null &&
            input.all { it.isLetterOrDigit() || it in ".-/" }
}
```

- [ ] **Step 4: 运行测试确认通过**

Run: `.\gradlew.bat :app:testDebugUnitTest --tests "com.baicaohui.lightweb.browser.UrlSecurityTest"`
Expected: PASS（8 个用例）。

- [ ] **Step 5: 提交**

```powershell
git add app/src/main/java/com/baicaohui/lightweb/browser/UrlSecurity.kt app/src/test/java/com/baicaohui/lightweb/browser/UrlSecurityTest.kt
git commit -m "feat: add URL normalization and scheme whitelist"
```

### Task 3.2: TabManager（LRU 上限与当前 Tab）

**Files:**
- Create: `app/src/test/java/com/baicaohui/lightweb/browser/TabManagerTest.kt`
- Create: `app/src/main/java/com/baicaohui/lightweb/browser/TabManager.kt`

- [ ] **Step 1: 写失败测试 `TabManagerTest.kt`**

```kotlin
package com.baicaohui.lightweb.browser

import kotlinx.coroutines.flow.first
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class TabManagerTest {

    @Test
    fun `new tab is added and selected`() {
        val manager = TabManager()
        val tab = manager.newTab("https://a.com")
        assertEquals(listOf(tab), manager.tabs.first())
        assertEquals(tab.id, manager.currentId.first())
        assertEquals(tab, manager.current)
    }

    @Test
    fun `close current selects most recently accessed remaining`() {
        val manager = TabManager()
        val a = manager.newTab("https://a.com")
        manager.newTab("https://b.com")
        val c = manager.newTab("https://c.com")
        manager.select(a.id)
        manager.closeTab(a.id)
        assertEquals(c.id, manager.currentId.first())
        assertEquals(2, manager.tabs.first().size)
    }

    @Test
    fun `select updates current`() {
        val manager = TabManager()
        val a = manager.newTab()
        val b = manager.newTab()
        manager.select(a.id)
        assertEquals(a.id, manager.currentId.first())
    }

    @Test
    fun `over limit evicts oldest`() {
        val manager = TabManager(maxTabs = 2)
        manager.newTab("https://a.com")
        manager.newTab("https://b.com")
        val c = manager.newTab("https://c.com")
        assertEquals(listOf("https://b.com", "https://c.com"), manager.tabs.first().map { it.url })
        assertEquals(c.id, manager.currentId.first())
    }

    @Test
    fun `update modifies tab fields`() {
        val manager = TabManager()
        val tab = manager.newTab("https://a.com")
        manager.update(tab.id) { it.copy(title = "A", status = TabStatus.READY, progress = 100) }
        val updated = manager.tabs.first().first()
        assertEquals("A", updated.title)
        assertEquals(TabStatus.READY, updated.status)
        assertEquals(100, updated.progress)
    }

    @Test
    fun `close all leaves null current`() {
        val manager = TabManager()
        val a = manager.newTab()
        manager.closeTab(a.id)
        assertNull(manager.currentId.first())
        assertNull(manager.current)
    }
}
```

- [ ] **Step 2: 运行测试确认失败**

Run: `.\gradlew.bat :app:testDebugUnitTest --tests "com.baicaohui.lightweb.browser.TabManagerTest"`
Expected: FAIL，报 `Unresolved reference: TabManager`。

- [ ] **Step 3: 实现 `TabManager.kt`**

```kotlin
package com.baicaohui.lightweb.browser

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

enum class TabStatus { EMPTY, LOADING, READY, ERROR }

data class Tab(
    val id: Long,
    val url: String = "",
    val title: String = "",
    val status: TabStatus = TabStatus.EMPTY,
    val progress: Int = 0,
    val createdAt: Long = System.currentTimeMillis(),
)

class TabManager(private val maxTabs: Int = 12) {

    private val _tabs = MutableStateFlow<List<Tab>>(emptyList())
    val tabs: StateFlow<List<Tab>> = _tabs.asStateFlow()

    private val _currentId = MutableStateFlow<Long?>(null)
    val currentId: StateFlow<Long?> = _currentId.asStateFlow()

    private val accessOrder = ArrayDeque<Long>()
    private var nextId = 1L

    val current: Tab?
        get() = _tabs.value.firstOrNull { it.id == _currentId.value }

    fun newTab(url: String = ""): Tab {
        val tab = Tab(id = nextId++)
        _tabs.value = _tabs.value + tab.copy(url = url)
        accessOrder.addLast(tab.id)
        if (_tabs.value.size > maxTabs) evictOldest()
        _currentId.value = tab.id
        return tab
    }

    fun closeTab(id: Long) {
        _tabs.value = _tabs.value.filterNot { it.id == id }
        accessOrder.remove(id)
        if (_currentId.value == id) {
            _currentId.value = accessOrder.lastOrNull() ?: _tabs.value.lastOrNull()?.id
        }
    }

    fun select(id: Long) {
        if (_tabs.value.none { it.id == id }) return
        _currentId.value = id
        accessOrder.remove(id)
        accessOrder.addLast(id)
    }

    fun update(id: Long, transform: (Tab) -> Tab) {
        _tabs.value = _tabs.value.map { if (it.id == id) transform(it) else it }
    }

    fun touch(id: Long) {
        if (_tabs.value.any { it.id == id }) {
            accessOrder.remove(id)
            accessOrder.addLast(id)
        }
    }

    private fun evictOldest() {
        val victim = accessOrder.firstOrNull() ?: return
        closeTab(victim)
    }
}
```

- [ ] **Step 4: 运行测试确认通过**

Run: `.\gradlew.bat :app:testDebugUnitTest --tests "com.baicaohui.lightweb.browser.TabManagerTest"`
Expected: PASS（6 个用例）。

- [ ] **Step 5: 提交**

```powershell
git add app/src/main/java/com/baicaohui/lightweb/browser/TabManager.kt app/src/test/java/com/baicaohui/lightweb/browser/TabManagerTest.kt
git commit -m "feat: add tab manager with LRU cap"
```

> **检查点**：Task 3.2 完成后继续完成 Task 3.3（AdBlocker），再回到 Phase 2 依次执行 Task 2.1–2.3。

### Task 3.3: AdBlocker（分级 host 黑名单）

**Files:**
- Create: `app/src/main/res/raw/adblock_basic.txt`
- Create: `app/src/main/res/raw/adblock_strict.txt`
- Create: `app/src/test/java/com/baicaohui/lightweb/browser/AdBlockerTest.kt`
- Create: `app/src/main/java/com/baicaohui/lightweb/browser/AdBlocker.kt`

- [ ] **Step 1: 创建规则资源 `adblock_basic.txt`**（种子清单，后续里程碑可扩充或改为订阅更新）

```text
# 基础规则：常用广告/统计域名
doubleclick.net
googlesyndication.com
googleadservices.com
adservice.google.com
```

- [ ] **Step 2: 创建规则资源 `adblock_strict.txt`**

```text
# 严格规则：额外拦截的广告联盟
adnxs.com
rubiconproject.com
criteo.com
taboola.com
outbrain.com
```

- [ ] **Step 3: 写失败测试 `AdBlockerTest.kt`**

```kotlin
package com.baicaohui.lightweb.browser

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AdBlockerTest {

    private val blocker = AdBlocker(
        basicHosts = setOf("doubleclick.net", "googlesyndication.com"),
        strictHosts = setOf("criteo.com", "taboola.com"),
    )

    @Test
    fun `off never blocks`() {
        assertFalse(blocker.isBlocked("https://ad.doubleclick.net/x", AdLevel.OFF))
    }

    @Test
    fun `basic blocks basic host and subdomain`() {
        assertTrue(blocker.isBlocked("https://ad.doubleclick.net/x", AdLevel.BASIC))
        assertTrue(blocker.isBlocked("https://doubleclick.net", AdLevel.BASIC))
    }

    @Test
    fun `basic does not block strict host`() {
        assertFalse(blocker.isBlocked("https://www.criteo.com", AdLevel.BASIC))
    }

    @Test
    fun `strict blocks strict host`() {
        assertTrue(blocker.isBlocked("https://www.criteo.com", AdLevel.STRICT))
    }

    @Test
    fun `unrelated host not blocked`() {
        assertFalse(blocker.isBlocked("https://example.com", AdLevel.STRICT))
    }
}
```

- [ ] **Step 4: 运行测试确认失败**

Run: `.\gradlew.bat :app:testDebugUnitTest --tests "com.baicaohui.lightweb.browser.AdBlockerTest"`
Expected: FAIL，报 `Unresolved reference: AdBlocker`。

- [ ] **Step 5: 实现 `AdBlocker.kt`**

```kotlin
package com.baicaohui.lightweb.browser

import android.content.Context
import com.baicaohui.lightweb.R

enum class AdLevel { OFF, BASIC, STRICT }

class AdBlocker(
    private val basicHosts: Set<String>,
    private val strictHosts: Set<String>,
) {

    fun isBlocked(url: String, level: AdLevel): Boolean {
        if (level == AdLevel.OFF) return false
        val host = UrlSecurity.extractHost(url) ?: return false
        val hosts = if (level == AdLevel.STRICT) basicHosts + strictHosts else basicHosts
        return hosts.any { it == host || host.endsWith(".$it") }
    }

    companion object {
        fun fromResources(context: Context): AdBlocker {
            fun read(id: Int): Set<String> =
                context.resources.openRawResource(id).bufferedReader().readLines()
                    .map { it.trim().lowercase() }
                    .filter { it.isNotEmpty() && !it.startsWith("#") }
                    .toSet()
            return AdBlocker(
                basicHosts = read(R.raw.adblock_basic),
                strictHosts = read(R.raw.adblock_strict),
            )
        }
    }
}
```

- [ ] **Step 6: 运行测试确认通过**

Run: `.\gradlew.bat :app:testDebugUnitTest --tests "com.baicaohui.lightweb.browser.AdBlockerTest"`
Expected: PASS（5 个用例）。

- [ ] **Step 7: 提交**

```powershell
git add app/src/main/java/com/baicaohui/lightweb/browser/AdBlocker.kt app/src/main/res/raw/adblock_basic.txt app/src/main/res/raw/adblock_strict.txt app/src/test/java/com/baicaohui/lightweb/browser/AdBlockerTest.kt
git commit -m "feat: add tiered ad blocker"
```

### Task 3.4: PermissionMapping（网页权限 → 运行时权限映射）

**Files:**
- Create: `app/src/test/java/com/baicaohui/lightweb/browser/PermissionMappingTest.kt`
- Create: `app/src/main/java/com/baicaohui/lightweb/browser/PermissionMapping.kt`

- [ ] **Step 1: 写失败测试 `PermissionMappingTest.kt`**

```kotlin
package com.baicaohui.lightweb.browser

import android.Manifest
import android.webkit.PermissionRequest
import org.junit.Assert.assertEquals
import org.junit.Test

class PermissionMappingTest {

    @Test
    fun `video capture maps to camera`() {
        assertEquals(
            listOf(Manifest.permission.CAMERA),
            PermissionMapping.androidPermissions(arrayOf(PermissionRequest.RESOURCE_VIDEO_CAPTURE)),
        )
    }

    @Test
    fun `audio capture maps to record audio`() {
        assertEquals(
            listOf(Manifest.permission.RECORD_AUDIO),
            PermissionMapping.androidPermissions(arrayOf(PermissionRequest.RESOURCE_AUDIO_CAPTURE)),
        )
    }

    @Test
    fun `unknown resources map to empty list`() {
        assertEquals(
            emptyList<String>(),
            PermissionMapping.androidPermissions(arrayOf("android.webkit.resource.UNKNOWN")),
        )
    }

    @Test
    fun `describe lists resources in chinese`() {
        assertEquals(
            "摄像头、麦克风",
            PermissionMapping.describe(
                arrayOf(
                    PermissionRequest.RESOURCE_VIDEO_CAPTURE,
                    PermissionRequest.RESOURCE_AUDIO_CAPTURE,
                ),
            ),
        )
    }
}
```

（`PermissionRequest` 与 `Manifest` 的字符串常量在编译期内联，JVM 单测无需 Android 运行时。）

- [ ] **Step 2: 运行测试确认失败**

Run: `.\gradlew.bat :app:testDebugUnitTest --tests "com.baicaohui.lightweb.browser.PermissionMappingTest"`
Expected: FAIL，报 `Unresolved reference: PermissionMapping`。

- [ ] **Step 3: 实现 `PermissionMapping.kt`**

```kotlin
package com.baicaohui.lightweb.browser

import android.Manifest
import android.webkit.PermissionRequest

object PermissionMapping {

    fun androidPermissions(resources: Array<String>): List<String> = buildList {
        if (PermissionRequest.RESOURCE_VIDEO_CAPTURE in resources) {
            add(Manifest.permission.CAMERA)
        }
        if (PermissionRequest.RESOURCE_AUDIO_CAPTURE in resources) {
            add(Manifest.permission.RECORD_AUDIO)
        }
    }

    fun describe(resources: Array<String>): String = resources.joinToString("、") { resource ->
        when (resource) {
            PermissionRequest.RESOURCE_VIDEO_CAPTURE -> "摄像头"
            PermissionRequest.RESOURCE_AUDIO_CAPTURE -> "麦克风"
            PermissionRequest.RESOURCE_PROTECTED_MEDIA_ID -> "受保护媒体"
            PermissionRequest.RESOURCE_MIDI_SYSEX -> "MIDI"
            else -> resource
        }
    }
}
```

- [ ] **Step 4: 运行测试确认通过**

Run: `.\gradlew.bat :app:testDebugUnitTest --tests "com.baicaohui.lightweb.browser.PermissionMappingTest"`
Expected: PASS（4 个用例）。

- [ ] **Step 5: 提交**

```powershell
git add app/src/main/java/com/baicaohui/lightweb/browser/PermissionMapping.kt app/src/test/java/com/baicaohui/lightweb/browser/PermissionMappingTest.kt
git commit -m "feat: add web permission to runtime permission mapping"
```

### Task 3.5: DownloadHandler（系统 DownloadManager）

**Files:**
- Create: `app/src/main/java/com/baicaohui/lightweb/browser/DownloadHandler.kt`

- [ ] **Step 1: 创建 `DownloadHandler.kt`**

```kotlin
package com.baicaohui.lightweb.browser

import android.app.DownloadManager
import android.content.Context
import android.net.Uri

class DownloadHandler(private val context: Context) {

    fun start(url: String, userAgent: String, mimeType: String?) {
        val title = url.substringAfterLast('/').take(60).ifBlank { "download" }
        val request = DownloadManager.Request(Uri.parse(url))
            .setMimeType(mimeType ?: "application/octet-stream")
            .addRequestHeader("User-Agent", userAgent)
            .setTitle(title)
            .setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
        context.getSystemService(DownloadManager::class.java).enqueue(request)
    }
}
```

- [ ] **Step 2: 编译验证**

Run: `.\gradlew.bat :app:assembleDebug`
Expected: `BUILD SUCCESSFUL`。

- [ ] **Step 3: 提交**

```powershell
git add app/src/main/java/com/baicaohui/lightweb/browser/DownloadHandler.kt
git commit -m "feat: route downloads to system DownloadManager"
```

### Task 3.6: WebClientPolicy（WebViewClient/WebChromeClient 策略）

**Files:**
- Create: `app/src/main/java/com/baicaohui/lightweb/browser/WebClientPolicy.kt`

- [ ] **Step 1: 创建 `WebClientPolicy.kt`**

```kotlin
package com.baicaohui.lightweb.browser

import android.graphics.Bitmap
import android.net.http.SslError
import android.webkit.PermissionRequest
import android.webkit.SslErrorHandler
import android.webkit.WebChromeClient
import android.webkit.WebResourceError
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebView
import android.webkit.WebViewClient

interface WebCallbacks {
    fun onProgress(progress: Int)
    fun onPageStarted(url: String)
    fun onPageFinished(url: String)
    fun onTitleChanged(title: String)
    fun onDownloadStart(url: String, userAgent: String, contentDisposition: String?, mimeType: String?)
    fun onPermissionRequest(request: PermissionRequest)
    fun onExternalScheme(url: String)
    fun onMainFrameError(failingUrl: String, code: Int, description: String)
    fun onSslError(url: String, handler: SslErrorHandler)
}

class BchWebViewClient(
    private val adBlocker: AdBlocker,
    private val adLevel: () -> AdLevel,
    private val callbacks: WebCallbacks,
) : WebViewClient() {

    override fun shouldOverrideUrlLoading(view: WebView, request: WebResourceRequest): Boolean {
        val url = request.url.toString()
        return if (UrlSecurity.isSafeUrl(url)) {
            false
        } else {
            callbacks.onExternalScheme(url)
            true
        }
    }

    override fun shouldInterceptRequest(
        view: WebView,
        request: WebResourceRequest,
    ): WebResourceResponse? =
        if (adBlocker.isBlocked(request.url.toString(), adLevel())) {
            WebResourceResponse("text/plain", "utf-8", null)
        } else {
            null
        }

    override fun onPageStarted(view: WebView, url: String?, favicon: Bitmap?) {
        url?.let { callbacks.onPageStarted(it) }
    }

    override fun onPageFinished(view: WebView, url: String?) {
        url?.let { callbacks.onPageFinished(it) }
    }

    override fun onReceivedError(
        view: WebView,
        request: WebResourceRequest,
        error: WebResourceError,
    ) {
        if (request.isForMainFrame) {
            callbacks.onMainFrameError(request.url.toString(), error.errorCode, error.description.toString())
        }
    }

    override fun onReceivedSslError(view: WebView, handler: SslErrorHandler, error: SslError) {
        callbacks.onSslError(error.url, handler)
    }
}

class BchWebChromeClient(private val callbacks: WebCallbacks) : WebChromeClient() {

    override fun onProgressChanged(view: WebView, newProgress: Int) {
        callbacks.onProgress(newProgress)
    }

    override fun onReceivedTitle(view: WebView, title: String?) {
        title?.let { callbacks.onTitleChanged(it) }
    }

    override fun onDownloadStart(
        url: String?,
        userAgent: String?,
        contentDisposition: String?,
        mimetype: String?,
        contentLength: Long,
    ) {
        if (url != null) {
            callbacks.onDownloadStart(url, userAgent ?: "", contentDisposition, mimetype)
        }
    }

    override fun onPermissionRequest(request: PermissionRequest) {
        callbacks.onPermissionRequest(request)
    }
}
```

- [ ] **Step 2: 编译验证**

Run: `.\gradlew.bat :app:assembleDebug`
Expected: `BUILD SUCCESSFUL`。

- [ ] **Step 3: 提交**

```powershell
git add app/src/main/java/com/baicaohui/lightweb/browser/WebClientPolicy.kt
git commit -m "feat: add WebView client and chrome policy"
```

### Task 3.7: BrowserWebView（安全配置锁）

**Files:**
- Create: `app/src/main/java/com/baicaohui/lightweb/browser/BrowserWebView.kt`

- [ ] **Step 1: 创建 `BrowserWebView.kt`**

```kotlin
package com.baicaohui.lightweb.browser

import android.content.Context
import android.webkit.WebSettings
import android.webkit.WebView

class BrowserWebView(
    context: Context,
    private val callbacks: WebCallbacks,
    private val adBlocker: AdBlocker,
    private val adLevel: () -> AdLevel = { AdLevel.BASIC },
) : WebView(context) {

    init {
        settings.apply {
            javaScriptEnabled = true
            domStorageEnabled = true
            allowFileAccess = false
            allowContentAccess = false
            allowUniversalAccessFromFileURLs = false
            allowFileAccessFromFileURLs = false
            mixedContentMode = WebSettings.MIXED_CONTENT_NEVER_ALLOW
            builtInZoomControls = true
            displayZoomControls = false
            setSupportZoom(true)
        }
        setSafeBrowsingEnabled(true)
        webViewClient = BchWebViewClient(adBlocker, adLevel, callbacks)
        webChromeClient = BchWebChromeClient(callbacks)
    }
}
```

- [ ] **Step 2: 编译验证**

Run: `.\gradlew.bat :app:assembleDebug`
Expected: `BUILD SUCCESSFUL`。

- [ ] **Step 3: 提交**

```powershell
git add app/src/main/java/com/baicaohui/lightweb/browser/BrowserWebView.kt
git commit -m "feat: add secure WebView wrapper"
```

---

## Phase 4：M1 UI 集成

### Task 4.1: BrowserViewModel（输入路由与浏览器事件）

**Files:**
- Create: `app/src/test/java/com/baicaohui/lightweb/ui/browser/BrowserViewModelTest.kt`
- Create: `app/src/main/java/com/baicaohui/lightweb/ui/browser/BrowserViewModel.kt`

- [ ] **Step 1: 写失败测试 `BrowserViewModelTest.kt`**

```kotlin
package com.baicaohui.lightweb.ui.browser

import com.baicaohui.lightweb.browser.TabManager
import com.baicaohui.lightweb.browser.TabStatus
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class BrowserViewModelTest {

    private val dispatcher = StandardTestDispatcher()
    private val template = "https://www.bing.com/search?q=%s"

    @Before
    fun setUp() {
        Dispatchers.setMain(dispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `submitInput with domain loads https url`() = runTest {
        val vm = BrowserViewModel(TabManager())
        vm.submitInput("example.com", template)
        dispatcher.scheduler.advanceUntilIdle()
        val tab = vm.tabs.first().first()
        assertEquals("https://example.com", tab.url)
        assertEquals(TabStatus.LOADING, tab.status)
    }

    @Test
    fun `submitInput with phrase routes to search template`() = runTest {
        val vm = BrowserViewModel(TabManager())
        vm.submitInput("hello world", template)
        dispatcher.scheduler.advanceUntilIdle()
        val tab = vm.tabs.first().first()
        assertEquals("https://www.bing.com/search?q=hello+world", tab.url)
    }

    @Test
    fun `submitInput blank does nothing`() = runTest {
        val vm = BrowserViewModel(TabManager())
        vm.submitInput("   ", template)
        dispatcher.scheduler.advanceUntilIdle()
        assertEquals(0, vm.tabs.first().size)
    }

    @Test
    fun `new tab and close tab delegate to manager`() = runTest {
        val vm = BrowserViewModel(TabManager())
        val a = vm.newTab("https://a.com")
        vm.newTab("https://b.com")
        vm.closeTab(a.id)
        assertEquals(listOf("https://b.com"), vm.tabs.first().map { it.url })
    }
}
```

- [ ] **Step 2: 运行测试确认失败**

Run: `.\gradlew.bat :app:testDebugUnitTest --tests "com.baicaohui.lightweb.ui.browser.BrowserViewModelTest"`
Expected: FAIL，报 `Unresolved reference: BrowserViewModel`。

- [ ] **Step 3: 实现 `BrowserViewModel.kt`**

```kotlin
package com.baicaohui.lightweb.ui.browser

import android.webkit.PermissionRequest
import android.webkit.SslErrorHandler
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.baicaohui.lightweb.browser.Tab
import com.baicaohui.lightweb.browser.TabManager
import com.baicaohui.lightweb.browser.TabStatus
import com.baicaohui.lightweb.browser.UrlSecurity
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch

class BrowserViewModel(private val tabManager: TabManager) : ViewModel() {

    val tabs: StateFlow<List<Tab>> = tabManager.tabs
    val currentId: StateFlow<Long?> = tabManager.currentId

    private val _events = MutableSharedFlow<BrowserEvent>(extraBufferCapacity = 8)
    val events: SharedFlow<BrowserEvent> = _events.asSharedFlow()

    fun newTab(url: String = ""): Tab = tabManager.newTab(url)

    fun closeTab(id: Long) = tabManager.closeTab(id)

    fun selectTab(id: Long) = tabManager.select(id)

    fun submitInput(raw: String, searchTemplate: String) {
        val input = raw.trim()
        if (input.isEmpty()) return
        val normalized = UrlSecurity.normalize(input)
        val url = if (UrlSecurity.isSafeUrl(normalized)) {
            normalized
        } else {
            UrlSecurity.toSearchUrl(input, searchTemplate)
        }
        val existing = tabManager.currentId.value?.takeIf { id ->
            tabManager.tabs.value.any { it.id == id }
        }
        val id = existing ?: tabManager.newTab().id
        tabManager.update(id) { it.copy(url = url, status = TabStatus.LOADING, progress = 5) }
        emit(BrowserEvent.Navigate(url))
    }

    fun retry() {
        updateCurrent { it.copy(status = TabStatus.LOADING, progress = 10) }
        emit(BrowserEvent.Reload)
    }

    fun onProgress(progress: Int) = updateCurrent { it.copy(progress = progress) }

    fun onPageStarted(url: String) = updateCurrent {
        it.copy(url = url, status = TabStatus.LOADING, progress = 10)
    }

    fun onPageFinished(url: String) = updateCurrent {
        it.copy(url = url, status = TabStatus.READY, progress = 100)
    }

    fun onTitle(title: String) = updateCurrent { it.copy(title = title) }

    fun onError(failingUrl: String) = updateCurrent {
        it.copy(status = TabStatus.ERROR, progress = 100)
    }

    fun onExternalScheme(url: String) = emit(BrowserEvent.ExternalScheme(url))

    fun onPermissionRequest(request: PermissionRequest) =
        emit(BrowserEvent.PermissionRequest(request))

    fun onSslError(url: String, handler: SslErrorHandler) =
        emit(BrowserEvent.SslError(url, handler))

    fun onDownload(url: String, userAgent: String, mimeType: String?) =
        emit(BrowserEvent.Download(url, userAgent, mimeType))

    private fun updateCurrent(transform: (Tab) -> Tab) {
        tabManager.currentId.value?.let { tabManager.update(it, transform) }
    }

    private fun emit(event: BrowserEvent) {
        viewModelScope.launch { _events.emit(event) }
    }
}

sealed interface BrowserEvent {
    data object Reload : BrowserEvent
    data class Navigate(val url: String) : BrowserEvent
    data class ExternalScheme(val url: String) : BrowserEvent
    data class PermissionRequest(val request: PermissionRequest) : BrowserEvent
    data class SslError(val url: String, val handler: SslErrorHandler) : BrowserEvent
    data class Download(val url: String, val userAgent: String, val mimeType: String?) : BrowserEvent
}
```

- [ ] **Step 4: 运行测试确认通过**

Run: `.\gradlew.bat :app:testDebugUnitTest --tests "com.baicaohui.lightweb.ui.browser.BrowserViewModelTest"`
Expected: PASS（4 个用例）。

- [ ] **Step 5: 提交**

```powershell
git add app/src/main/java/com/baicaohui/lightweb/ui/browser/BrowserViewModel.kt app/src/test/java/com/baicaohui/lightweb/ui/browser/BrowserViewModelTest.kt
git commit -m "feat: add browser view model with input routing"
```

### Task 4.2: AddressBar 与 BrowserToolbar 组件

**Files:**
- Create: `app/src/main/java/com/baicaohui/lightweb/ui/browser/AddressBar.kt`
- Create: `app/src/main/java/com/baicaohui/lightweb/ui/browser/BrowserToolbar.kt`

- [ ] **Step 1: 创建 `AddressBar.kt`**

```kotlin
package com.baicaohui.lightweb.ui.browser

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import com.baicaohui.lightweb.R

@Composable
fun AddressBar(
    value: String,
    onValueChange: (String) -> Unit,
    onSubmit: () -> Unit,
    progress: Int,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier.fillMaxWidth()) {
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            modifier = Modifier.fillMaxWidth().height(52.dp),
            shape = RoundedCornerShape(26.dp),
            singleLine = true,
            placeholder = { Text(stringResource(R.string.search_hint)) },
            leadingIcon = { Icon(BchIcons.Search, contentDescription = null) },
            trailingIcon = {
                if (value.isNotEmpty()) {
                    IconButton(onClick = { onValueChange("") }) {
                        Icon(Icons.Filled.Close, contentDescription = "清空")
                    }
                }
            },
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Go),
            keyboardActions = KeyboardActions(onGo = { onSubmit() }),
        )
        if (progress in 1..99) {
            LinearProgressIndicator(
                progress = { progress / 100f },
                modifier = Modifier.fillMaxWidth().height(2.dp),
            )
        }
    }
}
```

- [ ] **Step 2: 创建 `BrowserToolbar.kt`**

```kotlin
package com.baicaohui.lightweb.ui.browser

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.weight
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.baicaohui.lightweb.R

@Composable
fun BrowserToolbar(
    canGoBack: Boolean,
    canGoForward: Boolean,
    tabCount: Int,
    onBack: () -> Unit,
    onForward: () -> Unit,
    onReload: () -> Unit,
    onTabs: () -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth().height(48.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        IconButton(onClick = onBack, enabled = canGoBack) {
            Icon(BchIcons.Back, contentDescription = stringResource(R.string.action_back))
        }
        IconButton(onClick = onForward, enabled = canGoForward) {
            Icon(BchIcons.Forward, contentDescription = stringResource(R.string.action_forward))
        }
        IconButton(onClick = onReload) {
            Icon(BchIcons.Refresh, contentDescription = stringResource(R.string.action_reload))
        }
        Spacer(modifier = Modifier.weight(1f))
        IconButton(onClick = onTabs) {
            Text(
                text = tabCount.toString(),
                style = MaterialTheme.typography.labelLarge,
            )
        }
    }
}
```

- [ ] **Step 3: 编译验证**

Run: `.\gradlew.bat :app:assembleDebug`
Expected: `BUILD SUCCESSFUL`。

- [ ] **Step 4: 提交**

```powershell
git add app/src/main/java/com/baicaohui/lightweb/ui/browser/AddressBar.kt app/src/main/java/com/baicaohui/lightweb/ui/browser/BrowserToolbar.kt
git commit -m "feat: add address bar and toolbar composables"
```

### Task 4.3: BrowserScreen 完整实现（替换占位）

**Files:**
- Create: `app/src/main/java/com/baicaohui/lightweb/ui/components/ErrorPage.kt`
- Update: `app/src/main/java/com/baicaohui/lightweb/ui/browser/BrowserScreen.kt`（整体替换 Task 2.2 的占位实现）

- [ ] **Step 1: 创建 `ErrorPage.kt`**

```kotlin
package com.baicaohui.lightweb.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.baicaohui.lightweb.R

@Composable
fun ErrorPage(
    onRetry: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.fillMaxSize().padding(32.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Icon(
            imageVector = Icons.Filled.Warning,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.error,
        )
        Text(
            text = stringResource(R.string.error_title),
            style = MaterialTheme.typography.titleMedium,
        )
        Button(onClick = onRetry) {
            Text(stringResource(R.string.error_retry))
        }
    }
}
```

- [ ] **Step 2: 整体替换 `BrowserScreen.kt`**

```kotlin
package com.baicaohui.lightweb.ui.browser

import android.content.Intent
import android.net.Uri
import android.webkit.PermissionRequest
import android.webkit.SslErrorHandler
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.matchParentSize
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.baicaohui.lightweb.BchApp
import com.baicaohui.lightweb.R
import com.baicaohui.lightweb.browser.AdLevel
import com.baicaohui.lightweb.browser.BrowserWebView
import com.baicaohui.lightweb.browser.DownloadHandler
import com.baicaohui.lightweb.browser.PermissionMapping
import com.baicaohui.lightweb.browser.TabStatus
import com.baicaohui.lightweb.browser.WebCallbacks
import com.baicaohui.lightweb.ui.components.ErrorPage

@Composable
fun BrowserScreen() {
    val context = LocalContext.current
    val app = context.applicationContext as BchApp
    val viewModel: BrowserViewModel = viewModel(
        factory = viewModelFactory {
            initializer { BrowserViewModel(app.tabManager) }
        },
    )
    val tabs by viewModel.tabs.collectAsStateWithLifecycle()
    val currentId by viewModel.currentId.collectAsStateWithLifecycle()
    val activeTab = tabs.firstOrNull { it.id == currentId }

    var addressText by remember { mutableStateOf("") }
    var pendingExternal by remember { mutableStateOf<String?>(null) }
    var pendingSsl by remember { mutableStateOf<Pair<String, SslErrorHandler>?>(null) }
    var pendingPermission by remember { mutableStateOf<PermissionRequest?>(null) }
    var webView by remember { mutableStateOf<BrowserWebView?>(null) }

    val downloadHandler = remember { DownloadHandler(context) }

    val callbacks = remember(viewModel) {
        object : WebCallbacks {
            override fun onProgress(progress: Int) = viewModel.onProgress(progress)

            override fun onPageStarted(url: String) {
                viewModel.onPageStarted(url)
                addressText = url
            }

            override fun onPageFinished(url: String) = viewModel.onPageFinished(url)

            override fun onTitleChanged(title: String) = viewModel.onTitle(title)

            override fun onDownloadStart(
                url: String,
                userAgent: String,
                contentDisposition: String?,
                mimeType: String?,
            ) = viewModel.onDownload(url, userAgent, mimeType)

            override fun onPermissionRequest(request: PermissionRequest) =
                viewModel.onPermissionRequest(request)

            override fun onExternalScheme(url: String) = viewModel.onExternalScheme(url)

            override fun onMainFrameError(failingUrl: String, code: Int, description: String) =
                viewModel.onError(failingUrl)

            override fun onSslError(url: String, handler: SslErrorHandler) =
                viewModel.onSslError(url, handler)
        }
    }

    LaunchedEffect(viewModel) {
        viewModel.events.collect { event ->
            when (event) {
                is BrowserEvent.Reload -> webView?.reload()
                is BrowserEvent.Navigate -> webView?.loadUrl(event.url)
                is BrowserEvent.Download -> {
                    downloadHandler.start(event.url, event.userAgent, event.mimeType)
                    Toast.makeText(context, R.string.download_started, Toast.LENGTH_SHORT).show()
                }
                is BrowserEvent.ExternalScheme -> pendingExternal = event.url
                is BrowserEvent.PermissionRequest -> pendingPermission = event.request
                is BrowserEvent.SslError -> pendingSsl = event.url to event.handler
            }
        }
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions(),
    ) { grants ->
        val request = pendingPermission ?: return@rememberLauncherForActivityResult
        if (grants.values.all { it }) request.grant(request.resources) else request.deny()
        pendingPermission = null
    }

    val canGoBack = webView?.canGoBack() == true
    BackHandler(enabled = canGoBack) { webView?.goBack() }

    Column(modifier = Modifier.fillMaxSize()) {
        BrowserToolbar(
            canGoBack = canGoBack,
            canGoForward = webView?.canGoForward() == true,
            tabCount = tabs.size,
            onBack = { webView?.goBack() },
            onForward = { webView?.goForward() },
            onReload = { webView?.reload() },
            onTabs = { /* M2 打开标签页总览 */ },
        )
        AddressBar(
            value = addressText,
            onValueChange = { addressText = it },
            onSubmit = {
                viewModel.submitInput(addressText, "https://www.bing.com/search?q=%s")
            },
            progress = activeTab?.progress ?: 0,
        )
        Box(modifier = Modifier.fillMaxSize()) {
            AndroidView(
                factory = { ctx ->
                    BrowserWebView(
                        context = ctx,
                        callbacks = callbacks,
                        adBlocker = app.adBlocker,
                        adLevel = { AdLevel.BASIC },
                    ).also { wv ->
                        webView = wv
                        val target = initialUrl
                            ?: viewModel.tabs.value.firstOrNull { it.id == viewModel.currentId.value }?.url
                        if (!target.isNullOrBlank()) {
                            if (viewModel.currentId.value == null) viewModel.newTab(target)
                            wv.loadUrl(target)
                        }
                    }
                },
                update = {},
                modifier = Modifier.fillMaxSize(),
            )
            if (activeTab?.status == TabStatus.ERROR) {
                ErrorPage(
                    onRetry = viewModel::retry,
                    modifier = Modifier.matchParentSize(),
                )
            }
        }

        pendingExternal?.let { url ->
            AlertDialog(
                onDismissRequest = { pendingExternal = null },
                title = { Text(stringResource(R.string.external_scheme_title)) },
                text = { Text(stringResource(R.string.external_scheme_message, url)) },
                confirmButton = {
                    TextButton(
                        onClick = {
                            pendingExternal = null
                            runCatching {
                                context.startActivity(
                                    Intent(Intent.ACTION_VIEW, Uri.parse(url))
                                        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
                                )
                            }
                        },
                    ) {
                        Text(stringResource(R.string.dialog_allow))
                    }
                },
                dismissButton = {
                    TextButton(onClick = { pendingExternal = null }) {
                        Text(stringResource(R.string.dialog_cancel))
                    }
                },
            )
        }

        pendingSsl?.let { (url, handler) ->
            AlertDialog(
                onDismissRequest = { handler.cancel(); pendingSsl = null },
                title = { Text(stringResource(R.string.ssl_warning_title)) },
                text = { Text(stringResource(R.string.ssl_warning_message, url)) },
                confirmButton = {
                    TextButton(
                        onClick = {
                            handler.proceed()
                            pendingSsl = null
                        },
                    ) {
                        Text(stringResource(R.string.dialog_continue))
                    }
                },
                dismissButton = {
                    TextButton(onClick = { handler.cancel(); pendingSsl = null }) {
                        Text(stringResource(R.string.dialog_cancel))
                    }
                },
            )
        }

        pendingPermission?.let { request ->
            val needed = PermissionMapping.androidPermissions(request.resources)
            AlertDialog(
                onDismissRequest = {
                    request.deny()
                    pendingPermission = null
                },
                title = { Text(stringResource(R.string.permission_dialog_title)) },
                text = {
                    Text(
                        stringResource(
                            R.string.permission_dialog_message,
                            request.origin.toString(),
                            PermissionMapping.describe(request.resources),
                        ),
                    )
                },
                confirmButton = {
                    TextButton(
                        onClick = {
                            if (needed.isEmpty()) {
                                request.grant(request.resources)
                                pendingPermission = null
                            } else {
                                permissionLauncher.launch(needed.toTypedArray())
                            }
                        },
                    ) {
                        Text(stringResource(R.string.dialog_allow))
                    }
                },
                dismissButton = {
                    TextButton(onClick = { request.deny(); pendingPermission = null }) {
                        Text(stringResource(R.string.dialog_cancel))
                    }
                },
            )
        }
    }
}
```

- [ ] **Step 3: 编译验证**

Run: `.\gradlew.bat :app:assembleDebug`
Expected: `BUILD SUCCESSFUL`。

- [ ] **Step 4: 提交**

```powershell
git add app/src/main/java/com/baicaohui/lightweb/ui/components/ErrorPage.kt app/src/main/java/com/baicaohui/lightweb/ui/browser/BrowserScreen.kt
git commit -m "feat: implement browser screen with WebView integration"
```

### Task 4.4: 外部链接入口（ACTION_VIEW 深链）

**Files:**
- Update: `app/src/main/java/com/baicaohui/lightweb/BchApp.kt`
- Update: `app/src/main/java/com/baicaohui/lightweb/MainActivity.kt`
- Update: `app/src/main/java/com/baicaohui/lightweb/ui/BchAppRoot.kt`

- [ ] **Step 1: `BchApp.kt` 增加待打开 URL 暂存字段**

在 `val adBlocker: AdBlocker by lazy { AdBlocker.fromResources(this) }` 之后新增：

```kotlin
    @Volatile
    var pendingUrl: String? = null
```

- [ ] **Step 2: `MainActivity.kt` 读取 intent 数据**

在 `setContent {` 之前新增：

```kotlin
        (application as BchApp).pendingUrl = intent?.data?.toString()
```

- [ ] **Step 3: `BchAppRoot.kt` 按深链选择起始页并传递 URL**

在 `BchAppRoot()` 函数体开头（`val navController = ...` 之前）新增：

```kotlin
        val context = LocalContext.current
        val app = context.applicationContext as BchApp
        val startDestination = if (app.pendingUrl.isNullOrBlank()) {
            BchRoute.HOME.route
        } else {
            BchRoute.BROWSER.route
        }
```

将 `NavHost(startDestination = BchRoute.HOME.route, ...)` 改为：

```kotlin
        NavHost(
            navController = navController,
            startDestination = startDestination,
            modifier = Modifier.padding(innerPadding),
        ) {
```

将 `composable(BchRoute.BROWSER.route) { BrowserScreen() }` 改为：

```kotlin
            composable(BchRoute.BROWSER.route) {
                BrowserScreen(initialUrl = app.pendingUrl.also { app.pendingUrl = null })
            }
```

顶部补两个 import：

```kotlin
import androidx.compose.ui.platform.LocalContext
import com.baicaohui.lightweb.BchApp
```

- [ ] **Step 4: 编译验证**

Run: `.\gradlew.bat :app:assembleDebug`
Expected: `BUILD SUCCESSFUL`。

- [ ] **Step 5: 提交**

```powershell
git add app/src/main/java/com/baicaohui/lightweb/BchApp.kt app/src/main/java/com/baicaohui/lightweb/MainActivity.kt app/src/main/java/com/baicaohui/lightweb/ui/BchAppRoot.kt
git commit -m "feat: open http links from external apps"
```

### Task 4.5: 集成验证

**Files:** 无新增（仅运行验证）

- [ ] **Step 1: 全量单元测试**

Run: `.\gradlew.bat :app:testDebugUnitTest`
Expected: `BUILD SUCCESSFUL`，全部用例 PASS（UrlSecurity 8、TabManager 6、AdBlocker 5、PermissionMapping 4、TonalPaletteGenerator 3、ThemePrefs 2、BrowserViewModel 4，共 32 个）。

- [ ] **Step 2: debug 构建**

Run: `.\gradlew.bat :app:assembleDebug`
Expected: `BUILD SUCCESSFUL`，产物 `app/build/outputs/apk/debug/app-debug.apk`。

- [ ] **Step 3: release 构建（验证 R8 minify + shrinkResources）**

Run: `.\gradlew.bat :app:assembleRelease`
Expected: `BUILD SUCCESSFUL`，产物 `app/build/outputs/apk/release/app-release.apk` 体积小于 12MB。

- [ ] **Step 4: 手动冒烟清单（需真机/模拟器，安装 `installDebug`）**

```text
1. 启动 → 主页显示「BCH 白草灰」与「打开浏览器」按钮，底部导航五项齐全
2. 点「打开浏览器」→ 进入浏览页，地址栏可输入
3. 输入 example.com → 页面加载，地址栏显示 https://example.com，进度条出现
4. 页面内点击链接 → 同页跳转；返回/前进按钮状态正确；物理返回键先回退网页
5. 输入「hello world」→ 跳转 Bing 搜索
6. 输入 intent://test → 弹「打开外部应用？」对话框，取消后不跳转
7. 访问 https://self-signed.badssl.com/ → 弹 SSL 警告；「继续」可打开，「取消」留在原页
8. 访问含下载链接的页面 → Toast「开始下载」，系统通知栏出现下载任务
9. 访问 https://wrong.host.badssl.com/ → 错误页显示，点「重试」重新加载
10. 深色/浅色跟随系统切换（系统切深色后应用背景变化）
11. 系统浏览器/其他应用点开一个 http(s) 链接并选择 BCH → 直接进入浏览页加载该 URL
```

- [ ] **Step 5: 如设备可用，运行导航冒烟测试**

Run: `.\gradlew.bat :app:connectedDebugAndroidTest --tests "com.baicaohui.lightweb.NavigationSmokeTest"`
Expected: 1 个用例 PASS。

- [ ] **Step 6: 提交（如冒烟发现缺陷，先修复并补对应用例再提交）**

```powershell
git add -A
git commit -m "chore: verify M1 integration"
```

### Task 4.6: 工程 AGENTS.md

**Files:**
- Create: `AGENTS.md`

- [ ] **Step 1: 创建 `AGENTS.md`**

```markdown
# AGENTS.md — BCH（白草灰）浏览器

## 项目身份
- Android 应用，包名 `com.baicaohui.lightweb`，应用名 BCH（中文名：白草灰）
- 单 Activity + Jetpack Compose（Material 3）+ Navigation Compose + MVVM
- 渲染内核为系统 WebView（`browser/` 包封装安全策略）

## 构建命令
- Debug APK: `.\gradlew.bat :app:assembleDebug` → `app/build/outputs/apk/debug/app-debug.apk`
- 单元测试: `.\gradlew.bat :app:testDebugUnitTest`
- 仪器测试: `.\gradlew.bat :app:connectedDebugAndroidTest`
- 安装: `.\gradlew.bat :app:installDebug`

## 环境
- Windows / Java 22（编译目标 Java 17）/ Android SDK `D:\AndroidSDK`（`local.properties`，不入库）
- Gradle 8.14.3 wrapper；若 wrapper 下载遇 PKIX 证书问题，用 Android Studio JBR 直调缓存发行版：
  `C:\Users\杨镇豪\.gradle\wrapper\dists\gradle-8.14.3-all\10utluxaxniiv4wxiphsi49nj\gradle-8.14.3\bin\gradle.bat`

## 关键目录
- `browser/`：WebView 封装、URL 安全、Tab 管理、广告拦截、下载、权限映射（不依赖 UI）
- `ui/theme/`：MD3 主题令牌与 seed 调色板生成
- `data/prefs/`：DataStore 配置（主题/主页/浏览偏好）
- `ui/`：Compose 界面与导航

## 已知范围边界（截至 M1）
- 单 WebView 实例承载当前 Tab；Tab 切换的多 WebView 与会话恢复在 M2
- 主页组件化、设置页全量自定义在 M3/M4
```

- [ ] **Step 2: 提交**

```powershell
git add AGENTS.md
git commit -m "docs: add BCH project AGENTS.md"
```

---

## 5. 本计划验收标准

- [ ] 全部 32 个单元测试通过
- [ ] `assembleDebug` 与 `assembleRelease` 均 BUILD SUCCESSFUL
- [ ] 手动冒烟清单 10 项全部通过（无设备时至少完成项 1–4，其余留待有设备时补测）
- [ ] 每次提交后工作树干净（`git status` 无未提交改动）

## 6. 后续计划（独立文档，本计划完成后各立一份）

| 计划 | 内容 | 建议文档名 |
|---|---|---|
| M2 | Room 数据层、书签（文件夹+HTML 导入导出）、历史、多 WebView Tab 与会话恢复 | `YYYY-MM-DD-bch-browser-m2.md` |
| M3 | 组件化主页、编辑模式、背景、快捷拨号 | `YYYY-MM-DD-bch-browser-m3.md` |
| M4 | 设置页全量自定义：外观色盘、工具栏双模式、站点级设置、搜索引擎、清除数据 | `YYYY-MM-DD-bch-browser-m4.md` |
| M5 | 无障碍、空态/错误态打磨、性能调优、图标与上架材料 | `YYYY-MM-DD-bch-browser-m5.md` |
