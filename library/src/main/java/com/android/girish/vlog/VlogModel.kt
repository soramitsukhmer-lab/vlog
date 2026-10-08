package com.android.girish.vlog

import androidx.annotation.IntDef

internal class VlogModel(
    @param:LogPriority val logPriority: Int,
    val tag: String,
    val logMessage: String,
    // Logs with the same id are shown in one row, null for a log that stands on its own
    val id: String? = null,
    // When the log was written, in milliseconds since the epoch
    val timestamp: Long = System.currentTimeMillis(),
) {
    @Retention(AnnotationRetention.SOURCE)
    @IntDef(VERBOSE, DEBUG, INFO, WARN, ERROR)
    annotation class LogPriority

    companion object {
        /**
         * Priority constants
         */
        const val VERBOSE = 1
        const val DEBUG = 2
        const val INFO = 3
        const val WARN = 4
        const val ERROR = 5
    }
}
