package com.baicaohui.lightweb.data.prefs

import java.io.File

/**
 * 无痕进程使用独立的 DataStore 文件（快照副本），
 * 避免与主进程同时访问同一 DataStore 文件（官方不支持跨进程共用普通 DataStore）。
 */
object IncognitoPrefsFiles {

    val NAMES = listOf("browser", "theme", "home", "recent_searches")

    fun targetFile(dir: File, name: String): File =
        File(dir, "${name}_incognito.preferences_pb")

    fun copyMainPrefsToIncognito(dir: File) {
        NAMES.forEach { name ->
            val source = File(dir, "$name.preferences_pb")
            val target = targetFile(dir, name)
            if (source.exists()) {
                runCatching { source.copyTo(target, overwrite = true) }
            } else {
                runCatching { target.delete() }
            }
        }
    }
}
