package com.pizzatown.admin.core.notifications

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import androidx.core.content.getSystemService
import com.google.firebase.messaging.FirebaseMessaging
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Wires up the admin side of push notifications: creates the "new
 * orders" channel and subscribes this device to the admin topic so it
 * hears about every order the moment a customer places one — see
 * onOrderCreated in /functions/index.js.
 */
@Singleton
class AdminNotificationRegistrar @Inject constructor(
    @ApplicationContext private val context: Context
) {
    fun ensureChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                AdminNotificationConstants.CHANNEL_ID,
                AdminNotificationConstants.CHANNEL_NAME,
                NotificationManager.IMPORTANCE_HIGH
            ).apply { description = AdminNotificationConstants.CHANNEL_DESCRIPTION }
            context.getSystemService<NotificationManager>()?.createNotificationChannel(channel)
        }
    }

    /** Call once we know an admin is signed in. Best-effort — a failure
     *  here just means this device won't get new-order alerts until the
     *  next successful subscribe attempt; it should never block sign-in. */
    fun subscribeToAdminTopic() {
        runCatching { FirebaseMessaging.getInstance().subscribeToTopic(AdminNotificationConstants.ADMIN_TOPIC) }
    }
}
