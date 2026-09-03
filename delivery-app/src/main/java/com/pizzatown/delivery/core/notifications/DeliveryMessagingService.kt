package com.pizzatown.delivery.core.notifications

import android.Manifest
import android.app.PendingIntent
import android.content.Intent
import android.content.pm.PackageManager
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import com.pizzatown.delivery.MainActivity

class DeliveryMessagingService : FirebaseMessagingService() {

    override fun onNewToken(token: String) {
        super.onNewToken(token)

        val uid = FirebaseAuth.getInstance().currentUser?.uid
            ?: return

        FirebaseFirestore.getInstance()
            .collection("users")
            .document(uid)
            .set(
                mapOf("fcmToken" to token),
                SetOptions.merge()
            )
    }

    override fun onMessageReceived(message: RemoteMessage) {
        super.onMessageReceived(message)

        // Make sure the channel exists even if the Activity has not
        // recreated it yet.
        DeliveryNotificationRegistrar().ensureChannel()

        val data = message.data

        val title =
            data[DeliveryNotificationConstants.KEY_TITLE]
                ?: message.notification?.title
                ?: "PizzaTown Delivery"

        val body =
            data[DeliveryNotificationConstants.KEY_BODY]
                ?: message.notification?.body
                ?: "You have a new delivery update."

        val intent = Intent(this, MainActivity::class.java).apply {
            flags =
                Intent.FLAG_ACTIVITY_NEW_TASK or
                Intent.FLAG_ACTIVITY_CLEAR_TOP
        }

        val pendingIntent = PendingIntent.getActivity(
            this,
            2001,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or
                PendingIntent.FLAG_IMMUTABLE
        )

        val notification =
            NotificationCompat.Builder(
                this,
                DeliveryNotificationConstants.CHANNEL_ID
            )
                .setSmallIcon(com.pizzatown.delivery.R.mipmap.ic_launcher)
                .setContentTitle(title)
                .setContentText(body)
                .setStyle(
                    NotificationCompat.BigTextStyle()
                        .bigText(body)
                )
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setAutoCancel(true)
                .setContentIntent(pendingIntent)
                .build()

        if (
            android.os.Build.VERSION.SDK_INT >= 33 &&
            checkSelfPermission(
                Manifest.permission.POST_NOTIFICATIONS
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            return
        }

        NotificationManagerCompat
            .from(this)
            .notify(
                System.currentTimeMillis().toInt(),
                notification
            )
    }
}
