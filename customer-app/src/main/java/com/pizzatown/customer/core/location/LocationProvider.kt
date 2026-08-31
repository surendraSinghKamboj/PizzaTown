package com.pizzatown.customer.core.location

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

data class LatLng(val latitude: Double, val longitude: Double)

/**
 * Thin wrapper around FusedLocationProviderClient. Location is mandatory
 * for this app (orders are only accepted inside the shop's delivery
 * radius — see firestore.rules / DeliveryArea), so every caller of
 * [getCurrentLocation] should already have confirmed the permission via
 * a runtime request; this class re-checks defensively and simply
 * returns null rather than crashing if it's somehow still missing.
 */
@Singleton
class LocationProvider @Inject constructor(
    @ApplicationContext private val context: Context
) {
    fun hasLocationPermission(): Boolean =
        ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED ||
            ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED

    suspend fun getCurrentLocation(): LatLng? {
        if (!hasLocationPermission()) return null
        val client = LocationServices.getFusedLocationProviderClient(context)
        val cancellationSource = CancellationTokenSource()
        return suspendCancellableCoroutine { continuation ->
            try {
                client.getCurrentLocation(Priority.PRIORITY_BALANCED_POWER_ACCURACY, cancellationSource.token)
                    .addOnSuccessListener { location ->
                        continuation.resume(location?.let { LatLng(it.latitude, it.longitude) })
                    }
                    .addOnFailureListener { continuation.resume(null) }
            } catch (e: SecurityException) {
                continuation.resume(null)
            }
            continuation.invokeOnCancellation { cancellationSource.cancel() }
        }
    }
}
