package com.baicaohui.lightweb.browser

enum class DownloadRisk { LOW, HIGH }

/** 下载风险判定：可执行扩展名/MIME 视为高危，下载前需用户明确确认。 */
object DownloadRiskPolicy {

    private val HIGH_EXTENSIONS = setOf(
        "apk", "exe", "bat", "cmd", "com", "scr", "msi", "jar", "vbs", "ps1", "pif", "hta", "cpl",
    )

    private val HIGH_MIME_TYPES = setOf(
        "application/vnd.android.package-archive",
        "application/x-msdownload",
        "application/x-msdos-program",
        "application/x-bat",
        "application/x-msi",
    )

    fun riskOf(url: String, mimeType: String?): DownloadRisk {
        if (mimeType?.lowercase() in HIGH_MIME_TYPES) return DownloadRisk.HIGH
        val lower = url.lowercase()
        val pattern = Regex("\\.(${HIGH_EXTENSIONS.joinToString("|")})(?:[?#]|$)")
        return if (pattern.containsMatchIn(lower)) DownloadRisk.HIGH else DownloadRisk.LOW
    }
}
