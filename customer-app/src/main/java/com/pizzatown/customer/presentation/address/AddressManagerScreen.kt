package com.pizzatown.customer.presentation.address

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddLocationAlt
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Work
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import com.pizzatown.customer.core.location.LocationProvider
import com.pizzatown.customer.domain.model.Address
import com.pizzatown.customer.domain.model.DeliveryArea
import com.pizzatown.customer.domain.repository.SettingsRepository
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.launch
import org.osmdroid.config.Configuration
import org.osmdroid.events.MapEventsReceiver
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.MapEventsOverlay
import org.osmdroid.views.overlay.Marker
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.pow
import kotlin.math.sin
import kotlin.math.sqrt

private data class SelectedLocation(
    val latitude: Double,
    val longitude: Double
)

private fun distanceKm(
    lat1: Double,
    lon1: Double,
    lat2: Double,
    lon2: Double
): Double {
    val earthRadiusKm = 6371.0
    val dLat = Math.toRadians(lat2 - lat1)
    val dLon = Math.toRadians(lon2 - lon1)
    val a = sin(dLat / 2).pow(2.0) +
        cos(Math.toRadians(lat1)) *
        cos(Math.toRadians(lat2)) *
        sin(dLon / 2).pow(2.0)

    return earthRadiusKm * 2.0 * atan2(sqrt(a), sqrt(1.0 - a))
}

@EntryPoint
@InstallIn(SingletonComponent::class)
private interface AddressSettingsEntryPoint {
    fun settingsRepository(): SettingsRepository
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddressManagerScreen(
    existing: Address? = null,
    onBack: () -> Unit,
    onSave: (address: Address) -> Unit
) {
    val context = LocalContext.current

    val settingsRepository = remember {
        EntryPointAccessors.fromApplication(
            context.applicationContext,
            AddressSettingsEntryPoint::class.java
        ).settingsRepository()
    }

    val locationProvider = remember {
        LocationProvider(context.applicationContext)
    }

    var label by remember { mutableStateOf(existing?.label ?: "Home") }
    var houseFlat by remember { mutableStateOf(existing?.houseFlat ?: "") }
    var areaStreet by remember { mutableStateOf(existing?.areaStreet ?: "") }
    var landmark by remember { mutableStateOf(existing?.landmark ?: "") }
    var city by remember { mutableStateOf(existing?.city ?: "") }
    var pincode by remember { mutableStateOf(existing?.pincode ?: "") }
    var isDefault by remember { mutableStateOf(existing?.isDefault ?: false) }

    var selectedLocation by remember {
        mutableStateOf(
            SelectedLocation(
                existing?.latitude ?: 0.0,
                existing?.longitude ?: 0.0
            )
        )
    }

    var deliveryArea by remember { mutableStateOf(DeliveryArea()) }
    var locationError by remember { mutableStateOf<String?>(null) }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val granted =
            permissions[Manifest.permission.ACCESS_FINE_LOCATION] == true ||
            permissions[Manifest.permission.ACCESS_COARSE_LOCATION] == true

        if (granted) {
            locationError = null
        } else {
            locationError = "Location permission is required to select your delivery location."
        }
    }

    LaunchedEffect(Unit) {
        settingsRepository.observeDeliveryArea().collectLatest {
            deliveryArea = it
        }
    }

    LaunchedEffect(Unit) {
        if (
            selectedLocation.latitude == 0.0 &&
            selectedLocation.longitude == 0.0
        ) {
            val hasPermission =
                ContextCompat.checkSelfPermission(
                    context,
                    Manifest.permission.ACCESS_FINE_LOCATION
                ) == PackageManager.PERMISSION_GRANTED ||
                ContextCompat.checkSelfPermission(
                    context,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                ) == PackageManager.PERMISSION_GRANTED

            if (hasPermission) {
                locationProvider.getCurrentLocation()?.let {
                    selectedLocation = SelectedLocation(
                        it.latitude,
                        it.longitude
                    )
                }
            }
        }
    }

    val hasSelectedLocation =
        selectedLocation.latitude != 0.0 &&
        selectedLocation.longitude != 0.0

    val insideDeliveryArea =
        deliveryArea.isConfigured() &&
        deliveryArea.radiusKm > 0.0 &&
        hasSelectedLocation &&
        distanceKm(
            selectedLocation.latitude,
            selectedLocation.longitude,
            deliveryArea.centerLat,
            deliveryArea.centerLng
        ) <= deliveryArea.radiusKm

