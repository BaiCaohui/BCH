# BCH（白草灰）轻量安卓浏览器

单 Activity + Jetpack Compose（Material 3）+ Navigation Compose + MVVM 的轻量 Android 浏览器，
渲染内核为系统 WebView（`browser/` 包统一封装安全策略），包名 `com.baicaohui.lightweb`。

## 功能

- 多标签浏览：独立 WebView 按标签承载、LRU 软回收、冷启动恢复标签列表
- 书签 / 历史 / 快捷拨号 / 站点设置，Room 持久化
- 阅读模式：Mozilla Readability 本地正文提取、字号/主题调节、全文离线缓存（无痕模式不读写缓存）
- 无痕模式：独立进程 + 独立 WebView 数据目录，退出即清除
- 资源嗅探：扫描当前网页视频 / 音频 / 图片资源，展示缩略图、名称与文件大小，一键下载
- 长按菜单：文字（复制 / 全选 / 新标签页搜索）、链接（打开 / 无痕打开 / 复制 / 下载 / 分享）、图片（打开 / 复制 / 下载）
- 下载管理：内置下载器 / 系统下载器，危险文件（APK/EXE 等）下载前警告；断点续传、实时进度与速度、暂停 / 继续 / 全部暂停；条目长按可打开 / 分享 / 删除（可选删除源文件），点击条目用默认应用打开
- 下载当前网页：一键把当前页面保存为 HTML 文件（以页面标题命名）到下载目录
- 缓存当前网页 / 缓存管理：离线保存当前网页快照；缓存管理支持文件夹、移动、多选批量删除、点击离线打开缓存内容，操作逻辑与收藏夹一致
- 默认浏览器：设置中一键申请成为系统默认浏览器（Android 11+ 系统角色弹窗，旧版本跳转默认应用设置）
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
.\gradlew.bat :app:assembleRelease      # Release APK（R8 + 已签名）
.\gradlew.bat :app:testDebugUnitTest    # 单元测试（234 个用例）
```

产物位于 `app/build/outputs/apk/`；已签名 Release 副本同步到 `release/app-release.apk`。

## 发布与签名

`release/` 目录存放当前 Release 产物（R8 混淆 + 资源压缩）。签名配置已就绪：

- `release.jks` 与 `key.properties` 位于项目根目录，两者均已加入 `.gitignore`，不会入库。
- `app/build.gradle.kts` 会在 `key.properties` 存在时自动为 `release` 构建类型签名；
  文件缺失时回退为未签名构建（不会导致构建失败）。
- 重新运行 `.\gradlew.bat :app:assembleRelease` 即可生成已签名 APK。


## 许可证

Apache-2.0。阅读模式使用 Mozilla Readability（Apache-2.0）。
