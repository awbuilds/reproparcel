// SPDX-License-Identifier: AGPL-3.0-only

package com.awbuilds.reproparcel.spike.capturelab

import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import org.junit.Rule
import org.junit.Test

/**
 * Test-only observation of Compose semantics coverage.
 *
 * This does not prove that a production app can extract the same tree through
 * public APIs; the separate app-level traversal test records that limitation.
 */
class ComposeSemanticsCoverageTest {
    @get:Rule
    val composeRule = createAndroidComposeRule<MainActivity>()

    @Test
    fun coreComposeContentIsRepresented() {
        composeRule.onNodeWithText("Capture Lab").assertExists()
        composeRule.onNodeWithText("Capture").assertExists()
        composeRule.onNodeWithText("Synthetic input").assertExists()
        composeRule.onNodeWithContentDescription("Synthetic canvas").assertExists()
        composeRule.onNodeWithText("Synthetic row 1").assertExists()
    }

    @Test
    fun dialogContentIsRepresented() {
        composeRule.onNodeWithText("Dialog").performScrollTo().performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithText("Synthetic dialog", useUnmergedTree = true).assertExists()
        composeRule.onNodeWithText(
            "This dialog tests capture and semantics coverage.",
            useUnmergedTree = true,
        ).assertExists()
    }
}
