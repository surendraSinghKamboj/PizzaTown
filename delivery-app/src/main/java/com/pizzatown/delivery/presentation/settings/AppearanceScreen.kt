package com.pizzatown.delivery.presentation.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.pizzatown.delivery.presentation.home.DeliveryAppearance

@Composable
fun AppearanceScreen(
    selected: DeliveryAppearance,
    onSelected: (DeliveryAppearance) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(20.dp)
    ) {
        Text(
            text = "Appearance",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold
        )

        Spacer(Modifier.padding(top = 14.dp))

        AppearanceOption(
            title = "System default",
            subtitle = "Follow your phone's theme",
            selected = selected == DeliveryAppearance.SYSTEM,
            onClick = {
                onSelected(DeliveryAppearance.SYSTEM)
            }
        )

        Spacer(Modifier.padding(top = 10.dp))

        AppearanceOption(
            title = "Light",
            subtitle = "Soft light interface",
            selected = selected == DeliveryAppearance.LIGHT,
            onClick = {
                onSelected(DeliveryAppearance.LIGHT)
            }
        )

        Spacer(Modifier.padding(top = 10.dp))

        AppearanceOption(
            title = "Dark",
            subtitle = "Comfortable dark interface",
            selected = selected == DeliveryAppearance.DARK,
            onClick = {
                onSelected(DeliveryAppearance.DARK)
            }
        )
    }
}

@Composable
private fun AppearanceOption(
    title: String,
    subtitle: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        onClick = onClick,
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )

                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            RadioButton(
                selected = selected,
                onClick = onClick
            )
        }
    }
}
