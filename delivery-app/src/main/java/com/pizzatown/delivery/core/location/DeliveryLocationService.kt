package com.pizzatown.delivery.core.location

import android.Manifest
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import androidx.core.app.ServiceCompat
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationResult
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FieldValue

class DeliveryLocationService : Service() {

    private val fused by lazy {
        LocationServices.getFusedLocationProviderClient(this)
    }

    private val auth by lazy {
        FirebaseAuth.getInstance()
    }

    private val firestore by lazy {
        FirebaseFirestore.getInstance()
    }

    private val callback =
        object : LocationCallback() {
            override fun onLocationResult(
                result: LocationResult
            ) {
                val location =
                    result.lastLocation ?: return

                val uid = auth.currentUser?.uid
                    ?: return

                firestore
                    .collection("users")
                    .document(uid)
                    .update(
                        mapOf(
                            "liveLocation" to mapOf(
                                "latitude" to location.latitude,
                                "longitude" to location.longitude,
                                "accuracy" to location.accuracy.toDouble(),
                                "updatedAt" to FieldValue.serverTimestamp()
                            )
                        )
                    )
            }
        }

    override fun onCreate() {
        super.onCreate()

        createChannel()

        val notification: Notification =
            NotificationCompat.Builder(
                this,
                CHANNEL_ID
            )
                .setSmallIcon(
                    com.pizzatown.delivery.R.mipmap.ic_launcher
                )
                .setContentTitle("PizzaTown Delivery")
                .setContentText("Live delivery location is active")
                .setOngoing(true)
                .build()

        ServiceCompat.startForeground(
            this,
            NOTIFICATION_ID,
            notification,
            if (Build.VERSION.SDK_INT >= 29) {
                android.content.pm.ServiceInfo.FOREGROUND_SERVICE_TYPE_LOCATION
            } else {
                0
            }
        )

        startLocationUpdates()
    }

    private fun startLocationUpdates() {

        val fine =
            checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) ==
                PackageManager.PERMISSION_GRANTED

        val coarse =
            checkSelfPermission(Manifest.permission.ACCESS_COARSE_LOCATION) ==
                PackageManager.PERMISSION_GRANTED

        if (!fine && !coarse) {
            stopSelf()
            return
        }

        val request =
            LocationRequest.Builder(
                Priority.PRIORITY_HIGH_ACCURACY,
                10_000L
            )
                .setMinUpdateIntervalMillis(5_000L)
                .setWaitForAccurateLocation(false)
                .build()

        fused.requestLocationUpdates(
            request,
            callback,
            mainLooper
        )
    }

    override fun onDestroy() {
        fused.removeLocationUpdates(callback)
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun createChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val manager =
                getSystemService(NotificationManager::class.java)

            manager.createNotificationChannel(
                NotificationChannel(
                    CHANNEL_ID,
                    "Live Delivery Location",
                    NotificationManager.IMPORTANCE_LOW
                )
            )
        }
    }

    companion object {
        const val CHANNEL_ID =
            "pizzatown_delivery_live_location"

        const val NOTIFICATION_ID = 3001

        const val ACTION_START =
            "com.pizzatown.delivery.START_LOCATION"

        const val ACTION_STOP =
            "com.pizzatown.delivery.STOP_LOCATION"
    }
}
