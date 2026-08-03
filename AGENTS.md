# AGENTS.md — BCH（白草灰）浏览器

## 项目身份
- Android 应用，包名 `com.baicaohui.lightweb`，应用名 BCH（中文名：白草灰）
- 单 Activity + Jetpack Compose（Material 3）+ Navigation Compose + MVVM
- 渲染内核为系统 WebView（`browser/` 包封装安全策略）

## 构建命令
- Debug APK: `.\gradlew.bat :app:assembleDebug` → `app/build/outputs/apk/debug/app-debug.apk`
- Release APK（R8，未签名）: `.\gradlew.bat :app:assembleRelease` → `app/build/outputs/apk/release/app-release-unsigned.apk`
- 单元测试: `.\gradlew.bat :app:testDebugUnitTest`
- 仪器测试: `.\gradlew.bat :app:connectedDebugAndroidTest`
- 安装: `.\gradlew.bat :app:installDebug`

## 环境（本机 Windows 专属）
- Java 22（编译目标 Java 17）；Android SDK `D:\AndroidSDK`（`local.properties`，不入库）
- Gradle 8.14.3 wrapper；若 wrapper 下载遇 PKIX 证书问题，用 Android Studio JBR 直调缓存发行版：
  `C:\Users\杨镇豪\.gradle\wrapper\dists\gradle-8.14.3-all\10utluxaxniiv4wxiphsi49nj\gradle-8.14.3\bin\gradle.bat`
- **单元测试必须设置 `GRADLE_USER_HOME=D:\gradle-home`**：本机用户名含中文（杨镇豪），Gradle 测试 worker 用 @argfile 传类路径，Java 按 GBK 解码会损坏中文路径导致 `ClassNotFoundException: GradleWorkerMain`。D:\gradle-home 已复制完整缓存（含 wrapper 发行版），所有 `gradlew` 命令前先执行：
  ```powershell
  $env:GRADLE_USER_HOME = "D:\gradle-home"
  $env:JAVA_HOME = "C:\Program Files\Android\Android Studio\jbr"
  $env:Path = "$env:JAVA_HOME\bin;$env:Path"
  ```

## 关键目录
- `browser/`：WebView 封装、URL 安全、Tab 管理、广告拦截、下载、权限映射（不依赖 UI）
- `ui/theme/`：MD3 主题令牌与 seed 调色板生成（纯 Kotlin HSV 实现，JVM 可测）
- `data/prefs/`：DataStore 配置（主题/主页/浏览偏好）
- `ui/`：Compose 界面与导航

## 已知范围边界（截至 M1）
## 范围边界（截至 M4）
- 多 WebView 按 Tab 独立承载（`WebViewStore` LRU 销毁），冷启动恢复标签列表（URL+标题）
- Room 数据层：书签（文件夹/导入导出）、历史（按天分组）、快捷拨号、站点设置
- 主页组件化：搜索/快捷拨号/最近访问/书签/时钟 + 背景与遮罩 + 编辑页
- 设置全量自定义：外观（预设/HSV/动态取色/深色/字号/圆角/密度）、工具栏、搜索引擎、浏览、隐私清除、站点设置、关于
