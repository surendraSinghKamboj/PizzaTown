package com.pizzatown.delivery.core.notifications

import android.app.NotificationChannel
import android.app.NotificationManager
import android.os.Build
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import com.google.firebase.messaging.FirebaseMessaging

class DeliveryNotificationRegistrar {

    fun ensureChannel() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
            return
        }

        val manager =
            com.google.firebase.FirebaseApp
                .getInstance()
                .applicationContext
                .getSystemService(NotificationManager::class.java)

        val channel = NotificationChannel(
            DeliveryNotificationConstants.CHANNEL_ID,
            DeliveryNotificationConstants.CHANNEL_NAME,
            NotificationManager.IMPORTANCE_HIGH
        ).apply {
            description =
                "New delivery assignments and order updates"
            enableVibration(true)
        }

        manager.createNotificationChannel(channel)
    }

    fun registerCurrentToken() {
        val uid = FirebaseAuth.getInstance()
            .currentUser
            ?.uid
            ?: return

        FirebaseMessaging.getInstance()
            .token
            .addOnSuccessListener { token ->
                FirebaseFirestore.getInstance()
                    .collection("users")
                    .document(uid)
                    .set(
                        mapOf(
                            "fcmToken" to token,
                            "fcmTokenUpdatedAt" to System.currentTimeMillis()
                        ),
                        SetOptions.merge()
                    )
            }
            .addOnFailureListener {
                android.util.Log.e(
                    "PizzaTownFCM",
                    "Unable to obtain delivery FCM token",
                    it
                )
            }
    }
}
