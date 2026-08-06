package com.baicaohui.lightweb.browser

import java.io.File

/**
 * 无痕隔离常量与纯函数。
 *
 * 单进程内 WebView 的 Cookie/网站数据全局共享，无法按 WebView 隔离；
 * 唯一可靠做法是让无痕 WebView 运行在独立进程并使用独立数据目录
 * （`WebView.setDataDirectorySuffix`，API 28+），退出时删除该目录。
 */
object IncognitoProcess {
    const val SUFFIX = "incognito"

    fun isIncognitoProcessName(processName: String, packageName: String): Boolean =
        processName == "$packageName:$SUFFIX"

    fun dataDir(dataDir: File): File = File(dataDir, "app_webview_$SUFFIX")

    /**
     * 删除无痕 WebView 数据目录。WebView 会把 `webview_data.lock` 放在该目录根部，
     * 若上次无痕进程异常退出（崩溃/被杀）导致锁文件残留，新进程初始化时会报
     * “Failed to create lock file”。因此先删除锁文件再递归清空整个目录，
     * 并重试若干次，避免与旧进程退出的竞态。
     * 注意：系统 WebView 初始化只打开 `webview_data.lock`，不会创建数据目录本身，
     * 所以清空后必须重建空目录，否则初始化会因 ENOENT 崩溃。
     *
     * @return 目录最终存在且为空返回 true，否则 false。
     */
    fun purgeDataDir(
        dataDir: File,
        attempts: Int = 4,
        retryDelayMs: Long = 50,
        deleteDir: (File) -> Boolean = { it.deleteRecursively() },
    ): Boolean {
        val dir = dataDir(dataDir)
        var remaining = attempts
        while (remaining-- > 0) {
            if (dir.exists()) {
                runCatching { File(dir, "webview_data.lock").delete() }
                if (!deleteDir(dir)) {
                    if (retryDelayMs > 0 && remaining > 0) Thread.sleep(retryDelayMs)
                    continue
                }
            }
            if (dir.isDirectory) {
                if (dir.listFiles().isNullOrEmpty()) return true
            } else if (dir.mkdirs()) {
                return true
            }
            if (retryDelayMs > 0 && remaining > 0) Thread.sleep(retryDelayMs)
        }
        return dir.isDirectory && dir.listFiles().isNullOrEmpty()
    }
}
