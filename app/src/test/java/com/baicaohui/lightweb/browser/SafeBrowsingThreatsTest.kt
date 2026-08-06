package com.baicaohui.lightweb.browser

import com.baicaohui.lightweb.R
import org.junit.Assert.assertEquals
import org.junit.Test

class SafeBrowsingThreatsTest {

    @Test
    fun `maps known threat types to labels`() {
        assertEquals(
            R.string.safe_browsing_malware,
            SafeBrowsingThreats.labelRes(1),
        )
        assertEquals(
            R.string.safe_browsing_phishing,
            SafeBrowsingThreats.labelRes(2),
        )
        assertEquals(
            R.string.safe_browsing_unwanted,
            SafeBrowsingThreats.labelRes(3),
        )
    }

    @Test
    fun `unknown threat maps to unknown label`() {
        assertEquals(
            R.string.safe_browsing_unknown,
            SafeBrowsingThreats.labelRes(9_999),
        )
    }
}
