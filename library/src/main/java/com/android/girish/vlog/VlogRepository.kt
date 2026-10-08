package com.android.girish.vlog

import android.os.Handler
import android.os.Looper
import android.widget.Filter
import androidx.annotation.UiThread
import androidx.annotation.WorkerThread
import com.android.girish.vlog.VlogModel.LogPriority
import com.android.girish.vlog.filter.Criteria
import com.android.girish.vlog.filter.KeywordFilter
import com.android.girish.vlog.filter.PriorityFilter
import com.android.girish.vlog.filter.TagFilter

/**
 * Filter manager
 *
 * @property mFilterDelay The amount of delay (in ms) before the filter process starts
 * @constructor Create empty Filter manager
 */
internal class VlogRepository(
    private val mFilterDelay: Long = 100,
) : Filter() {
    private val handler: Handler = Handler(Looper.getMainLooper())
    private val mKeywordFilter = KeywordFilter()
    private val mPriorityFilter = PriorityFilter()
    private val mTagFilter = TagFilter()
    private val mFilters: List<Criteria<VlogModel>>
    private val mVlogs: MutableList<VlogModel>
    private var mResultListener: ResultListener? = null

    init {
        mFilters = ArrayList()
        mFilters.add(mKeywordFilter)
        mFilters.add(mPriorityFilter)
        mFilters.add(mTagFilter)
        mVlogs = mutableListOf()
    }

    /**
     * Initiate filter process
     *
     */
    private fun initiateFilter() {
        // remove all callbacks and messages
        handler.removeCallbacksAndMessages(null)
        handler.postDelayed(
            object : Runnable {
                override fun run() {
                    this@VlogRepository.filter(null)
                }
            },
            mFilterDelay,
        )
    }

    /**
     * For listening the filtered results
     *
     * @param resultListener
     */
    fun setResultListener(resultListener: ResultListener) {
        mResultListener = resultListener
        initiateFilter()
    }

    /**
     * pre-configures the keyword
     *
     * @param keyword
     */
    fun configureKeywordFilter(keyword: String) {
        mKeywordFilter.setKeyword(keyword)
        initiateFilter()
    }

    /**
     * pre-configures the log priority
     *
     * @param priority
     */
    fun configureLogPriority(
        @LogPriority priority: Int,
    ) {
        mPriorityFilter.setPriority(priority)
        initiateFilter()
    }

    /**
     * pre-configures the tags to show, none means every tag
     *
     * @param tags
     */
    fun configureTags(tags: Set<String>) {
        mTagFilter.setTags(tags)
        initiateFilter()
    }

    /**
     * Result listener
     *
     * @constructor Create empty Result listener
     */
    interface ResultListener {
        /**
         * On filter results
         *
         * @param filterResults
         */
        fun onFilterResults(filterResults: List<VlogModel>)

        /**
         * On tags available, the tags to offer as filter, the tags of every log and the selected tags
         *
         * @param tags
         */
        fun onTagsAvailable(tags: List<String>)
    }

    /**
     * The outcome of a filter run: the logs to show and the tags to offer
     */
    private class FilterOutput(
        val logs: List<VlogModel>,
        val tags: List<String>,
    )

    @WorkerThread
    override fun performFiltering(constraint: CharSequence?): FilterResults {
        val allLogs: List<VlogModel> = mVlogs
        var filteredList: List<VlogModel> = allLogs
        for (filter in mFilters) {
            filteredList = filter.meetCriteria(filteredList)
        }

        // Offered tags come from every log, not just the shown ones, or the chips would vanish once one is
        // picked. Selected tags stay even when their logs were cleared so they can still be deselected.
        val tags = (allLogs.map { it.tag } + mTagFilter.selectedTags).distinct()

        val filterResult = FilterResults()
        filterResult.values = FilterOutput(filteredList, tags)
        return filterResult
    }

    @UiThread
    override fun publishResults(
        constraint: CharSequence?,
        results: FilterResults?,
    ) {
        val output = results?.values as? FilterOutput ?: return
        mResultListener?.onFilterResults(output.logs)
        mResultListener?.onTagsAvailable(output.tags)
    }

    /**
     * Add log to the existing log repository
     *
     * @param model
     */
    fun feedLog(model: VlogModel) {
        mVlogs.add(model)
        initiateFilter()
    }

    /**
     * Clear logs
     *
     */
    fun clearLogs() {
        mVlogs.clear()
        initiateFilter()
    }

    fun reset() {
        mVlogs.clear()
        for (filter in mFilters) {
            filter.reset()
        }
        initiateFilter()
    }
}
