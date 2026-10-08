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

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.listSaver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringArrayResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

private const val COLLAPSED_MESSAGE_LENGTH = 50
private const val EXPANDED_MESSAGE_MAX_LINES = 20

// The bar on the left of the rows that group logs
private val GROUP_BAR_WIDTH = 4.dp

// A set of strings is not something a bundle can hold, a list is
private val TagSelectionSaver = listSaver<Set<String>, String>(save = { it.toList() }, restore = { it.toSet() })

/**
 * The log viewer: a top app bar (app title, an export action and a clear action), a filter bar (priority
 * and keyword) and the list of logs.
 *
 * Replaces the `log_content_view` XML layout. Filtering is delegated to the caller through the
 * callbacks, [logs] is expected to be the already filtered list.
 *
 * @param title the title shown in the header, usually the name and version of the app using Vlog
 * @param logs the logs to show
 * @param tags the tags to offer as filter chips, none hides the chips
 * @param onKeywordChange called when the user edits the filter keyword
 * @param onPriorityIndexSelected called with the position in `log_priority_names` the user picked
 * @param onTagsSelected called with the selected tags when the user toggles a chip, none means every tag
 * @param onClearLogs called when the user taps Clear
 * @param onExportLogs called with the logs that are shown when the user taps Export
 */
