package com.pizzatown.customer.presentation.profile

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Work
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.foundation.background
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.pizzatown.customer.core.common.UiState
import com.pizzatown.customer.domain.model.Address
import com.pizzatown.customer.presentation.components.ErrorView
import com.pizzatown.customer.presentation.components.LoadingView
import com.pizzatown.customer.presentation.settings.ThemeSettingRow

import com.pizzatown.customer.presentation.address.AddressManagerScreen
@Composable
fun ProfileScreen(
    onLoggedOut: () -> Unit,
    onViewOrders: () -> Unit,
    openAddressManager: Boolean = false,
    viewModel: ProfileViewModel = hiltViewModel()
) {
    var showAddressManager by remember {
        mutableStateOf(openAddressManager)
    }

    LaunchedEffect(openAddressManager) {
        if (openAddressManager) {
            showAddressManager = true
        }
    }

    var editingAddress by remember {
        mutableStateOf<com.pizzatown.customer.domain.model.Address?>(null)
    }




    if (showAddressManager) {
        AddressManagerScreen(
            existing = editingAddress,
            onBack = {
                showAddressManager = false
                editingAddress = null
            },
            onSave = { address ->
                if (editingAddress == null) {
                    viewModel.addStructuredAddress(address)
                } else {
                    viewModel.updateStructuredAddress(address)
                }

                showAddressManager = false
                editingAddress = null
            }
        )
        return
    }
    val profileState by viewModel.profileState.collectAsStateWithLifecycle()
    val editState by viewModel.editState.collectAsStateWithLifecycle()
    val saving by viewModel.saveInProgress.collectAsStateWithLifecycle()
    val saveSuccess by viewModel.saveSuccess.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }

    

    LaunchedEffect(saveSuccess) {
        if (saveSuccess) {
            snackbarHostState.showSnackbar("Profile updated")
            viewModel.consumeSaveSuccess()
        }
    }

    Scaffold(snackbarHost = { SnackbarHost(snackbarHostState) }) { padding ->
        when (val current = profileState) {
            is UiState.Loading -> LoadingView(Modifier.padding(padding))
            is UiState.Error -> ErrorView(current.message, modifier = Modifier.padding(padding))
            is UiState.Empty -> {}
            is UiState.Success -> {
                val profile = current.data
                Column(
                    Modifier.padding(padding).padding(20.dp).verticalScroll(rememberScrollState()),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Box(
                        modifier = Modifier
                            .size(104.dp)
                            .clip(CircleShape)
                            .background(
                                MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        if (profile.profileImageUrl.isNotBlank()) {
                            AsyncImage(
                                model = profile.profileImageUrl,
                                contentDescription = "Profile photo",
                                contentScale = ContentScale.Crop,
                                modifier = Modifier
                                    .fillMaxSize()
                                    .clip(CircleShape)
                            )
                        } else {
                            val firstLetter = profile.fullName
                                .trim()
                                .firstOrNull()
                                ?.uppercaseChar()
                                ?.toString()
                                ?: "?"

                            Text(
                                text = firstLetter,
                                style = MaterialTheme.typography.displaySmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }

                    Spacer(Modifier.height(12.dp))

                    Text(
                        text = profile.fullName.ifBlank { "Pizza Town Customer" },
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )

                    if (profile.email.isNotBlank()) {
                        Spacer(Modifier.height(2.dp))
                        Text(
                            text = profile.email,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    Spacer(Modifier.height(20.dp))

                    OutlinedTextField(
                        value = editState.fullName, onValueChange = viewModel::onNameChange,
                        label = { Text("Full Name") }, modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(Modifier.height(10.dp))
                    OutlinedTextField(
                        value = editState.mobile, onValueChange = viewModel::onMobileChange,
                        label = { Text("Mobile") }, modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(Modifier.height(10.dp))
                    OutlinedTextField(
                        value = profile.email, onValueChange = {}, readOnly = true,
                        label = { Text("Email") }, modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(Modifier.height(20.dp))
                    Text("Additional Info", style = MaterialTheme.typography.titleMedium)
                    Text(
                        "Share your birthday and anniversary to get special offers from Pizza Town!",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(Modifier.height(8.dp))
                    com.pizzatown.customer.presentation.components.DateField(
                        label = "Date of Birth",
                        epochMillis = editState.dateOfBirth,
                        onDateSelected = viewModel::onDateOfBirthChange,
                        modifier = Modifier.fillMaxWidth(),
                        // Once a birthday is saved to the server it's locked —
                        // profile.dateOfBirth (not editState) is the saved value.
                        locked = profile.dateOfBirth > 0L
                    )
                    if (profile.dateOfBirth > 0L) {
                        Text(
                            "Date of birth can only be set once and can't be changed. Contact support if this is wrong.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Spacer(Modifier.height(10.dp))
                    com.pizzatown.customer.presentation.components.DateField(
                        label = "Anniversary Date",
                        epochMillis = editState.anniversaryDate,
                        onDateSelected = viewModel::onAnniversaryDateChange,
                        modifier = Modifier.fillMaxWidth(),
                        locked = profile.anniversaryDate > 0L
                    )
                    if (profile.anniversaryDate > 0L) {
                        Text(
                            "Anniversary date can only be set once and can't be changed. Contact support if this is wrong.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    Spacer(Modifier.height(20.dp))

                    Button(onClick = viewModel::save, enabled = !saving, modifier = Modifier.fillMaxWidth().height(48.dp)) {
                        if (saving) CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                        else Text("Save Changes")
                    }

                    Spacer(Modifier.height(12.dp))

                    OutlinedButton(onClick = onViewOrders, modifier = Modifier.fillMaxWidth().height(48.dp)) {
                        Icon(Icons.AutoMirrored.Filled.List, contentDescription = null)
                        Spacer(Modifier.width(8.dp))
                        Text("My Orders")
                    }

                    Spacer(Modifier.height(24.dp))
                    HorizontalDivider()
                    Spacer(Modifier.height(16.dp))

                    Row(
                        Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                "Delivery Addresses",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                "Choose where we should deliver your order.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        Button(
                            onClick = {
                                editingAddress = null
                                showAddressManager = true
                            },
                            shape = RoundedCornerShape(13.dp),
                            contentPadding = PaddingValues(
                                horizontal = 16.dp,
                                vertical = 10.dp
                            )
                        ) {
                            Text(
                                "+ Add Address",
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    if (profile.addresses.isEmpty()) {
                        Spacer(Modifier.height(4.dp))
                        Text(
                            "No saved addresses yet.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    } else {
                        Spacer(Modifier.height(8.dp))
                        profile.addresses.forEach { address ->
                            AddressRow(
                                address = address,
                                onSetDefault = { viewModel.setDefaultAddress(address.id) },
                                onEdit = { editingAddress = address; showAddressManager = true },
                                onDelete = { viewModel.deleteAddress(address.id) }
                            )
                            Spacer(Modifier.height(8.dp))
                        }
                    }

                    Spacer(Modifier.height(24.dp))
                    HorizontalDivider()
                    Spacer(Modifier.height(16.dp))

                    ThemeSettingRow()

                    Spacer(Modifier.height(16.dp))

                    OutlinedButton(
                        onClick = { viewModel.logout(); onLoggedOut() },
                        modifier = Modifier.fillMaxWidth().height(48.dp)
                    ) {
                        Icon(Icons.AutoMirrored.Filled.Logout, contentDescription = null)
                        Spacer(Modifier.width(8.dp))
                        Text("Logout")
                    }
                }
            }
        }
    }

    
}

@Composable
private fun AddressRow(
    address: Address,
    onSetDefault: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    val addressIcon = when (address.label.lowercase()) {
        "home" -> Icons.Filled.Home
        "work" -> Icons.Filled.Work
        else -> Icons.Filled.LocationOn
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .animateContentSize(),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        border = BorderStroke(
            width = 1.dp,
            color = if (address.isDefault)
                MaterialTheme.colorScheme.primary.copy(alpha = 0.55f)
            else
                MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.55f)
        ),
        elevation = CardDefaults.cardElevation(
            defaultElevation = 1.dp
        )
    ) {
        Column {

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(15.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {

                Surface(
                    modifier = Modifier.size(46.dp),
                    shape = RoundedCornerShape(14.dp),
                    color = if (address.isDefault)
                        MaterialTheme.colorScheme.primary.copy(alpha = 0.10f)
                    else
                        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f)
                ) {
                    Box(
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            addressIcon,
                            contentDescription = null,
                            tint = if (address.isDefault)
                                MaterialTheme.colorScheme.primary
                            else
                                MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(23.dp)
                        )
                    }
                }

                Spacer(Modifier.width(13.dp))

                Column(
                    modifier = Modifier.weight(1f)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = address.label.ifBlank { "Address" },
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )

                        if (address.isDefault) {
                            Spacer(Modifier.width(7.dp))

                            Surface(
                                shape = RoundedCornerShape(50),
                                color = MaterialTheme.colorScheme.primary.copy(
                                    alpha = 0.10f
                                )
                            ) {
                                Text(
                                    text = "Default",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.primary,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(
                                        horizontal = 8.dp,
                                        vertical = 4.dp
                                    )
                                )
                            }
                        }
                    }

                    Spacer(Modifier.height(4.dp))

                    Text(
                        text = address.fullAddress,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 2
                    )
                }

                Spacer(Modifier.width(4.dp))

                IconButton(
                    onClick = onEdit,
                    modifier = Modifier.size(40.dp)
                ) {
                    Icon(
                        Icons.Filled.Edit,
                        contentDescription = "Edit ${address.label}",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(19.dp)
                    )
                }

                IconButton(
                    onClick = onDelete,
                    modifier = Modifier.size(40.dp)
                ) {
                    Icon(
                        Icons.Filled.Delete,
                        contentDescription = "Delete ${address.label}",
                        tint = MaterialTheme.colorScheme.error,
                        modifier = Modifier.size(19.dp)
                    )
                }
            }

            if (!address.isDefault) {
                HorizontalDivider(
                    color = MaterialTheme.colorScheme.outlineVariant.copy(
                        alpha = 0.35f
                    )
                )

                TextButton(
                    onClick = onSetDefault,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(42.dp)
                ) {
                    Text(
                        "Set as default",
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }
    }
}
