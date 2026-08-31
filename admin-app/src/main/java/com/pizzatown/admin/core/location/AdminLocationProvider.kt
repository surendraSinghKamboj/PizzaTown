package com.pizzatown.admin.core.location

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import androidx.core.content.ContextCompat
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.tasks.CancellationTokenSource
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.suspendCancellableCoroutine
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.resume

data class AdminLatLng(val latitude: Double, val longitude: Double)

@Singleton
class AdminLocationProvider @Inject constructor(
    @ApplicationContext private val context: Context
) {
    fun hasLocationPermission(): Boolean =
        ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED ||
        ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED

    suspend fun getCurrentLocation(): AdminLatLng? {
        if (!hasLocationPermission()) return null
        val client = LocationServices.getFusedLocationProviderClient(context)
        val source = CancellationTokenSource()
        return suspendCancellableCoroutine { continuation ->
            try {
                client.getCurrentLocation(Priority.PRIORITY_HIGH_ACCURACY, source.token)
                    .addOnSuccessListener { location ->
                        continuation.resume(location?.let { AdminLatLng(it.latitude, it.longitude) })
                    }
                    .addOnFailureListener { continuation.resume(null) }
            } catch (_: SecurityException) {
                continuation.resume(null)
            }
            continuation.invokeOnCancellation { source.cancel() }
        }
    }
}
