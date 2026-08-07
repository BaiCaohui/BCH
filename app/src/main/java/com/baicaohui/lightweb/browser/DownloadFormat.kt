package com.baicaohui.lightweb.browser

import java.util.Locale

object DownloadFormat {

    fun formatBytes(bytes: Long): String {
        val value = bytes.coerceAtLeast(0)
        return when {
            value >= 1_073_741_824 ->
                String.format(Locale.US, "%.1f GB", value / 1_073_741_824.0)
            value >= 1_048_576 ->
                String.format(Locale.US, "%.1f MB", value / 1_048_576.0)
            value >= 1024 ->
                String.format(Locale.US, "%.1f KB", value / 1024.0)
            else -> "$value B"
        }
    }

    fun formatSpeed(bytesPerSecond: Long): String =
        "${formatBytes(bytesPerSecond.coerceAtLeast(0))}/s"
}
