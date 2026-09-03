package com.pizzatown.admin.presentation.delivery

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.RestartAlt
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.CardDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.pizzatown.admin.domain.model.DeliveryPartner

import androidx.compose.runtime.collectAsState
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DeliveryPartnersScreen(
    onBack: () -> Unit,
    viewModel: DeliveryPartnersViewModel = hiltViewModel()
) {
    val partners by viewModel.partners.collectAsState()
    val loading by viewModel.loading.collectAsState()
    val message by viewModel.message.collectAsState()

    var showCreate by remember { mutableStateOf(false) }
    var editPartner by remember { mutableStateOf<DeliveryPartner?>(null) }
    var resetPartner by remember { mutableStateOf<DeliveryPartner?>(null) }
    var deletePartner by remember { mutableStateOf<DeliveryPartner?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "Delivery Partners",
                        fontWeight = FontWeight.Bold
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.Default.RestartAlt,
                            contentDescription = "Back"
                        )
                    }
                },
                actions = {
                    IconButton(
                        onClick = { showCreate = true }
                    ) {
                        Icon(
                            imageVector = Icons.Default.PersonAdd,
                            contentDescription = "Add delivery partner"
                        )
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showCreate = true }
            ) {
                Icon(
                    imageVector = Icons.Default.PersonAdd,
                    contentDescription = "Add delivery partner"
                )
            }
        }
    ) { padding ->

        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .padding(padding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {

            item {
                Spacer(Modifier.height(8.dp))

                Text(
                    text = "${partners.size} delivery account${if (partners.size == 1) "" else "s"}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            if (message != null) {
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            message.orEmpty(),
                            modifier = Modifier.padding(14.dp)
                        )
                    }
                }
            }

            items(
                items = partners,
                key = { it.id }
            ) { partner ->

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(
                        containerColor =
                            MaterialTheme.colorScheme.surface
                    )
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(
                                modifier = Modifier.weight(1f)
                            ) {
                                Text(
                                    partner.name.ifBlank {
                                        "Delivery Partner"
                                    },
                                    style =
                                        MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )

                                Spacer(Modifier.height(3.dp))

                                Text(partner.email)

                                Text(
                                    partner.phone,
                                    color =
                                        MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }

                            Text(
                                if (partner.active) "ACTIVE" else "DISABLED",
                                style =
                                    MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                color =
                                    if (partner.active) {
                                        MaterialTheme.colorScheme.primary
                                    } else {
                                        MaterialTheme.colorScheme.error
                                    }
                            )
                        }

                        Spacer(Modifier.height(14.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement =
                                Arrangement.spacedBy(8.dp)
                        ) {
                            OutlinedButton(
                                modifier = Modifier.weight(1f),
                                onClick = {
                                    editPartner = partner
                                }
                            ) {
                                Icon(
                                    Icons.Default.Edit,
                                    contentDescription = null
                                )
                                Spacer(Modifier.width(5.dp))
                                Text("Edit")
                            }

                            OutlinedButton(
                                modifier = Modifier.weight(1f),
                                onClick = {
                                    resetPartner = partner
                                }
                            ) {
                                Icon(
                                    Icons.Default.Key,
                                    contentDescription = null
                                )
                                Spacer(Modifier.width(5.dp))
                                Text("Password")
                            }
                        }

                        Spacer(Modifier.height(8.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement =
                                Arrangement.spacedBy(8.dp)
                        ) {
                            OutlinedButton(
                                modifier = Modifier.weight(1f),
                                onClick = {
                                    viewModel.toggleActive(partner)
                                }
                            ) {
                                Text(
                                    if (partner.active) {
                                        "Disable"
                                    } else {
                                        "Enable"
                                    }
                                )
                            }

                            OutlinedButton(
                                modifier = Modifier.weight(1f),
                                onClick = {
                                    deletePartner = partner
                                }
                            ) {
                                Icon(
                                    Icons.Default.Delete,
                                    contentDescription = null
                                )
                                Spacer(Modifier.width(5.dp))
                                Text("Delete")
                            }
                        }
                    }
                }
            }

            item {
                Spacer(Modifier.height(90.dp))
            }
        }
    }

    if (showCreate) {
        CreateDeliveryPartnerDialog(
            loading = loading,
            onDismiss = {
                showCreate = false
            },
            onCreate = { name, email, phone, password ->
                viewModel.createPartner(
                    name = name,
                    email = email,
                    phone = phone,
                    password = password
                ) {
                    showCreate = false
                }
            }
        )
    }

    editPartner?.let { partner ->
        EditDeliveryPartnerDialog(
            partner = partner,
            loading = loading,
            onDismiss = {
                editPartner = null
            },
            onSave = { name, email, phone ->
                viewModel.updatePartner(
                    partner = partner,
                    name = name,
                    email = email,
                    phone = phone
                ) {
                    editPartner = null
                }
            }
        )
    }

    resetPartner?.let { partner ->
        ResetPasswordDialog(
            partner = partner,
            loading = loading,
            onDismiss = {
                resetPartner = null
            },
            onReset = { password ->
                viewModel.resetPassword(
                    partner = partner,
                    password = password
                ) {
                    resetPartner = null
                }
            }
        )
    }

    deletePartner?.let { partner ->
        AlertDialog(
            onDismissRequest = {
                deletePartner = null
            },
            title = {
                Text("Delete Delivery Partner")
            },
            text = {
                Text(
                    "This permanently removes the delivery account and its profile. Continue?"
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.deletePartner(partner) {
                            deletePartner = null
                        }
                    },
                    enabled = !loading
                ) {
                    Text("Delete")
                }
            },
            dismissButton = {
                OutlinedButton(
                    onClick = {
                        deletePartner = null
                    }
                ) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
private fun CreateDeliveryPartnerDialog(
    loading: Boolean,
    onDismiss: () -> Unit,
    onCreate: (String, String, String, String) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var phone by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Create Delivery Account") },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(9.dp)
            ) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Name") },
                    singleLine = true
                )
                OutlinedTextField(
                    value = email,
                    onValueChange = { email = it },
                    label = { Text("Email") },
                    singleLine = true
                )
                OutlinedTextField(
                    value = phone,
                    onValueChange = { phone = it },
                    label = { Text("Phone") },
                    singleLine = true
                )
                OutlinedTextField(
                    value = password,
                    onValueChange = { password = it },
                    label = { Text("Temporary password") },
                    singleLine = true
                )
            }
        },
        confirmButton = {
            Button(
                enabled = !loading,
                onClick = {
                    onCreate(name, email, phone, password)
                }
            ) {
                Text(if (loading) "Creating..." else "Create")
            }
        },
        dismissButton = {
            OutlinedButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

@Composable
private fun EditDeliveryPartnerDialog(
    partner: DeliveryPartner,
    loading: Boolean,
    onDismiss: () -> Unit,
    onSave: (String, String, String) -> Unit
) {
    var name by remember(partner.id) {
        mutableStateOf(partner.name)
    }
    var email by remember(partner.id) {
        mutableStateOf(partner.email)
    }
    var phone by remember(partner.id) {
        mutableStateOf(partner.phone)
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Edit Delivery Partner") },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(9.dp)
            ) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Name") },
                    singleLine = true
                )
                OutlinedTextField(
                    value = email,
                    onValueChange = { email = it },
                    label = { Text("Email") },
                    singleLine = true
                )
                OutlinedTextField(
                    value = phone,
                    onValueChange = { phone = it },
                    label = { Text("Phone") },
                    singleLine = true
                )
            }
        },
        confirmButton = {
            Button(
                enabled = !loading,
                onClick = {
                    onSave(name, email, phone)
                }
            ) {
                Text(if (loading) "Saving..." else "Save Changes")
            }
        },
        dismissButton = {
            OutlinedButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

@Composable
private fun ResetPasswordDialog(
    partner: DeliveryPartner,
    loading: Boolean,
    onDismiss: () -> Unit,
    onReset: (String) -> Unit
) {
    var password by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text("Reset Password")
        },
        text = {
            Column {
                Text(
                    "Set a new password for ${partner.name.ifBlank { "this partner" }}."
                )

                Spacer(Modifier.height(10.dp))

                OutlinedTextField(
                    value = password,
                    onValueChange = { password = it },
                    label = { Text("New password") },
                    singleLine = true
                )
            }
        },
        confirmButton = {
            Button(
                enabled = !loading,
                onClick = {
                    onReset(password)
                }
            ) {
                Text(if (loading) "Resetting..." else "Reset Password")
            }
        },
        dismissButton = {
            OutlinedButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}
