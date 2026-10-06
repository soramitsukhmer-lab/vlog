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

import androidx.annotation.ColorRes
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.colorResource

/**
 * The theme chosen by the host app through [Vlog.setTheme]. It is snapshot state, so a viewer that is
 * already showing picks up a new theme right away.
 */
internal object VlogThemeState {
    var config by mutableStateOf(VlogThemeConfig())
}

/**
 * Colors of the log viewer that have no slot in the Material color scheme.
 */
@Immutable
internal class VlogColors(
    val inputText: Color,
    val hint: Color,
    val button: Color,
    val buttonText: Color,
    val warn: Color,
    val error: Color,
)

internal val LocalVlogColors =
    staticCompositionLocalOf<VlogColors> {
        error("VlogColors not provided, wrap the content in VlogTheme")
    }

/**
 * Compose theme for the log viewer. Colors come from [config], and fall back to the library's color
 * resources for everything the host app did not set.
 */
@Composable
internal fun VlogTheme(
    config: VlogThemeConfig = VlogThemeState.config,
    content: @Composable () -> Unit,
) {
    val text = config.textColor.orDefault(Color.Black)
    val surface = config.surfaceColor.orDefault(R.color.white_bg)
    val vlogColors =
        VlogColors(
            inputText = config.textColor.orDefault(R.color.editTextColor),
            hint = config.hintColor.orDefault(R.color.editTextColorHint),
            button = config.buttonColor.orDefault(R.color.button_bg),
            buttonText = config.buttonTextColor.orDefault(text),
            warn = config.warnColor.orDefault(R.color.log_warn),
            error = config.errorColor.orDefault(R.color.log_error),
        )
    val colorScheme =
        lightColorScheme(
            primary = colorResource(R.color.colorPrimary),
            surface = surface,
            // Used by the dropdown menu
            surfaceContainer = surface,
            onSurface = text,
            outline = config.outlineColor.orDefault(R.color.background),
        )

    CompositionLocalProvider(LocalVlogColors provides vlogColors) {
        MaterialTheme(colorScheme = colorScheme, content = content)
    }
}

private fun Int?.orDefault(default: Color): Color = if (this != null) Color(this) else default

@Composable
private fun Int?.orDefault(
    @ColorRes default: Int,
): Color = if (this != null) Color(this) else colorResource(default)
