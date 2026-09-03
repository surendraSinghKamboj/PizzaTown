package com.pizzatown.delivery.presentation.profile

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.Logout
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

data class DeliveryProfile(
    val name: String = "Delivery Partner",
    val email: String = "",
    val phone: String = ""
)

@Composable
fun ProfileScreen(
    profile: DeliveryProfile,
    onChangePassword: () -> Unit,
    onAppearance: () -> Unit,
    onLogout: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 16.dp)
    ) {

        Spacer(Modifier.height(8.dp))

        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .height(190.dp),
            shape = RoundedCornerShape(28.dp),
            color = MaterialTheme.colorScheme.primaryContainer
        ) {
            Column(
                modifier = Modifier.padding(22.dp),
                verticalArrangement = Arrangement.Center
            ) {
                Surface(
                    modifier = Modifier.size(68.dp),
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.primary
                ) {
                    Column(
                        modifier = Modifier.fillMaxSize(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Person,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onPrimary,
                            modifier = Modifier.size(32.dp)
                        )
                    }
                }

                Spacer(Modifier.height(14.dp))

                Text(
                    text = profile.name.ifBlank { "Delivery Partner" },
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold
                )

                Spacer(Modifier.height(3.dp))

                Text(
                    text = "PizzaTown Delivery Partner",
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        }

        Spacer(Modifier.height(18.dp))

        ProfileInfoCard(
            label = "Email",
            value = profile.email.ifBlank { "Not available" }
        )

        Spacer(Modifier.height(10.dp))

        ProfileInfoCard(
            label = "Phone",
            value = profile.phone.ifBlank { "Not available" }
        )

        Spacer(Modifier.height(22.dp))

        Text(
            text = "Account",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold
        )

        Spacer(Modifier.height(10.dp))

        ProfileAction(
            icon = Icons.Filled.Key,
            title = "Change Password",
            subtitle = "Update your delivery account password",
            onClick = onChangePassword
        )

        Spacer(Modifier.height(10.dp))

        ProfileAction(
            icon = Icons.Filled.DarkMode,
            title = "Appearance",
            subtitle = "Light, dark or system theme",
            onClick = onAppearance
        )

        Spacer(Modifier.height(10.dp))

        ProfileAction(
            icon = Icons.Filled.Settings,
            title = "App Settings",
            subtitle = "Delivery app preferences",
            onClick = onAppearance
        )

        Spacer(Modifier.height(22.dp))

        OutlinedButton(
            onClick = onLogout,
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp)
        ) {
            Icon(
                imageVector = Icons.Filled.Logout,
                contentDescription = null
            )

            Spacer(Modifier.width(8.dp))

            Text("Sign Out")
        }

        Spacer(Modifier.height(24.dp))
    }
}

@Composable
private fun ProfileInfoCard(
    label: String,
    value: String
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(Modifier.height(4.dp))

            Text(
                text = value,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Medium
            )
        }
    }
}

@Composable
private fun ProfileAction(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    subtitle: String,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .then(
                Modifier
            ),
        shape = RoundedCornerShape(17.dp),
        onClick = onClick
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                modifier = Modifier.size(46.dp),
                shape = RoundedCornerShape(14.dp),
                color = MaterialTheme.colorScheme.secondaryContainer
            ) {
                Column(
                    modifier = Modifier.fillMaxSize(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null
                    )
                }
            }

            Spacer(Modifier.width(13.dp))

            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold
                )

                Spacer(Modifier.height(2.dp))

                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
