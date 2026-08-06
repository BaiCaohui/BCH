# BCH（白草灰）轻量安卓浏览器

单 Activity + Jetpack Compose（Material 3）+ Navigation Compose + MVVM 的轻量 Android 浏览器，
渲染内核为系统 WebView（`browser/` 包统一封装安全策略），包名 `com.baicaohui.lightweb`。

## 功能

- 多标签浏览：独立 WebView 按标签承载、LRU 软回收、冷启动恢复标签列表
- 书签 / 历史 / 快捷拨号 / 站点设置，Room 持久化
- 阅读模式：Mozilla Readability 本地正文提取、字号/主题调节、全文离线缓存（无痕模式不读写缓存）
- 无痕模式：独立进程 + 独立 WebView 数据目录，退出即清除
- 下载管理：内置下载器 / 系统下载器，危险文件（APK/EXE 等）下载前警告
- 隐私与安全：
  - 智能反追踪（拦截已知跨站跟踪器）
  - 站点权限精细控制（位置 / 摄像头 / 麦克风 / 通知 / 弹窗 / 自动播放）
  - HTTPS 升级（优先 / 强制）+ 不安全连接警告
  - Cookie 与站点数据管理（查看 / 删除 / 关闭浏览器即清除）
  - Safe Browsing（恶意网站 / 钓鱼）+ 危险下载拦截
  - 广告拦截（内置基础 / 严格两级列表）+ 用户自定义屏蔽规则

## 构建

环境：JDK 17+（推荐 Android Studio JBR）、Android SDK（在 `local.properties` 中配置 `sdk.dir`）。

```powershell
.\gradlew.bat :app:assembleDebug        # Debug APK
.\gradlew.bat :app:assembleRelease      # Release APK（R8，未签名）
.\gradlew.bat :app:testDebugUnitTest    # 单元测试（189 个用例）
```

产物位于 `app/build/outputs/apk/`。发布前需配置 `signingConfigs` 签名。

## 许可证

Apache-2.0。阅读模式使用 Mozilla Readability（Apache-2.0）。
