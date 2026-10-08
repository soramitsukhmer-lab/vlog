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

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import androidx.core.content.ContextCompat
import androidx.core.graphics.createBitmap
import androidx.core.graphics.drawable.IconCompat
import kotlin.math.roundToInt

/**
 * Creates the icon of the notification bubble.
 *
 * A bubble takes its icon from the shortcut it is based on, so this is the shortcut's icon. A plain
 * white glyph on a transparent background would be invisible on the bubble, so the glyph is drawn on a
 * colored square, as an adaptive bitmap.
 */
internal object BubbleIcon {
    // An adaptive icon is 108dp wide and only the center 66dp is guaranteed to be visible
    private const val ICON_SIZE_DP = 108
    private const val SAFE_ZONE_INSET_DP = 28

    fun createAdaptiveBitmap(context: Context): IconCompat = IconCompat.createWithAdaptiveBitmap(draw(context))

    private fun draw(context: Context): Bitmap {
        val density = context.resources.displayMetrics.density
        val size = (ICON_SIZE_DP * density).roundToInt()
        val inset = (SAFE_ZONE_INSET_DP * density).roundToInt()

        val bitmap = createBitmap(size, size)
        val canvas = Canvas(bitmap)
        canvas.drawColor(ContextCompat.getColor(context, R.color.colorPrimary))
        requireNotNull(ContextCompat.getDrawable(context, R.drawable.ic_adb_notification)).apply {
            setBounds(inset, inset, size - inset, size - inset)
            draw(canvas)
        }
        return bitmap
    }
}
