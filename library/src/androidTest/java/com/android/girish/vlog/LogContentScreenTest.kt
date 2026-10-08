/*
 * The MIT License (MIT)
 *
 * Copyright (c) 2020 Girish Budhwani
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

package com.android.girish.vlog

import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsFocused
import androidx.compose.ui.test.assertIsNotFocused
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class LogContentScreenTest {
    @get:Rule
    val composeRule = createComposeRule()

    private val shortLog = VlogModel(VlogModel.INFO, "Surface", "A short message")
    private val longMessage = "L".repeat(80)
    private val longLog = VlogModel(VlogModel.ERROR, "Choreographer", longMessage)

    private fun setScreen(
        logs: List<VlogModel> = listOf(shortLog, longLog),
        tags: List<String> = listOf("Surface", "Choreographer"),
        onKeywordChange: (String) -> Unit = {},
        onPriorityIndexSelected: (Int) -> Unit = {},
        onTagsSelected: (Set<String>) -> Unit = {},
        onClearLogs: () -> Unit = {},
        onExportLogs: (List<VlogModel>) -> Unit = {},
    ) {
        composeRule.setContent {
            VlogTheme {
                LogContentScreen(
                    title = "Sample App v1.2",
                    logs = logs,
                    tags = tags,
                    onKeywordChange = onKeywordChange,
                    onPriorityIndexSelected = onPriorityIndexSelected,
                    onTagsSelected = onTagsSelected,
                    onClearLogs = onClearLogs,
                    onExportLogs = onExportLogs,
                )
            }
        }
    }

    @Test
    fun showsAppTitleInTheHeader() {
        setScreen()

        composeRule.onNodeWithText("Sample App v1.2").assertIsDisplayed()
    }

    @Test
    fun logsWithTheSameIdShareARowShowingTheId() {
        val request = VlogModel(VlogModel.INFO, "Net", "--> GET /a", id = "GET /a #1")
        val response = VlogModel(VlogModel.ERROR, "Net", "<-- 500 /a", id = "GET /a #1")
        setScreen(logs = listOf(shortLog, request, response), tags = emptyList())

        // One id label for the two logs, and both of them are shown
        composeRule.onAllNodesWithText("GET /a #1").assertCountEquals(1)
        composeRule.onNodeWithText("--> GET /a").assertIsDisplayed()
        composeRule.onNodeWithText("<-- 500 /a").assertIsDisplayed()
        composeRule.onNodeWithText("A short message").assertIsDisplayed()
    }

    @Test
    fun showsAChipForEveryTag() {
        setScreen()

        composeRule.onNodeWithText("Surface").assertIsDisplayed()
        composeRule.onNodeWithText("Choreographer").assertIsDisplayed()
    }

    @Test
    fun showsNoChipsWithoutTags() {
        setScreen(tags = emptyList())

        composeRule.onNodeWithText("Surface").assertDoesNotExist()
    }

    @Test
    fun togglingChipsReportsTheSelectedTags() {
        val reported = mutableListOf<Set<String>>()
        setScreen(onTagsSelected = { reported.add(it) })

        composeRule.onNodeWithText("Surface").performClick()
        composeRule.onNodeWithText("Choreographer").performClick()
        composeRule.onNodeWithText("Surface").performClick()

        assertEquals(
            listOf(setOf("Surface"), setOf("Surface", "Choreographer"), setOf("Choreographer")),
            reported,
        )
    }

    @Test
    fun showsLogsWithPriorityAndTag() {
        setScreen()

        composeRule.onNodeWithText("I/Surface: ").assertIsDisplayed()
        composeRule.onNodeWithText("A short message").assertIsDisplayed()
        composeRule.onNodeWithText("E/Choreographer: ").assertIsDisplayed()
    }

    @Test
    fun longMessageIsTruncatedUntilTheLogIsExpanded() {
        setScreen()
        val truncated = longMessage.substring(0, 49) + "..."

        composeRule.onNodeWithText(truncated).assertIsDisplayed()

        composeRule.onNodeWithText("E/Choreographer: ").performClick()

        composeRule.onNodeWithText(longMessage).assertIsDisplayed()
    }

    @Test
    fun typingInTheFilterReportsTheKeyword() {
        var keyword = ""
        setScreen(onKeywordChange = { keyword = it })

        composeRule.onNodeWithText("Enter filter text").performTextInput("surface")

        assertEquals("surface", keyword)
    }

    @Test
    fun selectingAPriorityReportsItsIndexAndUpdatesTheLabel() {
        var selectedIndex = -1
        setScreen(onPriorityIndexSelected = { selectedIndex = it })

        composeRule.onNodeWithText("Log Priority").performClick()
        composeRule.onNodeWithText("Warn").performClick()

        assertEquals(3, selectedIndex)
        composeRule.onNodeWithText("Warn").assertIsDisplayed()
    }

    @Test
    fun tappingOutsideTheFilterFieldClearsItsFocus() {
        setScreen()
        val filterField = composeRule.onNode(hasSetTextAction())

        filterField.performTextInput("surface")
        filterField.assertIsFocused()

        composeRule.onNodeWithText("Sample App v1.2").performClick()

        filterField.assertIsNotFocused()
    }

    @Test
    fun tappingALogClearsTheFilterFieldFocus() {
        setScreen()
        val filterField = composeRule.onNode(hasSetTextAction())

        filterField.performTextInput("surface")
        composeRule.onNodeWithText("I/Surface: ").performClick()

        filterField.assertIsNotFocused()
    }

    @Test
    fun exportActionReportsTheLogsThatAreShown() {
        var exported: List<VlogModel> = emptyList()
        setScreen(onExportLogs = { exported = it })

        composeRule.onNodeWithContentDescription("Export the last 10 seconds").performClick()

        assertEquals(listOf(shortLog, longLog), exported)
    }

    @Test
    fun clearButtonReportsClick() {
        var cleared = false
        setScreen(onClearLogs = { cleared = true })

        composeRule.onNodeWithContentDescription("Clear").performClick()

        assertEquals(true, cleared)
    }
}
