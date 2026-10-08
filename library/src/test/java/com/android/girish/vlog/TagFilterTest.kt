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

import com.android.girish.vlog.filter.TagFilter
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class TagFilterTest {
    private val network = VlogModel(VlogModel.INFO, "Network", "request")
    private val ui = VlogModel(VlogModel.DEBUG, "Ui", "rendered")
    private val db = VlogModel(VlogModel.ERROR, "Db", "failed")
    private val logs = listOf(network, ui, db, network)

    @Test
    fun nothingSelectedKeepsEveryLog() {
        assertEquals(logs, TagFilter().meetCriteria(logs))
    }

    @Test
    fun oneTagKeepsOnlyItsLogs() {
        val filter = TagFilter().apply { setTags(setOf("Network")) }

        assertEquals(listOf(network, network), filter.meetCriteria(logs))
    }

    @Test
    fun severalTagsKeepTheLogsOfAnyOfThem() {
        val filter = TagFilter().apply { setTags(setOf("Ui", "Db")) }

        assertEquals(listOf(ui, db), filter.meetCriteria(logs))
    }

    @Test
    fun aTagWithoutLogsKeepsNothing() {
        val filter = TagFilter().apply { setTags(setOf("Missing")) }

        assertTrue(filter.meetCriteria(logs).isEmpty())
    }

    @Test
    fun resetSelectsEveryTagAgain() {
        val filter = TagFilter().apply { setTags(setOf("Ui")) }

        filter.reset()

        assertTrue(filter.selectedTags.isEmpty())
        assertEquals(logs, filter.meetCriteria(logs))
    }
}
