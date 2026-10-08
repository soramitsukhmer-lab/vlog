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

package com.android.girish.vlog.filter

import com.android.girish.vlog.VlogModel

/**
 * Keeps the logs of the selected tags. Nothing selected means every tag.
 */
internal class TagFilter : Criteria<VlogModel> {
    // Written on the main thread and read by the filter thread, the set itself is never modified
    @Volatile
    var selectedTags: Set<String> = emptySet()
        private set

    fun setTags(tags: Set<String>) {
        selectedTags = tags.toSet()
    }

    override fun meetCriteria(input: List<VlogModel>): List<VlogModel> {
        val tags = selectedTags
        if (tags.isEmpty()) {
            return input
        }

        return input.filter { it.tag in tags }
    }

    override fun reset() {
        selectedTags = emptySet()
    }
}
