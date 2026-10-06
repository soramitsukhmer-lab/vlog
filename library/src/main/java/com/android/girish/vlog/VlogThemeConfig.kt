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

import androidx.annotation.ColorInt

/**
 * Colors of the Vlog log viewer. Pass it to [Vlog.setTheme] to match the viewer to your app.
 *
 * Every color is optional, a `null` color keeps the Vlog default. Colors are ARGB color ints,
 * for example `Color.WHITE` or `ContextCompat.getColor(context, R.color.my_color)`.
 *
 * @property surfaceColor background of the viewer
 * @property textColor title, log text and the text typed into the filter field
 * @property hintColor hint of the filter field
 * @property outlineColor border of the filter field
 * @property buttonColor background of the Clear and priority buttons
 * @property buttonTextColor text and icon of the Clear and priority buttons
 * @property warnColor text of warning logs
 * @property errorColor text of error logs
 */
data class VlogThemeConfig
    @JvmOverloads
    constructor(
        @param:ColorInt val surfaceColor: Int? = null,
        @param:ColorInt val textColor: Int? = null,
        @param:ColorInt val hintColor: Int? = null,
        @param:ColorInt val outlineColor: Int? = null,
        @param:ColorInt val buttonColor: Int? = null,
        @param:ColorInt val buttonTextColor: Int? = null,
        @param:ColorInt val warnColor: Int? = null,
        @param:ColorInt val errorColor: Int? = null,
    )
