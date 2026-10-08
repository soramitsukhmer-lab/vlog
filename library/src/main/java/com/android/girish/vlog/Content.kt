package com.android.girish.vlog

import android.content.Context
import android.view.animation.AlphaAnimation
import android.view.animation.Animation
import android.widget.LinearLayout
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Observer
import com.android.girish.vlog.utils.getAppTitle
import com.facebook.rebound.SimpleSpringListener
import com.facebook.rebound.Spring
import com.facebook.rebound.SpringSystem

internal class Content(
    context: Context,
    val mContentViewModel: ContentViewModel,
) : LinearLayout(context) {
    private val springSystem = SpringSystem.create()
    private val scaleSpring = springSystem.createSpring()

    private val logs = mutableStateOf<List<VlogModel>>(emptyList())
    private val logObserver = Observer<List<VlogModel>> { logs.value = it }

    private val tags = mutableStateOf<List<String>>(emptyList())
    private val tagsObserver = Observer<List<String>> { tags.value = it }

    init {
        addView(
            ComposeView(context).apply {
                setContent {
                    VlogTheme {
                        LogContentScreen(
                            title = remember(VlogOptions.showAppName) { context.getAppTitle(VlogOptions.showAppName) },
                            logs = logs.value,
                            tags = tags.value,
                            onKeywordChange = mContentViewModel::onKeywordEnter,
                            onPriorityIndexSelected = mContentViewModel::onPriorityIndexSelected,
                            onTagsSelected = mContentViewModel::onTagsSelected,
                            onClearLogs = mContentViewModel::onClearLogs,
                            onExportLogs = { LogExporter.exportRecent(context, it) },
                            collapseAfterLines = VlogOptions.collapseAfterLines,
                            // Leaves room for the chat head above the content
                            modifier = Modifier.padding(top = 80.dp),
                        )
                    }
                }
            },
            LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT),
        )

        scaleSpring.addListener(
            object : SimpleSpringListener() {
                override fun onSpringUpdate(spring: Spring) {
                    scaleX = spring.currentValue.toFloat()
                    scaleY = spring.currentValue.toFloat()
                }
            },
        )
        scaleSpring.springConfig = SpringConfigs.CONTENT_SCALE

        scaleSpring.currentValue = 0.0

        mContentViewModel.resultObserver.observeForever(logObserver)
        mContentViewModel.tagsObserver.observeForever(tagsObserver)
    }

    /**
     * The view model outlives the service, so the observer must be removed explicitly
     * to avoid leaking this view (and the service context) across restarts.
     */
    fun release() {
        mContentViewModel.resultObserver.removeObserver(logObserver)
        mContentViewModel.tagsObserver.removeObserver(tagsObserver)
    }

    fun hideContent() {
        VlogService.sInstance.chatHeads.showContentRunnable?.let {
            VlogService.sInstance.chatHeads.handler.removeCallbacks(
                it,
            )
        }

        scaleSpring.endValue = 0.0

        val anim = AlphaAnimation(1.0f, 0.0f)
        anim.duration = 200
        anim.repeatMode = Animation.RELATIVE_TO_SELF
        startAnimation(anim)
    }

    fun showContent() {
        scaleSpring.endValue = 1.0

        val anim = AlphaAnimation(0.0f, 1.0f)
        anim.duration = 100
        anim.repeatMode = Animation.RELATIVE_TO_SELF
        startAnimation(anim)
    }
}
