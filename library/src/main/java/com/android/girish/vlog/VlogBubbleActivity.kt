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

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.android.girish.vlog.utils.getAppTitle

/**
 * Expanded content of the Vlog notification bubble. Shows the same log viewer as the overlay mode.
 */
internal class VlogBubbleActivity : ComponentActivity() {
    private val viewModel = ServiceLocator.provideContentViewModel()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            VlogTheme {
                val logs by viewModel.resultObserver.observeAsState(emptyList())
                val tags by viewModel.tagsObserver.observeAsState(emptyList())
                LogContentScreen(
                    title = remember(VlogOptions.showAppName) { getAppTitle(VlogOptions.showAppName) },
                    logs = logs,
                    tags = tags,
                    onKeywordChange = viewModel::onKeywordEnter,
                    onPriorityIndexSelected = viewModel::onPriorityIndexSelected,
                    onTagsSelected = viewModel::onTagsSelected,
                    onClearLogs = viewModel::onClearLogs,
                    onExportLogs = { LogExporter.exportRecent(this@VlogBubbleActivity, it) },
                    collapseAfterLines = VlogOptions.collapseAfterLines,
                    // The bubble is drawn edge to edge, keep the viewer off the system bars and the edges
                    modifier = Modifier.safeDrawingPadding().padding(8.dp),
                )
            }
        }
    }
}