    val currentDistanceKm =
        if (hasSelectedLocation && deliveryArea.isConfigured()) {
            distanceKm(
                selectedLocation.latitude,
                selectedLocation.longitude,
                deliveryArea.centerLat,
                deliveryArea.centerLng
            )
        } else {
            null
        }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        if (existing == null) {
                            "Add Address"
                        } else {
                            "Edit Address"
                        }
                    )
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
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                Spacer(Modifier.height(4.dp))

                Text(
                    text = "Address type",
                    style = MaterialTheme.typography.titleMedium
                )

                Spacer(Modifier.height(8.dp))

                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FilterChip(
                        selected = label == "Home",
                        onClick = { label = "Home" },
                        label = { Text("Home") },
                        leadingIcon = {
                            Icon(
                                Icons.Filled.Home,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    )

                    FilterChip(
                        selected = label == "Work",
                        onClick = { label = "Work" },
                        label = { Text("Work") },
                        leadingIcon = {
                            Icon(
                                Icons.Filled.Work,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    )

                    FilterChip(
                        selected = label != "Home" && label != "Work",
                        onClick = {
                            if (label == "Home" || label == "Work") {
                                label = "Other"
                            }
                        },
                        label = { Text("Other") }
                    )
                }
            }

            item {
                OutlinedTextField(
                    value = houseFlat,
                    onValueChange = { houseFlat = it },
                    label = { Text("House / Flat / Building") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
            }

            item {
                OutlinedTextField(
                    value = areaStreet,
                    onValueChange = { areaStreet = it },
                    label = { Text("Area / Street") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
            }

            item {
                OutlinedTextField(
                    value = landmark,
                    onValueChange = { landmark = it },
                    label = { Text("Landmark (optional)") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
            }

            item {
                OutlinedTextField(
                    value = city,
                    onValueChange = { city = it },
                    label = { Text("City") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
            }

            item {
                OutlinedTextField(
                    value = pincode,
                    onValueChange = { pincode = it },
                    label = { Text("Pincode") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
            }

            item {
                Text(
                    text = "Pin your delivery location",
                    style = MaterialTheme.typography.titleMedium
                )

                Spacer(Modifier.height(8.dp))

                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(300.dp),
                    shape = RoundedCornerShape(16.dp),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    AndroidView(
                        modifier = Modifier.fillMaxSize(),
                        factory = { ctx ->
                            Configuration.getInstance().userAgentValue =
                                ctx.packageName

                            MapView(ctx).apply {
                                setTileSource(TileSourceFactory.MAPNIK)
                                setMultiTouchControls(true)

                                val initialLat =
                                    if (hasSelectedLocation) {
                                        selectedLocation.latitude
                                    } else if (deliveryArea.isConfigured()) {
                                        deliveryArea.centerLat
                                    } else {
                                        29.3919
                                    }

                                val initialLng =
                                    if (hasSelectedLocation) {
                                        selectedLocation.longitude
                                    } else if (deliveryArea.isConfigured()) {
                                        deliveryArea.centerLng
                                    } else {
                                        79.4542
                                    }

                                controller.setZoom(15.0)
                                controller.setCenter(
                                    GeoPoint(initialLat, initialLng)
                                )

                                overlays.add(
                                    MapEventsOverlay(
                                        object : MapEventsReceiver {
                                            override fun singleTapConfirmedHelper(
                                                p: GeoPoint
                                            ): Boolean {
                                                selectedLocation =
                                                    SelectedLocation(
                                                        p.latitude,
                                                        p.longitude
                                                    )
                                                locationError = null
                                                return true
                                            }

                                            override fun longPressHelper(
                                                p: GeoPoint
                                            ): Boolean = false
                                        }
                                    )
                                )
                            }
                        },
                        update = { mapView ->
                            mapView.overlays.removeAll {
                                it is Marker
                            }

                            if (hasSelectedLocation) {
                                val marker = Marker(mapView).apply {
                                    position = GeoPoint(
                                        selectedLocation.latitude,
                                        selectedLocation.longitude
                                    )
                                    title = "Delivery location"
                                    setAnchor(
                                        Marker.ANCHOR_CENTER,
                                        Marker.ANCHOR_BOTTOM
                                    )
                                }

                                mapView.overlays.add(marker)

                                mapView.controller.setCenter(
                                    GeoPoint(
                                        selectedLocation.latitude,
                                        selectedLocation.longitude
                                    )
                                )
                            }

                            mapView.invalidate()
                        }
                    )
                }

                Spacer(Modifier.height(6.dp))

                Text(
                    text = "© OpenStreetMap contributors",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            item {
                Button(
                    onClick = {
                        val hasPermission =
                            locationProvider.hasLocationPermission()

                        if (!hasPermission) {
                            permissionLauncher.launch(
                                arrayOf(
                                    Manifest.permission.ACCESS_FINE_LOCATION,
                                    Manifest.permission.ACCESS_COARSE_LOCATION
                                )
                            )
                            return@Button
                        }

                        locationError = null

                        MainScope().launch {
                            locationProvider.getCurrentLocation()?.let {
                                selectedLocation = SelectedLocation(
                                    it.latitude,
                                    it.longitude
                                )
                            } ?: run {
                                locationError =
                                    "Could not get your current location. Please turn on GPS and try again."
                            }
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Icon(
                        Icons.Filled.LocationOn,
                        contentDescription = null
                    )

                    Spacer(Modifier.size(8.dp))

                    Text("Use my current location")
                }
            }

            item {
                if (hasSelectedLocation) {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        Column(
                            modifier = Modifier.padding(14.dp)
                        ) {
                            Text(
                                text = "Selected location",
                                style = MaterialTheme.typography.titleSmall
                            )

                            Spacer(Modifier.height(6.dp))

                            Text(
                                text = "Latitude: %.6f\nLongitude: %.6f".format(
                                    selectedLocation.latitude,
                                    selectedLocation.longitude
                                ),
                                style = MaterialTheme.typography.bodySmall
                            )

                            if (currentDistanceKm != null) {
                                Spacer(Modifier.height(6.dp))

                                Text(
                                    text = if (insideDeliveryArea) {
                                        "Within delivery area: %.2f km from shop".format(
                                            currentDistanceKm
                                        )
                                    } else {
                                        "Outside delivery area: %.2f km from shop (limit %.2f km)".format(
                                            currentDistanceKm,
                                            deliveryArea.radiusKm
                                        )
                                    },
                                    color =
                                        if (insideDeliveryArea) {
                                            MaterialTheme.colorScheme.primary
                                        } else {
                                            MaterialTheme.colorScheme.error
                                        }
                                )
                            }
                        }
                    }
                }
            }

            item {
                if (!deliveryArea.isConfigured()) {
                    Text(
                        text = "Delivery area is not configured yet. Please try again later.",
                        color = MaterialTheme.colorScheme.error
                    )
                }

                if (locationError != null) {
                    Text(
                        text = locationError!!,
                        color = MaterialTheme.colorScheme.error
                    )
                }
            }

            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(
                            text = "Set as default",
                            style = MaterialTheme.typography.titleSmall
                        )

                        Text(
                            text = "Use this address for future orders",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    Switch(
                        checked = isDefault,
                        onCheckedChange = { isDefault = it }
                    )
                }
            }

            item {
                val composedAddress = listOf(
                    houseFlat.trim(),
                    areaStreet.trim(),
                    landmark.trim()
                ).filter {
                    it.isNotBlank()
                }.joinToString(", ")

                Button(
                    onClick = {
                        val address = Address(
                            id = existing?.id
                                ?: java.util.UUID.randomUUID().toString(),
                            label = label.trim().ifBlank { "Home" },
                            fullAddress = composedAddress,
                            houseFlat = houseFlat.trim(),
                            areaStreet = areaStreet.trim(),
                            landmark = landmark.trim(),
                            city = city.trim(),
                            pincode = pincode.trim(),
                            latitude = selectedLocation.latitude,
                            longitude = selectedLocation.longitude,
                            isDefault = isDefault
                        )

                        onSave(address)
                    },
                    enabled =
                        houseFlat.isNotBlank() &&
                        areaStreet.isNotBlank() &&
                        city.isNotBlank() &&
                        pincode.isNotBlank() &&
                        hasSelectedLocation &&
                        deliveryArea.isConfigured() &&
                        insideDeliveryArea,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(54.dp),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Icon(
                        if (existing == null) {
                            Icons.Filled.AddLocationAlt
                        } else {
                            Icons.Filled.Check
                        },
                        contentDescription = null
                    )

                    Spacer(Modifier.size(8.dp))

                    Text(
                        if (existing == null) {
                            "Save Address"
                        } else {
                            "Update Address"
                        }
                    )
                }
            }

            item {
                Spacer(Modifier.height(24.dp))
            }
        }
    }
}
