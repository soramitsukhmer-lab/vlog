/*
 * The MIT License (MIT)
 *
 * Copyright (c) 2026 Chanrithy Thim
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

import androidx.compose.ui.semantics.SemanticsActions
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
import androidx.compose.ui.text.TextLayoutResult
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
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
        collapseAfterLines: Int? = null,
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
                    collapseAfterLines = collapseAfterLines,
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
    fun aLongMessageIsShownInFull() {
        // 3000 characters wrap to far more lines than the 20 a log used to be limited to
        val message = "word ".repeat(600).trim()
        setScreen(logs = listOf(VlogModel(VlogModel.INFO, "Net", message)), tags = emptyList())

        val layout = layoutOf(message)
        assertTrue("the message wraps to more than 20 lines", layout.lineCount > 20)
        assertFalse("nothing is clipped", layout.didOverflowHeight)
        assertFalse("no line ends in an ellipsis", (0 until layout.lineCount).any { layout.isLineEllipsized(it) })
    }

    @Test
    fun aLongLogIsCollapsedWhenAskedToAndOpensWithTheArrow() {
        val message = "word ".repeat(200).trim()
        setScreen(logs = listOf(VlogModel(VlogModel.INFO, "Net", message)), tags = emptyList(), collapseAfterLines = 3)

        // Cut after 3 lines, with the way to show it in full
        val collapsed = layoutOf(message)
        assertEquals(3, collapsed.lineCount)
        assertTrue(collapsed.hasVisualOverflow)
        composeRule.onNodeWithContentDescription("Show more").assertIsDisplayed()

        composeRule.onNodeWithContentDescription("Show more").performClick()

        val expanded = layoutOf(message)
        assertTrue("the whole message wraps to more than 3 lines", expanded.lineCount > 3)
        assertFalse("nothing is clipped", expanded.hasVisualOverflow)
        composeRule.onNodeWithContentDescription("Show less").assertIsDisplayed()

        composeRule.onNodeWithContentDescription("Show less").performClick()

        assertEquals(3, layoutOf(message).lineCount)
        composeRule.onNodeWithContentDescription("Show more").assertIsDisplayed()
    }

    @Test
    fun aGroupHasOneArrowForAllItsLogs() {
        val request = "request " + "word ".repeat(200).trim()
        val response = "response " + "word ".repeat(200).trim()
        setScreen(
            logs =
                listOf(
                    VlogModel(VlogModel.INFO, "Net", request, id = "POST /login"),
                    VlogModel(VlogModel.INFO, "Net", response, id = "POST /login"),
                ),
            tags = emptyList(),
            collapseAfterLines = 3,
        )

        // Two logs are cut off, there is still just the one arrow
        composeRule.onNodeWithContentDescription("Show more").assertIsDisplayed()
        composeRule.onNodeWithContentDescription("Show more").performClick()

        // It opened both
        assertFalse(layoutOf(request).hasVisualOverflow)
        assertFalse(layoutOf(response).hasVisualOverflow)
        composeRule.onNodeWithContentDescription("Show less").performClick()
        assertEquals(3, layoutOf(request).lineCount)
        assertEquals(3, layoutOf(response).lineCount)
    }

    @Test
    fun aLogThatFitsGetsNoArrow() {
        setScreen(collapseAfterLines = 3)

        composeRule.onNodeWithContentDescription("Show more").assertDoesNotExist()
    }

    @Test
    fun longLogsAreNotCollapsedUnlessAskedTo() {
        val message = "word ".repeat(200).trim()
        setScreen(logs = listOf(VlogModel(VlogModel.INFO, "Net", message)), tags = emptyList())

        composeRule.onNodeWithContentDescription("Show more").assertDoesNotExist()
        assertFalse(layoutOf(message).hasVisualOverflow)
    }

    private fun layoutOf(text: String): TextLayoutResult {
        val layouts = mutableListOf<TextLayoutResult>()
        composeRule
            .onNodeWithText(text)
            .fetchSemanticsNode()
            .config[SemanticsActions.GetTextLayoutResult]
            .action
            ?.invoke(layouts)
        return layouts.single()
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
