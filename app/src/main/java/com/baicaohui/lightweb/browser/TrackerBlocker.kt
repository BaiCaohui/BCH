package com.baicaohui.lightweb.browser

import android.content.Context
import com.baicaohui.lightweb.R

/** 跨站跟踪器拦截：内置域名列表，主机或子域匹配。 */
class TrackerBlocker(private val hosts: Set<String>) {

    fun isTracker(url: String): Boolean {
        val host = UrlSecurity.extractHost(url) ?: return false
        return hosts.any { it == host || host.endsWith(".$it") }
    }

    companion object {
        fun fromResources(context: Context): TrackerBlocker =
            TrackerBlocker(
                context.resources.openRawResource(R.raw.trackers).bufferedReader().readLines()
                    .map { it.trim().lowercase() }
                    .filter { it.isNotEmpty() && !it.startsWith("#") }
                    .toSet(),
            )
    }
}
