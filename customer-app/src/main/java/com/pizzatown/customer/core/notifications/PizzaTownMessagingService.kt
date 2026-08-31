package com.pizzatown.customer.core.notifications

import android.Manifest
import android.app.PendingIntent
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import com.pizzatown.customer.MainActivity
import com.pizzatown.customer.R
import dagger.hilt.android.AndroidEntryPoint
import java.util.concurrent.atomic.AtomicInteger
import javax.inject.Inject

/**
 * Handles both halves of push notifications for the customer app:
 *  - onNewToken: the device's FCM token changed (fresh install, app data
 *    cleared, token rotated by Google Play services) — re-save it to
 *    users/{uid}.fcmToken so the backend keeps targeting the right device.
 *  - onMessageReceived: a message arrived while the app was in the
 *    foreground (data-only messages, which is all we send, do NOT get
 *    auto-displayed by the system — we build and show the notification
 *    ourselves so it also works consistently in the background/killed
 *    states via this same callback).
 */
@AndroidEntryPoint
class PizzaTownMessagingService : FirebaseMessagingService() {

    @Inject lateinit var firestore: FirebaseFirestore

    override fun onNewToken(token: String) {
        super.onNewToken(token)
        val uid = FirebaseAuth.getInstance().currentUser?.uid ?: return
        firestore.collection("users").document(uid)
            .set(mapOf("fcmToken" to token), SetOptions.merge())
    }

    override fun onMessageReceived(message: RemoteMessage) {
        super.onMessageReceived(message)
        val data = message.data
        val title = data[NotificationConstants.KEY_TITLE] ?: message.notification?.title ?: "Pizza Town"
        val body = data[NotificationConstants.KEY_BODY] ?: message.notification?.body ?: return

        val intent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val pendingIntent = PendingIntent.getActivity(
            this, 0, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(this, NotificationConstants.CHANNEL_ID)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle(title)
            .setContentText(body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(body))
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .build()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ActivityCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
            != PackageManager.PERMISSION_GRANTED
        ) {
            return // user hasn't granted POST_NOTIFICATIONS — nothing we can do
        }
        NotificationManagerCompat.from(this).notify(notificationIdFor(data[NotificationConstants.KEY_TYPE]), notification)
    }

    companion object {
        private val counter = AtomicInteger(0)
        // Group by type so, e.g., a new order-status notification doesn't
        // stack forever, while coupon/broadcast notifications each get their own slot.
        private fun notificationIdFor(type: String?): Int = when (type) {
            NotificationConstants.TYPE_ORDER_STATUS -> 1001
            else -> 2000 + counter.incrementAndGet() % 1000
        }
    }
}
