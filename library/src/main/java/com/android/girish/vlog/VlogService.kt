package com.android.girish.vlog

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.graphics.Color
import android.os.Binder
import android.os.IBinder
import android.util.Log
import android.view.WindowManager
import androidx.core.app.NotificationCompat

internal class VlogService : Service() {
    companion object {
        lateinit var sInstance: VlogService
        private val TAG = VlogService::class.java.simpleName
    }

    private val mContentViewModel: ContentViewModel = ServiceLocator.provideContentViewModel()
    lateinit var windowManager: WindowManager
    lateinit var chatHeads: ChatHeads

    // Provides the lifecycle that Compose needs in the overlay window
    private val viewTreeOwner = OverlayViewTreeOwner()

    private lateinit var innerReceiver: InnerReceiver

    // Binder given to clients
    private val binder = LocalBinder()

    /**
     * Class used for the client Binder.  Because we know this service always
     * runs in the same process as its clients, we don't need to deal with IPC.
     */
    inner class LocalBinder : Binder() {
        // Return this instance of LocalService so clients can call public methods
        fun getService(): VlogService = this@VlogService
    }

    fun addChat() {
        chatHeads.add()
    }

    override fun onCreate() {
        super.onCreate()

        Log.d(TAG, "Creating Service")

        sInstance = this

        windowManager = getSystemService(Context.WINDOW_SERVICE) as WindowManager

        chatHeads = ChatHeads(this, mContentViewModel)
        viewTreeOwner.attachTo(chatHeads)
        viewTreeOwner.onCreate()

        innerReceiver = InnerReceiver()
        val intentFilter = IntentFilter(Intent.ACTION_CLOSE_SYSTEM_DIALOGS)
        // registerReceiver(innerReceiver, intentFilter)

        // If you wanna keep showing foreground notifications then uncomment the below method
        createForegroundNotification()
    }

    private fun createForegroundNotification() {
        val channelId = createNotificationChannel("Vlog", "Vlog service")

        /*val notificationIntent = Intent(this, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(
            this, 0,
            notificationIntent, 0
        )*/

        val notification =
            NotificationCompat
                .Builder(this, channelId)
                // .setOngoing(true)
                .setContentTitle("Vlog bubble is active")
                .setSmallIcon(R.drawable.ic_vlog_notification)
                .setCategory(Notification.CATEGORY_SERVICE)
                .build()
        // .setContentIntent(pendingIntent).build()

        startForeground(101, notification)
    }

    private fun createNotificationChannel(
        channelId: String,
        channelName: String,
    ): String {
        val chan =
            NotificationChannel(
                channelId,
                channelName,
                NotificationManager.IMPORTANCE_NONE,
            )
        chan.lightColor = Color.BLUE
        chan.lockscreenVisibility = Notification.VISIBILITY_PRIVATE
        val service = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        service.createNotificationChannel(chan)
        return channelId
    }

    override fun onDestroy() {
        Log.d(TAG, "Destroying Service")
        cleanUp()
        if (::chatHeads.isInitialized) {
            chatHeads.content.release()
        }
        viewTreeOwner.onDestroy()
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder = binder

    override fun onStartCommand(
        intent: Intent?,
        flags: Int,
        startId: Int,
    ): Int = START_STICKY

    fun cleanUp() {
        chatHeads.removeAll()
        // unregisterReceiver(innerReceiver)
    }
}

internal class InnerReceiver : BroadcastReceiver() {
    override fun onReceive(
        context: Context,
        intent: Intent,
    ) {
        val action = intent.action
        if (Intent.ACTION_CLOSE_SYSTEM_DIALOGS == action) {
            val reason = intent.getStringExtra("reason")
            if (reason != null) {
                VlogService.sInstance.chatHeads.collapse()
            }
        }
    }
}
