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

import androidx.compose.ui.graphics.Color
import org.junit.Assert.assertSame
import org.junit.Test

class VlogColorsTest {
    private fun level(value: Long) = LevelColors(text = Color(value), accent = Color(value))

    private val verbose = level(0xFF000001)
    private val debug = level(0xFF000002)
    private val info = level(0xFF000003)
    private val warn = level(0xFF000004)
    private val error = level(0xFF000005)
    private val colors =
        VlogColors(
            inputText = Color.Black,
            hint = Color.Gray,
            button = Color.LightGray,
            buttonText = Color.Black,
            verbose = verbose,
            debug = debug,
            info = info,
            warn = warn,
            error = error,
        )

    @Test
    fun everyPriorityGetsItsOwnLevelColors() {
        assertSame(verbose, colors.forPriority(VlogModel.VERBOSE))
        assertSame(debug, colors.forPriority(VlogModel.DEBUG))
        assertSame(info, colors.forPriority(VlogModel.INFO))
        assertSame(warn, colors.forPriority(VlogModel.WARN))
        assertSame(error, colors.forPriority(VlogModel.ERROR))
    }
}
