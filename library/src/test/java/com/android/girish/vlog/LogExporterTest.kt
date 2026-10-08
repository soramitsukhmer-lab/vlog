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

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class LogExporterTest {
    private val now = 1_000_000L

    private fun log(
        priority: Int,
        tag: String,
        message: String,
        ageMs: Long,
    ) = VlogModel(priority, tag, message, timestamp = now - ageMs)

    @Test
    fun recentLogsKeepsOnlyTheLastTenSeconds() {
        val old = log(VlogModel.INFO, "Old", "too old", ageMs = 10_001)
        val edge = log(VlogModel.INFO, "Edge", "exactly ten seconds", ageMs = 10_000)
        val fresh = log(VlogModel.INFO, "Fresh", "just now", ageMs = 0)

        val recent = LogExporter.recentLogs(listOf(old, edge, fresh), now)

        assertEquals(listOf(edge, fresh), recent)
    }

    @Test
    fun recentLogsOfNothingIsEmpty() {
        assertTrue(LogExporter.recentLogs(emptyList(), now).isEmpty())
    }

    @Test
    fun folderNameIsTheAppName() {
        assertEquals("My App", LogExporter.folderName("My App", "com.example.app"))
    }

    @Test
    fun folderNameReplacesCharactersAFolderCannotHave() {
        assertEquals("A_B_C_D", LogExporter.folderName("A/B:C*D", "com.example.app"))
    }

    @Test
    fun folderNameFallsBackToThePackageNameWhenTheNameIsBlank() {
        assertEquals("com.example.app", LogExporter.folderName("   ", "com.example.app"))
    }

    @Test
    fun formatPutsTheIdOfAGroupedLogAfterTheTag() {
        val logs =
            listOf(
                VlogModel(VlogModel.INFO, "Net", "--> GET /a", id = "GET /a #1", timestamp = now),
                VlogModel(VlogModel.INFO, "Ui", "no id", timestamp = now),
            )

        val lines = LogExporter.format("My App", now, logs).lines().filter { it.isNotEmpty() }

        assertTrue(lines[1].endsWith("I/Net [GET /a #1]: --> GET /a"))
        assertTrue(lines[2].endsWith("I/Ui: no id"))
    }

    @Test
    fun formatWritesAHeaderAndOneLinePerLogInOrder() {
        val logs =
            listOf(
                log(VlogModel.ERROR, "Net", "request failed", ageMs = 2_000),
                log(VlogModel.DEBUG, "Ui", "rendered", ageMs = 1_000),
            )

        val lines = LogExporter.format("My App v1.0 (1)", now, logs).lines().filter { it.isNotEmpty() }

        assertEquals(3, lines.size)
        assertTrue(lines[0].startsWith("My App v1.0 (1), logs of the last 10 seconds"))
        assertTrue(lines[1].endsWith("E/Net: request failed"))
        assertTrue(lines[2].endsWith("D/Ui: rendered"))
    }
}
