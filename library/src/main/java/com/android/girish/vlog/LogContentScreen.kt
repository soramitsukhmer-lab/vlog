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
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
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

// The bar on the left of the rows that group logs
private val GROUP_BAR_WIDTH = 4.dp

// The bar of a log that shows its level
private val ACCENT_BAR_WIDTH = 4.dp

// The room for the arrow that opens and closes a collapsed log, an icon with the padding of a touch target
private val ARROW_SLOT_SIZE = 40.dp

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
 * @param collapseAfterLines the number of lines after which a log is collapsed behind an arrow that opens it,
 * null shows every log in full
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
    collapseAfterLines: Int? = null,
) {
    var selectedTags by rememberSaveable(stateSaver = TagSelectionSaver) { mutableStateOf(emptySet<String>()) }
    val rows = remember(logs) { groupLogs(logs) }

    // The logs the user showed in full. Logs have no equality, so this is an identity match. It is kept here and
    // not per row, a row that scrolls out of the list and back must stay as the user left it.
    var expandedLogs by remember { mutableStateOf(emptySet<VlogModel>()) }
    // A row opens or closes as a whole: every log of a group follows the one arrow of the group
    val toggleExpanded = { rowLogs: List<VlogModel> ->
        expandedLogs = if (rowLogs.all { it in expandedLogs }) expandedLogs - rowLogs.toSet() else expandedLogs + rowLogs
    }
    // Do not hold on to logs that are gone, after Clear for example
    LaunchedEffect(logs) {
        if (expandedLogs.isNotEmpty()) expandedLogs = expandedLogs.intersect(logs.toHashSet())
    }

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
                if (row.id == null) {
                    val log = row.logs.first()
                    LogItem(
                        log = log,
                        collapseAfterLines = collapseAfterLines,
                        isExpanded = log in expandedLogs,
                        onToggleExpanded = { toggleExpanded(row.logs) },
                    )
                } else {
                    LogGroupItem(
                        row = row,
                        collapseAfterLines = collapseAfterLines,
                        expandedLogs = expandedLogs,
                        onToggleExpanded = { toggleExpanded(row.logs) },
                    )
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
    collapseAfterLines: Int?,
    isExpanded: Boolean,
    onToggleExpanded: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var overflows by remember(log) { mutableStateOf(false) }

    Row(
        modifier
            .fillMaxWidth()
            .padding(horizontal = 10.dp, vertical = 15.dp),
    ) {
        LogEntry(
            log = log,
            collapseAfterLines = collapseAfterLines,
            isExpanded = isExpanded,
            onOverflowChange = { overflows = it },
            modifier = Modifier.weight(1f),
        )
        CollapseArrow(
            enabled = collapseAfterLines != null,
            visible = isExpanded || overflows,
            isExpanded = isExpanded,
            onClick = onToggleExpanded,
        )
    }
}

/**
 * The logs that share an id in one row: the id on top, then every log with its own priority and tag. The
 * row is tinted and has a bar on its left, so it stands out from the logs that stand on their own.
 */
@Composable
private fun LogGroupItem(
    row: LogRow,
    collapseAfterLines: Int?,
    expandedLogs: Set<VlogModel>,
    onToggleExpanded: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val onSurface = MaterialTheme.colorScheme.onSurface
    val accent = MaterialTheme.colorScheme.outline
    // The logs of this row that are cut off while collapsed
    val overflowing = remember(row) { mutableStateMapOf<VlogModel, Boolean>() }
    val isExpanded = row.logs.all { it in expandedLogs }

    Row(
        modifier
            .fillMaxWidth()
            .background(onSurface.copy(alpha = 0.04f))
            .drawBehind { drawRect(accent, size = Size(GROUP_BAR_WIDTH.toPx(), size.height)) }
            .padding(start = 16.dp, end = 10.dp, top = 15.dp, bottom = 15.dp),
    ) {
        Column(Modifier.weight(1f)) {
            Text(
                text = row.id.orEmpty(),
                color = onSurface.copy(alpha = 0.6f),
                fontSize = 12.sp,
            )
            row.logs.forEach { log ->
                LogEntry(
                    log = log,
                    collapseAfterLines = collapseAfterLines,
                    isExpanded = log in expandedLogs,
                    onOverflowChange = { overflowing[log] = it },
                    modifier = Modifier.padding(top = 8.dp),
                )
            }
        }
        CollapseArrow(
            enabled = collapseAfterLines != null,
            visible = row.logs.any { it in expandedLogs } || overflowing.values.any { it },
            isExpanded = isExpanded,
            onClick = onToggleExpanded,
        )
    }
}

/**
 * The arrow at the end of a row that opens or closes its collapsed logs. Its room is kept while collapsing
 * is [enabled], so the message keeps its width when the arrow shows up. It is [visible] only for a row with
 * a log that is cut off, or that was opened.
 */
@Composable
private fun CollapseArrow(
    enabled: Boolean,
    visible: Boolean,
    isExpanded: Boolean,
    onClick: () -> Unit,
) {
    if (!enabled) return

    Box(Modifier.size(ARROW_SLOT_SIZE)) {
        if (visible) {
            Icon(
                painter = painterResource(if (isExpanded) R.drawable.ic_arrow_up else R.drawable.ic_arrow_down),
                contentDescription = stringResource(if (isExpanded) R.string.show_less else R.string.show_more),
                tint = MaterialTheme.colorScheme.onSurface,
                modifier =
                    Modifier
                        .fillMaxSize()
                        .clickable(onClick = onClick)
                        .padding(8.dp),
            )
        }
    }
}

/**
 * One log: its priority and tag, then its message. A long message or tag wraps to more lines. Only when
 * [collapseAfterLines] is set is a message that needs more lines cut off. The arrow that shows it in full
 * is not here, it is the row's, so a group has one for all its logs.
 *
 * @param collapseAfterLines the lines a collapsed message keeps, null never collapses
 * @param isExpanded whether the user showed the message in full
 */
@Composable
private fun LogEntry(
    log: VlogModel,
    collapseAfterLines: Int?,
    isExpanded: Boolean,
    onOverflowChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    val levelColors = LocalVlogColors.current.forPriority(log.logPriority)
    val color = levelColors.text
    val maxLines = if (collapseAfterLines != null && !isExpanded) collapseAfterLines else Int.MAX_VALUE

    Row(modifier.height(IntrinsicSize.Min)) {
        // The bar tells the level at a glance, also in a group where every log has its own level
        Box(
            Modifier
                .width(ACCENT_BAR_WIDTH)
                .fillMaxHeight()
                .background(levelColors.accent, RoundedCornerShape(2.dp)),
        )
        Spacer(Modifier.width(8.dp))
        Column(Modifier.weight(1f)) {
            Text(
                text = "${log.priorityInitial()}/${log.tag}: ",
                color = color,
                fontSize = 14.sp,
            )
            Text(
                text = log.logMessage,
                color = color,
                fontSize = 14.sp,
                maxLines = maxLines,
                overflow = TextOverflow.Ellipsis,
                onTextLayout = { if (!isExpanded) onOverflowChange(it.hasVisualOverflow) },
                modifier = Modifier.padding(top = 3.dp),
            )
        }
    }
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
                    VlogModel(
                        VlogModel.WARN,
                        "DecorView",
                        "Test log with warn priority for a message that is long enough to wrap onto several lines, none of it is cut off",
                    ),
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

// Long logs collapsed after 2 lines: the long ones get an arrow, the short one does not
@Preview(showBackground = true, heightDp = 420)
@Composable
private fun LogContentScreenCollapsedPreview() {
    VlogTheme {
        LogContentScreen(
            title = "Vlog Sample v1.0 (1)",
            logs =
                listOf(
                    VlogModel(VlogModel.INFO, "Surface", "A short log that fits"),
                    VlogModel(
                        VlogModel.ERROR,
                        "Network",
                        "<-- 500 /orders {\"error\":\"internal\",\"trace\":\"at OrdersService.create at Gateway.route at Server.handle\",\"requestId\":\"3f9c2\"}",
                    ),
                    VlogModel(
                        VlogModel.DEBUG,
                        "Json",
                        "[1,2,3,4,5,6,7,8,9,10,11,12,13,14,15,16,17,18,19,20,21,22,23,24,25,26,27,28,29,30]",
                    ),
                ),
            tags = emptyList(),
            onKeywordChange = {},
            onPriorityIndexSelected = {},
            onTagsSelected = {},
            onClearLogs = {},
            onExportLogs = {},
            collapseAfterLines = 2,
        )
    }
}

// A host app with a dark theme: only the surface and the text are set, the level colors follow the surface
@Preview(showBackground = true, backgroundColor = 0xFF1E1E1E, heightDp = 560)
@Composable
private fun LogContentScreenDarkPreview() {
    VlogTheme(
        VlogThemeConfig(
            surfaceColor = 0xFF1E1E1E.toInt(),
            textColor = 0xFFEEEEEE.toInt(),
            hintColor = 0x99EEEEEE.toInt(),
            outlineColor = 0xFF777777.toInt(),
            buttonColor = 0xFF3A3A3A.toInt(),
            buttonTextColor = 0xFFEEEEEE.toInt(),
        ),
    ) {
        LogContentScreen(
            title = "Vlog Sample v1.0 (1)",
            logs =
                listOf(
                    VlogModel(VlogModel.VERBOSE, "Surface", "Test log with verbose priority"),
                    VlogModel(VlogModel.DEBUG, "Surface", "Test log with debug priority"),
                    VlogModel(VlogModel.INFO, "Surface", "Test log with info priority"),
                    VlogModel(VlogModel.WARN, "Surface", "Test log with warn priority"),
                    VlogModel(VlogModel.ERROR, "Surface", "Test log with error priority"),
                ),
            tags = emptyList(),
            onKeywordChange = {},
            onPriorityIndexSelected = {},
            onTagsSelected = {},
            onClearLogs = {},
            onExportLogs = {},
        )
    }
}
