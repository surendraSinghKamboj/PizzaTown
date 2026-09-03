package com.pizzatown.delivery.presentation.navigation

import androidx.compose.material3.ExperimentalMaterial3Api
import android.Manifest
import android.content.pm.PackageManager
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Navigation
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.app.ActivityCompat
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.pizzatown.delivery.domain.model.DeliveryOrder
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import org.json.JSONObject
import org.osmdroid.config.Configuration
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker
import org.osmdroid.views.overlay.Polyline
import androidx.compose.ui.viewinterop.AndroidView
import kotlin.math.abs

@Composable
@OptIn(ExperimentalMaterial3Api::class)
fun DeliveryNavigationScreen(
    order: DeliveryOrder,
    onBack: () -> Unit,
    onCall: (String) -> Unit,
    onDelivered: () -> Unit
) {
    val context = LocalContext.current

    var riderLat by remember { mutableStateOf<Double?>(null) }
    var riderLng by remember { mutableStateOf<Double?>(null) }
    var routePoints by remember { mutableStateOf<List<GeoPoint>>(emptyList()) }
    var routeDistanceKm by remember { mutableStateOf<Double?>(null) }
    var routeDurationMin by remember { mutableStateOf<Double?>(null) }
    var routeLoading by remember { mutableStateOf(false) }

    val destination = remember(order.deliveryLat, order.deliveryLng) {
        GeoPoint(order.deliveryLat, order.deliveryLng)
    }

    val fused = remember(context) {
        LocationServices.getFusedLocationProviderClient(context)
    }

    DisposableEffect(Unit) {
        val hasFine =
            ActivityCompat.checkSelfPermission(
                context,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED

        val hasCoarse =
            ActivityCompat.checkSelfPermission(
                context,
                Manifest.permission.ACCESS_COARSE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED

        if (!hasFine && !hasCoarse) {
            onDispose {}
        } else {
            val callback = object : LocationCallback() {
                override fun onLocationResult(result: LocationResult) {
                    val location = result.lastLocation ?: return
                    riderLat = location.latitude
                    riderLng = location.longitude
                }
            }

            val request = LocationRequest.Builder(
                Priority.PRIORITY_HIGH_ACCURACY,
                5000L
            )
                .setMinUpdateIntervalMillis(3000L)
                .setWaitForAccurateLocation(false)
                .build()

            fused.requestLocationUpdates(request, callback, android.os.Looper.getMainLooper())

            onDispose {
                fused.removeLocationUpdates(callback)
            }
        }
    }

    LaunchedEffect(riderLat, riderLng, order.deliveryLat, order.deliveryLng) {
        val lat = riderLat ?: return@LaunchedEffect
        val lng = riderLng ?: return@LaunchedEffect

        if (
            order.deliveryLat !in -90.0..90.0 ||
            order.deliveryLng !in -180.0..180.0 ||
            order.deliveryLat == 0.0 && order.deliveryLng == 0.0
        ) {
            return@LaunchedEffect
        }

        routeLoading = true

        val result = withContext(Dispatchers.IO) {
            runCatching {
                val url =
                    "https://router.project-osrm.org/route/v1/driving/" +
                        "$lng,$lat;${order.deliveryLng},${order.deliveryLat}" +
                        "?overview=full&geometries=geojson&steps=false"

                val connection =
                    java.net.URL(url).openConnection() as java.net.HttpURLConnection

                try {
                    connection.requestMethod = "GET"
                    connection.connectTimeout = 10000
                    connection.readTimeout = 15000
                    connection.setRequestProperty("User-Agent", "PizzaTown-Delivery/1.0")

                    val responseCode = connection.responseCode
                    if (responseCode !in 200..299) {
                        error("Routing service returned HTTP $responseCode")
                    }

                    val body = connection.inputStream.bufferedReader().use { it.readText() }
                    val root = JSONObject(body)

                    if (root.optString("code") != "Ok") {
                        error("No road route available.")
                    }

                    val route = root
                        .getJSONArray("routes")
                        .getJSONObject(0)

                    val distanceKm = route.getDouble("distance") / 1000.0
                    val durationMin = route.getDouble("duration") / 60.0

                    val geometry =
                        route.getJSONObject("geometry").getJSONArray("coordinates")

                    val points = buildList {
                        for (i in 0 until geometry.length()) {
                            val coordinate = geometry.getJSONArray(i)
                            add(
                                GeoPoint(
                                    coordinate.getDouble(1),
                                    coordinate.getDouble(0)
                                )
                            )
                        }
                    }

                    Triple(points, distanceKm, durationMin)
                } finally {
                    connection.disconnect()
                }
            }.getOrNull()
        }

        routePoints = result?.first.orEmpty()
        routeDistanceKm = result?.second
        routeDurationMin = result?.third
        routeLoading = false

        delay(5000)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text("Live Navigation")
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            Icons.Filled.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            AndroidView(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                factory = { ctx ->
                    Configuration.getInstance().userAgentValue = ctx.packageName

                    MapView(ctx).apply {
                        setTileSource(TileSourceFactory.MAPNIK)
                        setMultiTouchControls(true)
                        minZoomLevel = 3.0
                        maxZoomLevel = 20.0
                        controller.setZoom(15.0)
                        controller.setCenter(destination)
                    }
                },
                update = { map ->
                    map.overlays.removeAll { true }

                    val destinationMarker = Marker(map).apply {
                        position = destination
                        title = "Customer"
                        snippet = order.customerAddress
                        setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
                        icon = context.getDrawable(com.pizzatown.delivery.R.drawable.ic_launcher_foreground)
                    }

                    map.overlays.add(destinationMarker)

                    val riderLatValue = riderLat
                    val riderLngValue = riderLng

                    if (riderLatValue != null && riderLngValue != null) {
                        val riderMarker = Marker(map).apply {
                            position = GeoPoint(riderLatValue, riderLngValue)
                            title = "You"
                            snippet = "Live rider location"
                            setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
                        }
                        map.overlays.add(riderMarker)
                    }

                    if (routePoints.size >= 2) {
                        val polyline = Polyline().apply {
                            setPoints(routePoints)
                            width = 10f
                            title = "Delivery route"
                        }
                        map.overlays.add(polyline)
                    }

                    val centerPoint = riderLatValue?.let { lat ->
                        riderLngValue?.let { lng -> GeoPoint(lat, lng) }
                    } ?: destination

                    map.controller.setCenter(centerPoint)
                    map.invalidate()
                }
            )

            Text(
                text = "© OpenStreetMap contributors",
                style = MaterialTheme.typography.labelSmall,
                modifier = Modifier.padding(
                    horizontal = 12.dp,
                    vertical = 4.dp
                )
            )

            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp)
            ) {
                Column(
                    modifier = Modifier.padding(14.dp)
                ) {
                    Text(
                        "#${order.orderId.takeLast(6).uppercase()}",
                        style = MaterialTheme.typography.titleMedium
                    )

                    Spacer(Modifier.height(4.dp))

                    Text(
                        order.customerName.ifBlank { "Customer" },
                        style = MaterialTheme.typography.titleLarge
                    )

                    Text(
                        order.customerAddress.ifBlank {
                            "Customer address unavailable"
                        },
                        style = MaterialTheme.typography.bodySmall
                    )

                    Spacer(Modifier.height(8.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Text(
                            routeDistanceKm?.let {
                                "${"%.1f".format(it)} km"
                            } ?: if (routeLoading) {
                                "Routing..."
                            } else {
                                "Route unavailable"
                            },
                            style = MaterialTheme.typography.titleSmall
                        )

                        Text(
                            routeDurationMin?.let {
                                "${"%.0f".format(it)} min"
                            } ?: "",
                            style = MaterialTheme.typography.titleSmall
                        )
                    }

                    Spacer(Modifier.height(10.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedButton(
                            onClick = {
                                onCall(order.customerPhone)
                            },
                            modifier = Modifier.weight(1f),
                            enabled = order.customerPhone.isNotBlank()
                        ) {
                            Icon(
                                Icons.Filled.Phone,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(Modifier.size(6.dp))
                            Text("Call")
                        }

                        Button(
                            onClick = onDelivered,
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(
                                Icons.Filled.Navigation,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(Modifier.size(6.dp))
                            Text("Mark Delivered")
                        }
                    }

                    Text(
                        "Live location updates are active while this delivery is in progress.",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 8.dp)
                    )
                }
            }

            Spacer(Modifier.height(8.dp))
        }
    }
}
