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

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Test

class LogGroupsTest {
    private val plain1 = VlogModel(VlogModel.INFO, "Ui", "plain 1")
    private val request = VlogModel(VlogModel.INFO, "Net", "--> GET /a", id = "a")
    private val plain2 = VlogModel(VlogModel.DEBUG, "Ui", "plain 2")
    private val otherRequest = VlogModel(VlogModel.INFO, "Net", "--> GET /b", id = "b")
    private val response = VlogModel(VlogModel.INFO, "Net", "<-- 200 /a", id = "a")

    @Test
    fun logsWithoutAnIdEachGetARow() {
        val rows = groupLogs(listOf(plain1, plain2))

        assertEquals(2, rows.size)
        assertNull(rows[0].id)
        assertEquals(listOf(plain1), rows[0].logs)
        assertEquals(listOf(plain2), rows[1].logs)
    }

    @Test
    fun logsWithTheSameIdShareTheRowOfTheOldestOne() {
        val rows = groupLogs(listOf(plain1, request, plain2, otherRequest, response))

        assertEquals(4, rows.size)
        assertEquals(listOf(plain1), rows[0].logs)
        assertEquals("a", rows[1].id)
        assertEquals(listOf(request, response), rows[1].logs)
        assertEquals(listOf(plain2), rows[2].logs)
        assertEquals("b", rows[3].id)
        assertEquals(listOf(otherRequest), rows[3].logs)
    }

    @Test
    fun aRowStaysKeyedByItsFirstLogWhileItGrows() {
        val before = groupLogs(listOf(request))
        val after = groupLogs(listOf(request, response))

        assertSame(before[0].logs.first(), after[0].logs.first())
    }

    @Test
    fun noLogsMakeNoRows() {
        assertEquals(0, groupLogs(emptyList()).size)
    }

    @Test
    fun expandGroupsAddsTheOtherLogsOfAMatchingGroup() {
        val all = listOf(plain1, request, plain2, otherRequest, response)

        // Only the request passed the filters, the response comes with it, in the original order
        assertEquals(listOf(request, response), expandGroups(all, listOf(request)))
    }

    @Test
    fun expandGroupsKeepsFilteredLogsWithoutAnIdAsTheyAre() {
        val all = listOf(plain1, request, plain2, response)

        assertEquals(listOf(plain1, plain2), expandGroups(all, listOf(plain1, plain2)))
    }

    @Test
    fun expandGroupsMixesGroupsAndPlainLogsInTheOriginalOrder() {
        val all = listOf(plain1, request, plain2, otherRequest, response)

        assertEquals(listOf(request, plain2, response), expandGroups(all, listOf(response, plain2)))
    }

    @Test
    fun expandGroupsOfNothingStaysEmpty() {
        assertEquals(emptyList<VlogModel>(), expandGroups(listOf(plain1, request, response), emptyList()))
    }
}
