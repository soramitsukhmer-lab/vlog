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

import android.annotation.SuppressLint
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.core.app.NotificationChannelCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.app.Person
import androidx.core.content.LocusIdCompat
import androidx.core.content.pm.ShortcutInfoCompat
import androidx.core.content.pm.ShortcutManagerCompat
import androidx.core.graphics.drawable.IconCompat

/**
 * Shows the Vlog log viewer through the Android notification bubble API instead of a
 * draw-over-other-apps overlay. The expanded bubble hosts [VlogBubbleActivity].
 */
internal class BubbleController(
    private val context: Context,
) {
    private val notificationManager = NotificationManagerCompat.from(context)

    /**
     * Bubbles need Android 11+, notifications to be allowed and the user to allow bubbles for the app.
     */
    fun isSupported(): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.R) return false
        return areBubblesAllowed() && notificationManager.areNotificationsEnabled()
    }

    @RequiresApi(Build.VERSION_CODES.R)
    private fun areBubblesAllowed(): Boolean {
        val manager = context.getSystemService(NotificationManager::class.java)
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            manager.bubblePreference != NotificationManager.BUBBLE_PREFERENCE_NONE
        } else {
            @Suppress("DEPRECATION")
            manager.areBubblesAllowed()
        }
    }

    // isSupported() has already verified that notifications are allowed (including POST_NOTIFICATIONS)
    @SuppressLint("MissingPermission")
    fun show() {
        notificationManager.createNotificationChannel(
            NotificationChannelCompat
                .Builder(CHANNEL_ID, NotificationManagerCompat.IMPORTANCE_DEFAULT)
                .setName(context.getString(R.string.vlog_bubble_channel_name))
                .build(),
        )

        val person =
            Person
                .Builder()
                .setName(context.getString(R.string.vlog_bubble_title))
                .setKey(SHORTCUT_ID)
                .setBot(true)
                .setImportant(true)
                .build()
        val icon = IconCompat.createWithResource(context, R.drawable.ic_vlog_notification)

        // Bubbles must be backed by a long lived shortcut
        ShortcutManagerCompat.pushDynamicShortcut(
            context,
            ShortcutInfoCompat
                .Builder(context, SHORTCUT_ID)
                .setLongLived(true)
                .setShortLabel(context.getString(R.string.vlog_bubble_title))
                .setIcon(icon)
                .setPerson(person)
                .setLocusId(LocusIdCompat(SHORTCUT_ID))
                .setIntent(Intent(context, VlogBubbleActivity::class.java).setAction(Intent.ACTION_VIEW))
                .build(),
        )

        val bubbleIntent =
            PendingIntent.getActivity(
                context,
                REQUEST_CODE,
                Intent(context, VlogBubbleActivity::class.java),
                PendingIntent.FLAG_UPDATE_CURRENT or mutableFlag(),
            )
        val dismissIntent =
            PendingIntent.getBroadcast(
                context,
                REQUEST_CODE,
                Intent(context, VlogBubbleDismissReceiver::class.java),
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
            )
        val bubbleMetadata =
            NotificationCompat.BubbleMetadata
                .Builder(bubbleIntent, icon)
                .setDesiredHeight(DESIRED_HEIGHT_DP)
                .setDeleteIntent(dismissIntent)
                .setSuppressNotification(true)
                .build()

        val notification =
            NotificationCompat
                .Builder(context, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_vlog_notification)
                .setContentTitle(context.getString(R.string.vlog_bubble_title))
                .setContentText(context.getString(R.string.vlog_bubble_message))
                .setCategory(NotificationCompat.CATEGORY_MESSAGE)
                .setShortcutId(SHORTCUT_ID)
                .setLocusId(LocusIdCompat(SHORTCUT_ID))
                .addPerson(person)
                .setStyle(
                    NotificationCompat
                        .MessagingStyle(person)
                        .addMessage(context.getString(R.string.vlog_bubble_message), System.currentTimeMillis(), person),
                ).setBubbleMetadata(bubbleMetadata)
                .setSilent(true)
                .build()

        notificationManager.notify(NOTIFICATION_ID, notification)
    }

    fun dismiss() {
        notificationManager.cancel(NOTIFICATION_ID)
        ShortcutManagerCompat.removeDynamicShortcuts(context, listOf(SHORTCUT_ID))
    }

    // Bubble intents must be mutable on Android 12+ so the system can fill in the launch bounds
    private fun mutableFlag(): Int = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) PendingIntent.FLAG_MUTABLE else 0

    private companion object {
        const val CHANNEL_ID = "vlog_bubble"
        const val SHORTCUT_ID = "vlog_bubble_shortcut"
        const val NOTIFICATION_ID = 102
        const val REQUEST_CODE = 102
        const val DESIRED_HEIGHT_DP = 600
    }
}
