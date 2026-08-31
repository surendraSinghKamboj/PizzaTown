package com.pizzatown.customer.core.notifications

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import androidx.core.content.getSystemService
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import com.google.firebase.messaging.FirebaseMessaging
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Wires up the client side of push notifications:
 *  1. Creates the notification channel (required once, API 26+).
 *  2. On sign-in, fetches the current FCM token and saves it to
 *     users/{uid}.fcmToken so Cloud Functions know where to send
 *     order-status / coupon notifications for this customer.
 *  3. Subscribes every signed-in device to the [NotificationConstants.BROADCAST_TOPIC]
 *     topic so admin broadcasts (which are sent to the topic, not a
 *     specific token) reach everyone.
 *
 * Runtime POST_NOTIFICATIONS permission (Android 13+) is requested
 * separately from MainActivity, since that needs an Activity.
 */
@Singleton
class NotificationRegistrar @Inject constructor(
    @ApplicationContext private val context: Context,
    private val firestore: FirebaseFirestore
) {
    fun ensureChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                NotificationConstants.CHANNEL_ID,
                NotificationConstants.CHANNEL_NAME,
                NotificationManager.IMPORTANCE_HIGH
            ).apply { description = NotificationConstants.CHANNEL_DESCRIPTION }
            context.getSystemService<NotificationManager>()?.createNotificationChannel(channel)
        }
    }

    /** Call once after we know the user is signed in (e.g. from MainActivity). */
    suspend fun syncForSignedInUser(userId: String) {
        runCatching {
            val token = FirebaseMessaging.getInstance().token.await()
            firestore.collection("users").document(userId)
                .set(mapOf("fcmToken" to token), SetOptions.merge())
                .await()
            FirebaseMessaging.getInstance().subscribeToTopic(NotificationConstants.BROADCAST_TOPIC).await()
        }
        // Best-effort: a failure here (offline, etc.) just means notifications
        // are delayed until the next successful sync — never block sign-in on it.
    }
}
