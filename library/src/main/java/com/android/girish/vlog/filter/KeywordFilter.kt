package com.android.girish.vlog.filter
import com.android.girish.vlog.VlogModel

internal class KeywordFilter : Criteria<VlogModel> {
    var mKeyword: String = ""

    fun setKeyword(keyword: String) {
        mKeyword = keyword
    }

    override fun meetCriteria(input: List<VlogModel>): List<VlogModel> {
        if (mKeyword.isEmpty()) {
            return input
        }

        val normalizedKeyword = mKeyword.lowercase().trim()
        val filteredLogs = ArrayList<VlogModel>()

        for (item in input) {
            val normalizedLog = item.logMessage.lowercase().trim()
            val normalizedTag = item.tag.lowercase().trim()

            if (normalizedLog.contains(normalizedKeyword) || normalizedTag.contains(normalizedKeyword)) {
                filteredLogs.add(item)
            }
        }

        return filteredLogs
    }

    override fun reset() {
        mKeyword = ""
    }
}
