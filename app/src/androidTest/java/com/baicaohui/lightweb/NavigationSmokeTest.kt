package com.baicaohui.lightweb

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import org.junit.Rule
import org.junit.Test

class NavigationSmokeTest {

    @get:Rule
    val composeRule = createAndroidComposeRule<MainActivity>()

    @Test
    fun bottomBarSwitchesDestinations() {
        composeRule.onNodeWithText("历史").performClick()
        composeRule.onNodeWithText("暂无浏览历史").assertIsDisplayed()

        composeRule.onNodeWithText("书签").performClick()
        composeRule.onNodeWithText("暂无书签").assertIsDisplayed()
    }
}
