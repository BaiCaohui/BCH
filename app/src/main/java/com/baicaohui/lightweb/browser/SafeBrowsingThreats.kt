package com.baicaohui.lightweb.browser

import com.baicaohui.lightweb.R

/**
 * Safe Browsing 威胁类型 → 文案。
 * 威胁类型常量来自 WebViewClient（API 27+）：1=恶意软件、2=钓鱼、3=有害软件、4=计费欺诈，其余按未知处理。
 */
object SafeBrowsingThreats {

    fun labelRes(threatType: Int): Int = when (threatType) {
        1 -> R.string.safe_browsing_malware
        2 -> R.string.safe_browsing_phishing
        3 -> R.string.safe_browsing_unwanted
        4 -> R.string.safe_browsing_billing
        else -> R.string.safe_browsing_unknown
    }
}
