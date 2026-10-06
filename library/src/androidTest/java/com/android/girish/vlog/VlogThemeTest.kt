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

import android.graphics.Color
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import androidx.compose.ui.graphics.Color as ComposeColor

@RunWith(AndroidJUnit4::class)
class VlogThemeTest {
    @get:Rule
    val composeRule = createComposeRule()

    private lateinit var colors: VlogColors

    @After
    fun resetTheme() {
        VlogThemeState.config = VlogThemeConfig()
    }

    @Test
    fun defaultsComeFromTheLibraryResources() {
        composeRule.setContent { VlogTheme { colors = LocalVlogColors.current } }
        composeRule.waitForIdle()

        val context = InstrumentationRegistry.getInstrumentation().targetContext
        assertEquals(ComposeColor(context.getColor(R.color.log_error)), colors.error)
        assertEquals(ComposeColor(context.getColor(R.color.button_bg)), colors.button)
    }

    @Test
    fun configuredColorsOverrideTheDefaults() {
        composeRule.setContent {
            VlogTheme(VlogThemeConfig(errorColor = Color.MAGENTA, textColor = Color.WHITE)) {
                colors = LocalVlogColors.current
            }
        }
        composeRule.waitForIdle()

        assertEquals(ComposeColor(Color.MAGENTA), colors.error)
        // The text color also drives the filter input and the button text
        assertEquals(ComposeColor(Color.WHITE), colors.inputText)
        assertEquals(ComposeColor(Color.WHITE), colors.buttonText)
    }

    @Test
    fun changingTheThemeThroughVlogUpdatesAShowingViewer() {
        var shownError by mutableStateOf(ComposeColor.Unspecified)
        composeRule.setContent { VlogTheme { shownError = LocalVlogColors.current.error } }
        composeRule.waitForIdle()

        VlogThemeState.config = VlogThemeConfig(errorColor = Color.CYAN)
        composeRule.waitForIdle()

        assertEquals(ComposeColor(Color.CYAN), shownError)
    }
}
