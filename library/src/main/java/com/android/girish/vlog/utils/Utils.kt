package com.android.girish.vlog.utils

import android.content.res.Resources
import android.os.Handler
import android.os.Looper
import android.util.DisplayMetrics
import android.util.TypedValue
import android.view.WindowManager

fun getOverlayFlag(): Int = WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY

fun getScreenSize(): DisplayMetrics = Resources.getSystem().displayMetrics

fun dpToPx(dp: Float): Int = (dp * Resources.getSystem().displayMetrics.density).toInt()

fun spToPx(sp: Float): Float = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_SP, sp, Resources.getSystem().displayMetrics)

fun runOnMainLoop(fn: () -> Unit) {
    Handler(Looper.getMainLooper()).post {
        fn()
    }
}

fun getAvatarUrl(
    url: String?,
    fallbackUrl: String,
): String =
    if (url != null) {
        url
    } else {
        fallbackUrl
    }

fun debug(txt: String) {
    println("asdf: $txt")
}
