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

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.Intent.FLAG_ACTIVITY_NEW_TASK
import android.content.ServiceConnection
import android.net.Uri
import android.os.IBinder
import android.provider.Settings
import android.util.Log
import com.android.girish.vlog.VlogService.LocalBinder
import java.util.concurrent.atomic.AtomicBoolean

class Vlog private constructor(
    val mApplicationContext: Context,
) {
    private val isEnabled = AtomicBoolean(false)
    private var mServiceIntent: Intent? = null
    private var mService: VlogService? = null
    private val mVlogRepository = ServiceLocator.provideVlogRepository()
    private val mBound = AtomicBoolean(false)
    private val mBubbleController by lazy { BubbleController(mApplicationContext) }
    private var activeMode: Mode? = null
    private val mServerConn: ServiceConnection =
        object : ServiceConnection {
            override fun onServiceConnected(
                name: ComponentName,
                binder: IBinder,
            ) {
                val service = binder as LocalBinder
                mService = service.getService()
                mBound.set(true)
                Log.d(TAG, "Service connected")
                if (isEnabled.get() && mBound.get()) {
                    Log.d(TAG, "Displaying Vlog Bubble")
                    mService!!.addChat()
                }
            }

            override fun onServiceDisconnected(name: ComponentName) {
                Log.d(TAG, "Service disconnected")
                mService = null
                mBound.set(false)
            }
        }

    private fun startService() {
        mServiceIntent = Intent(mApplicationContext, VlogService::class.java)
        // TODO: is there a need to pass token as an extra?
        mServiceIntent?.let {
            mApplicationContext.bindService(it, mServerConn, Context.BIND_AUTO_CREATE)
            mApplicationContext.startService(it)
        }
    }

    fun isEnabled(): Boolean = isEnabled.get()

    /**
     * Sets the colors of the log viewer, for example to match your app. Takes effect right away, also
     * when the viewer is already showing. Colors left `null` in [config] keep the Vlog default.
     */
    fun setTheme(config: VlogThemeConfig) {
        VlogThemeState.config = config
    }

    /**
     * Chooses whether the title of the log viewer starts with the name of your app, `My App v1.2.0 (42)`, or
     * only shows the version, `v1.2.0 (42)`. Hide the name when it is too long for the title. The name is
     * shown by default, a viewer that is already showing changes right away.
     */
    fun setShowAppNameInTitle(show: Boolean) {
        VlogOptions.showAppName = show
    }

    private fun requestDrawOverPermission() {
        val intent = Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:${mApplicationContext.packageName}"))
        intent.setFlags(FLAG_ACTIVITY_NEW_TASK)
        mApplicationContext.startActivity(intent)
    }

    private fun canDrawOverOtherApp(): Boolean = Settings.canDrawOverlays(mApplicationContext)

    private fun feed(model: VlogModel) {
        if (!isEnabled.get()) {
            Log.d(TAG, "Vlog is not started, cannot log")
            return
        }
        mVlogRepository.feedLog(model)
    }

    // TODO: pass the context once, introduce an initializer or use builder pattern.

    /**
     * Starts Vlog.
     *
     * @param mode how the log viewer is shown. [Mode.BUBBLE] needs Android 11+, notifications allowed
     * and bubbles allowed for the app. If the user can allow them, Vlog opens the matching settings
     * page instead of starting, so call [start] again afterwards. Below Android 11 there is nothing to
     * allow, so [Mode.BUBBLE] falls back to [Mode.OVERLAY].
     */
    @JvmOverloads
    fun start(mode: Mode = Mode.OVERLAY) {
        val resolvedMode = resolveMode(mode) ?: return

        if (resolvedMode == Mode.OVERLAY && !canDrawOverOtherApp()) {
            requestDrawOverPermission()
            Log.d(TAG, "Please grant Vlog permission to draw over other apps")
            return
        }

        // Ignore if already started
        if (isEnabled.getAndSet(true)) {
            Log.d(TAG, "Vlog is already started")
            return
        }
        Log.d(TAG, "Initializing Vlog in $resolvedMode mode")
        activeMode = resolvedMode
        when (resolvedMode) {
            Mode.BUBBLE -> mBubbleController.show()
            Mode.OVERLAY -> startService()
        }

        // initialize other resources if any
    }

    /**
     * Returns the mode to start in, or `null` when the user has to change a setting first and the
     * settings page was opened.
     */
    private fun resolveMode(requested: Mode): Mode? {
        if (requested != Mode.BUBBLE) return requested

        return when (mBubbleController.availability()) {
            BubbleController.Availability.SUPPORTED -> Mode.BUBBLE
            BubbleController.Availability.UNSUPPORTED_VERSION -> {
                Log.d(TAG, "Notification bubbles need Android 11, falling back to overlay mode")
                Mode.OVERLAY
            }
            BubbleController.Availability.NOTIFICATIONS_DISABLED -> {
                Log.d(TAG, "Notifications are not allowed, opening the notification settings")
                mBubbleController.openNotificationSettings()
                null
            }
            BubbleController.Availability.BUBBLES_DISABLED -> {
                Log.d(TAG, "Bubbles are not allowed for the app, opening the bubble settings")
                mBubbleController.openBubbleSettings()
                null
            }
        }
    }

    /**
     * Opens the bubble settings of your app, where the user can allow bubbles. Android 11 has no bubble
     * page, there it opens the notification settings of the app.
     */
    fun openBubbleSettings() {
        mBubbleController.openBubbleSettings()
    }

    fun stop() {
        if (!isEnabled.get()) {
            Log.d(TAG, "Vlog is not started, cannot stop")
            return
        }
        Log.d(TAG, "Stopping Vlog")
        isEnabled.set(false)
        if (activeMode == Mode.BUBBLE) {
            mBubbleController.dismiss()
            mVlogRepository.reset()
        } else if (mServiceIntent != null) {
            // Null while the service is still connecting, then there is nothing to clean up yet, and the
            // service removes its windows by itself once it is destroyed below
            mService?.cleanUp()
            mVlogRepository.reset()
            mApplicationContext.unbindService(mServerConn)
            mApplicationContext.stopService(mServiceIntent)
            mServiceIntent = null
        }
    }

    fun v(
        tag: String,
        msg: String,
    ) {
        val model = VlogModel(VlogModel.VERBOSE, tag, msg)
        feed(model)
    }

    fun d(
        tag: String,
        msg: String,
    ) {
        val model = VlogModel(VlogModel.DEBUG, tag, msg)
        feed(model)
    }

    fun i(
        tag: String,
        msg: String,
    ) {
        val model = VlogModel(VlogModel.INFO, tag, msg)
        feed(model)
    }

    fun w(
        tag: String,
        msg: String,
    ) {
        val model = VlogModel(VlogModel.WARN, tag, msg)
        feed(model)
    }

    fun e(
        tag: String,
        msg: String,
    ) {
        val model = VlogModel(VlogModel.ERROR, tag, msg)
        feed(model)
    }

    /**
     * How the log viewer is shown.
     */
    enum class Mode {
        /** A draggable chat head drawn over other apps. Needs the draw over other apps permission. */
        OVERLAY,

        /** A system notification bubble (Android 11+). Needs notifications and bubbles to be allowed for the app. */
        BUBBLE,
    }

    companion object {
        private val TAG = Vlog::class.java.simpleName
        private var instance: Vlog? = null

        @JvmStatic
        fun getInstance(context: Context): Vlog {
            synchronized(this) {
                if (instance == null) {
                    instance = Vlog(context)
                }

                return instance!!
            }
        }
    }
}