@Composable
internal fun LogContentScreen(
    title: String,
    logs: List<VlogModel>,
    tags: List<String>,
    onKeywordChange: (String) -> Unit,
    onPriorityIndexSelected: (Int) -> Unit,
    onTagsSelected: (Set<String>) -> Unit,
    onClearLogs: () -> Unit,
    onExportLogs: (List<VlogModel>) -> Unit,
    modifier: Modifier = Modifier,
) {
    // Only one log is expanded at a time, logs have no equality so this is an identity match
    var expandedLog by remember { mutableStateOf<VlogModel?>(null) }
    var selectedTags by rememberSaveable(stateSaver = TagSelectionSaver) { mutableStateOf(emptySet<String>()) }
    val rows = remember(logs) { groupLogs(logs) }

    val focusManager = LocalFocusManager.current
    val keyboardController = LocalSoftwareKeyboardController.current
    val dismissKeyboard = {
        focusManager.clearFocus()
        keyboardController?.hide()
    }

    Column(
        modifier
            .fillMaxSize()
            // Taps that no child consumes (header, empty space) dismiss the keyboard
            .pointerInput(Unit) { detectTapGestures { dismissKeyboard() } }
            .background(
                MaterialTheme.colorScheme.surface,
                RoundedCornerShape(topStart = 6.dp, topEnd = 6.dp),
            ),
    ) {
        LogAppBar(title = title, onExportLogs = { onExportLogs(logs) }, onClearLogs = onClearLogs)
        LogFilterBar(
            onKeywordChange = onKeywordChange,
            onPriorityIndexSelected = onPriorityIndexSelected,
        )
        LogTagChips(
            tags = tags,
            selectedTags = selectedTags,
            onToggle = { tag ->
                dismissKeyboard()
                selectedTags = if (tag in selectedTags) selectedTags - tag else selectedTags + tag
                onTagsSelected(selectedTags)
            },
        )
        LazyColumn(Modifier.weight(1f)) {
            items(rows) { row ->
                // A row is expanded by its first log, which stays the same while the row grows
                val first = row.logs.first()
                val isExpanded = first == expandedLog
                val onClick = {
                    dismissKeyboard()
                    expandedLog = if (isExpanded) null else first
                }
                if (row.id == null) {
                    LogItem(log = first, isExpanded = isExpanded, onClick = onClick)
                } else {
                    LogGroupItem(row = row, isExpanded = isExpanded, onClick = onClick)
                }
                HorizontalDivider()
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun LogAppBar(
    title: String,
    onExportLogs: () -> Unit,
    onClearLogs: () -> Unit,
    modifier: Modifier = Modifier,
) {
    TopAppBar(
        title = {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        },
        actions = {
            IconButton(onClick = onExportLogs) {
                Icon(painterResource(R.drawable.ic_download), contentDescription = stringResource(R.string.export_logs))
            }
            IconButton(onClick = onClearLogs) {
                Icon(painterResource(R.drawable.ic_delete), contentDescription = stringResource(R.string.clear_logs))
            }
        },
        modifier = modifier,
        // The window handles the system bars: the bubble pads for them and the overlay sits below them
        windowInsets = WindowInsets(0),
        colors =
            TopAppBarDefaults.topAppBarColors(
                containerColor = MaterialTheme.colorScheme.surface,
                titleContentColor = MaterialTheme.colorScheme.onSurface,
                actionIconContentColor = MaterialTheme.colorScheme.onSurface,
            ),
    )
}

@Composable
private fun LogFilterBar(
    onKeywordChange: (String) -> Unit,
    onPriorityIndexSelected: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    val priorityNames = stringArrayResource(R.array.log_priority_names)
    var selectedPriorityIndex by rememberSaveable { mutableStateOf<Int?>(null) }
    var keyword by rememberSaveable { mutableStateOf("") }
    var isMenuExpanded by remember { mutableStateOf(false) }

    Row(
        modifier
            .fillMaxWidth()
            .padding(5.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box {
            LogButton(onClick = { isMenuExpanded = true }) {
                Text(selectedPriorityIndex?.let { priorityNames[it] } ?: stringResource(R.string.select_priority))
                Spacer(Modifier.width(2.dp))
                Icon(painterResource(R.drawable.ic_arrow_down), contentDescription = null)
            }
            DropdownMenu(expanded = isMenuExpanded, onDismissRequest = { isMenuExpanded = false }) {
                priorityNames.forEachIndexed { index, name ->
                    DropdownMenuItem(
                        text = { Text(name) },
                        onClick = {
                            selectedPriorityIndex = index
                            isMenuExpanded = false
                            onPriorityIndexSelected(index)
                        },
                    )
                }
            }
        }

        BasicTextField(
            value = keyword,
            onValueChange = {
                keyword = it
                onKeywordChange(it)
            },
            singleLine = true,
            textStyle = TextStyle(color = LocalVlogColors.current.inputText, fontSize = 14.sp),
            modifier =
                Modifier
                    .weight(1f)
                    .padding(horizontal = 2.dp),
            decorationBox = { innerTextField ->
                Box(
                    Modifier
                        .fillMaxWidth()
                        .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(2.dp))
                        .padding(start = 12.dp, top = 8.dp, bottom = 8.dp),
                ) {
                    if (keyword.isEmpty()) {
                        Text(
                            stringResource(R.string.filter_hint),
                            color = LocalVlogColors.current.hint,
                            fontSize = 14.sp,
                        )
                    }
                    innerTextField()
                }
            },
        )
    }
}

/**
 * A row of chips to filter the logs by tag. It scrolls sideways when the tags do not fit, and shows nothing
 * while there are no tags.
 */
@Composable
private fun LogTagChips(
    tags: List<String>,
    selectedTags: Set<String>,
    onToggle: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    if (tags.isEmpty()) return

    val vlogColors = LocalVlogColors.current
    LazyRow(
        modifier.fillMaxWidth(),
        contentPadding = PaddingValues(horizontal = 5.dp, vertical = 2.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        items(tags, key = { it }) { tag ->
            FilterChip(
                selected = tag in selectedTags,
                onClick = { onToggle(tag) },
                label = { Text(tag, maxLines = 1) },
                colors =
                    FilterChipDefaults.filterChipColors(
                        labelColor = MaterialTheme.colorScheme.onSurface,
                        selectedContainerColor = vlogColors.button,
                        selectedLabelColor = vlogColors.buttonText,
                    ),
            )
        }
    }
}

@Composable
private fun LogButton(
    onClick: () -> Unit,
    content: @Composable () -> Unit,
) {
    Button(
        onClick = onClick,
        shape = RoundedCornerShape(2.dp),
        colors =
            ButtonDefaults.buttonColors(
                containerColor = LocalVlogColors.current.button,
                contentColor = LocalVlogColors.current.buttonText,
            ),
    ) {
        content()
    }
}

@Composable
private fun LogItem(
    log: VlogModel,
    isExpanded: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 10.dp, vertical = 15.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        LogEntry(log, isExpanded, Modifier.weight(1f))
        ExpandArrow(isExpanded)
    }
}

/**
 * The logs that share an id in one row: the id on top, then every log with its own priority and tag. The
 * row is tinted and has a bar on its left, so it stands out from the logs that stand on their own.
 */
@Composable
private fun LogGroupItem(
    row: LogRow,
    isExpanded: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val onSurface = MaterialTheme.colorScheme.onSurface
    val accent = MaterialTheme.colorScheme.outline

    Row(
        modifier
            .fillMaxWidth()
            .background(onSurface.copy(alpha = 0.04f))
            .drawBehind { drawRect(accent, size = Size(GROUP_BAR_WIDTH.toPx(), size.height)) }
            .clickable(onClick = onClick)
            .padding(start = 16.dp, top = 15.dp, bottom = 15.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Column(Modifier.weight(1f)) {
            Text(
                text = row.id.orEmpty(),
                color = onSurface.copy(alpha = 0.6f),
                fontSize = 12.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            row.logs.forEach { log ->
                LogEntry(log, isExpanded, Modifier.padding(top = 8.dp))
            }
        }
        ExpandArrow(isExpanded)
    }
}

/**
 * One log: its priority and tag, then its message. The message is cut when collapsed.
 */
@Composable
private fun LogEntry(
    log: VlogModel,
    isExpanded: Boolean,
    modifier: Modifier = Modifier,
) {
    val color =
        when (log.logPriority) {
            VlogModel.ERROR -> LocalVlogColors.current.error
            VlogModel.WARN -> LocalVlogColors.current.warn
            else -> MaterialTheme.colorScheme.onSurface
        }
    val message =
        if (isExpanded || log.logMessage.length <= COLLAPSED_MESSAGE_LENGTH) {
            log.logMessage
        } else {
            log.logMessage.substring(0, COLLAPSED_MESSAGE_LENGTH - 1) + "..."
        }

    Column(modifier) {
        Text(
            text = "${log.priorityInitial()}/${log.tag}: ",
            color = color,
            fontSize = 14.sp,
            overflow = TextOverflow.Ellipsis,
        )
        Text(
            text = message,
            color = color,
            fontSize = 14.sp,
            maxLines = EXPANDED_MESSAGE_MAX_LINES,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(top = 3.dp),
        )
    }
}

@Composable
private fun ExpandArrow(isExpanded: Boolean) {
    Icon(
        painter = painterResource(if (isExpanded) R.drawable.ic_arrow_up else R.drawable.ic_arrow_down),
        contentDescription = null,
        tint = MaterialTheme.colorScheme.onSurface,
        modifier = Modifier.padding(end = 10.dp),
    )
}

internal fun VlogModel.priorityInitial(): String =
    when (logPriority) {
        VlogModel.DEBUG -> "D"
        VlogModel.ERROR -> "E"
        VlogModel.INFO -> "I"
        VlogModel.VERBOSE -> "V"
        VlogModel.WARN -> "W"
        else -> ""
    }

@Preview(showBackground = true, heightDp = 480)
@Composable
private fun LogContentScreenPreview() {
    VlogTheme {
        LogContentScreen(
            title = "Vlog Sample v1.0 (1)",
            logs =
                listOf(
                    VlogModel(VlogModel.VERBOSE, "Surface", "Test log with verbose priority"),
                    VlogModel(VlogModel.DEBUG, "DecorView", "Test log with debug priority"),
                    VlogModel(VlogModel.INFO, "Network", "--> POST /login {\"user\":\"sora\"}", id = "POST /login #1"),
                    VlogModel(VlogModel.INFO, "Surface", "Test log with info priority"),
                    VlogModel(VlogModel.ERROR, "Network", "<-- 401 /login {\"error\":\"invalid\"}", id = "POST /login #1"),
                    VlogModel(VlogModel.WARN, "DecorView", "Test log with warn priority for a message that is long enough to be truncated"),
                    VlogModel(VlogModel.ERROR, "Choreographer", "Test log with error priority"),
                ),
            tags = listOf("Surface", "DecorView", "Choreographer"),
            onKeywordChange = {},
            onPriorityIndexSelected = {},
            onTagsSelected = {},
            onClearLogs = {},
            onExportLogs = {},
        )
    }
}
