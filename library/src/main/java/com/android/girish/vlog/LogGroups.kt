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

/**
 * A row of the log viewer: one log that stands on its own, or the logs that share an id.
 *
 * @property id the shared id, null for a log without one
 * @property logs the logs of the row, oldest first, never empty
 */
internal class LogRow(
    val id: String?,
    val logs: List<VlogModel>,
)

/**
 * Turns the logs into the rows of the viewer. Logs with the same id end up in one row, at the position of
 * the oldest of them. Logs without an id each get a row of their own.
 */
internal fun groupLogs(logs: List<VlogModel>): List<LogRow> {
    val rows = mutableListOf<LogRow>()
    val groups = HashMap<String, MutableList<VlogModel>>()

    for (log in logs) {
        val id = log.id
        if (id == null) {
            rows.add(LogRow(null, listOf(log)))
            continue
        }

        val group = groups[id]
        if (group == null) {
            val newGroup = mutableListOf(log)
            groups[id] = newGroup
            rows.add(LogRow(id, newGroup))
        } else {
            group.add(log)
        }
    }

    return rows
}

/**
 * Adds the logs that share an id with a filtered log, so a row stays whole when only some of its logs
 * match the filters: a keyword that is only in the request still shows the response.
 *
 * @param all every log, oldest first
 * @param filtered the logs that passed the filters
 * @return the filtered logs and the logs that share an id with one of them, in the order of [all]
 */
internal fun expandGroups(
    all: List<VlogModel>,
    filtered: List<VlogModel>,
): List<VlogModel> {
    val ids = filtered.mapNotNullTo(HashSet()) { it.id }
    if (ids.isEmpty()) {
        return filtered
    }

    // Logs have no equality, so this is an identity match
    val kept = filtered.toHashSet()
    return all.filter { it in kept || it.id in ids }
}
