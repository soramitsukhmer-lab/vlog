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

import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.updateLayoutParams
import androidx.core.widget.doAfterTextChanged
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView

/**
 * Expanded content of the Vlog notification bubble. Shows the same log viewer as the overlay mode.
 */
internal class VlogBubbleActivity : AppCompatActivity() {
    private val viewModel = ServiceLocator.provideContentViewModel()
    private val vlogAdapter = VlogAdapter()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.log_content_view)

        // The top margin leaves room for the chat head in overlay mode, the bubble does not need it
        findViewById<View>(R.id.content_container).updateLayoutParams<ViewGroup.MarginLayoutParams> { topMargin = 0 }

        val messagesView: RecyclerView = findViewById(R.id.events)
        messagesView.layoutManager = LinearLayoutManager(this)
        messagesView.addItemDecoration(DividerItemDecoration(this, DividerItemDecoration.VERTICAL))
        messagesView.adapter = vlogAdapter

        val logPriorityTxtVw: TextView = findViewById(R.id.log_priority_txtvw)
        logPriorityTxtVw.setOnClickListener { showPriorityOptions(logPriorityTxtVw) }

        findViewById<EditText>(R.id.editText).doAfterTextChanged { viewModel.onKeywordEnter(it.toString()) }
        findViewById<View>(R.id.clear_logs).setOnClickListener { viewModel.onClearLogs() }

        viewModel.resultObserver.observe(this) { vlogAdapter.addLogs(it) }
    }

    private fun showPriorityOptions(logPriorityTxtVw: TextView) {
        val priorityNames = resources.getStringArray(R.array.log_priority_names)
        AlertDialog
            .Builder(this)
            .setTitle("Select Log priority")
            .setItems(priorityNames) { _, selectedIndex ->
                logPriorityTxtVw.text = priorityNames[selectedIndex]
                viewModel.onPriorityIndexSelected(selectedIndex)
            }.setPositiveButton("Cancel") { dialog, _ -> dialog.dismiss() }
            .show()
    }
}
