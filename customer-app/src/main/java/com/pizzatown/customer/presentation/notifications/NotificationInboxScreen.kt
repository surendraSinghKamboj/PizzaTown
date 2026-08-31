package com.pizzatown.customer.presentation.notifications

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.Alignment
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Campaign
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.pizzatown.customer.core.common.UiState
import com.pizzatown.customer.domain.model.Broadcast
import com.pizzatown.customer.presentation.components.EmptyView
import com.pizzatown.customer.presentation.components.ErrorView
import com.pizzatown.customer.presentation.components.LoadingView
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NotificationInboxScreen(
    onBack: () -> Unit,
    viewModel: NotificationInboxViewModel = hiltViewModel()
) {
    val state by viewModel.inboxState.collectAsStateWithLifecycle()

    LaunchedEffect(Unit) {
        viewModel.markSeen()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            "Notifications",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )

                        Text(
                            "Offers, updates & order news",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                }
            )
        }
    ) { padding ->

        when (val current = state) {

            is UiState.Loading -> {
                LoadingView(
                    Modifier.padding(padding)
                )
            }

            is UiState.Error -> {
                ErrorView(
                    current.message,
                    modifier = Modifier.padding(padding)
                )
            }

            is UiState.Empty -> {
                NotificationEmptyState(
                    modifier = Modifier.padding(padding)
                )
            }

            is UiState.Success -> {

                LazyColumn(
                    modifier = Modifier
                        .padding(padding)
                        .fillMaxSize(),

                    contentPadding = PaddingValues(
                        horizontal = 16.dp,
                        vertical = 12.dp
                    ),

                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {

                    item {
                        Text(
                            if (current.data.size == 1)
                                "1 update"
                            else
                                "${current.data.size} updates",

                            style = MaterialTheme.typography.bodyMedium,

                            color = MaterialTheme.colorScheme.onSurfaceVariant,

                            modifier = Modifier.padding(
                                horizontal = 4.dp,
                                vertical = 2.dp
                            )
                        )
                    }

                    items(
                        current.data,
                        key = { it.id }
                    ) { broadcast ->

                        NotificationCard(
                            broadcast = broadcast,
                            modifier = Modifier.animateItem()
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun NotificationCard(
    broadcast: Broadcast,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .animateContentSize(),

        shape = RoundedCornerShape(18.dp),

        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),

        elevation = CardDefaults.cardElevation(
            defaultElevation = 2.dp
        ),

        border = BorderStroke(
            1.dp,
            MaterialTheme.colorScheme.outlineVariant.copy(
                alpha = 0.55f
            )
        )
    ) {

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),

            verticalAlignment = Alignment.Top
        ) {

            // Notification icon container
            Surface(
                shape = RoundedCornerShape(14.dp),

                color = MaterialTheme.colorScheme.surfaceVariant.copy(
                    alpha = 0.55f
                )
            ) {
                Icon(
                    Icons.Filled.Campaign,
                    contentDescription = null,

                    tint = MaterialTheme.colorScheme.primary,

                    modifier = Modifier
                        .size(46.dp)
                        .padding(11.dp)
                )
            }

            Spacer(Modifier.width(12.dp))

            Column(
                modifier = Modifier.weight(1f)
            ) {

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.Top
                ) {

                    Text(
                        broadcast.title,

                        style = MaterialTheme.typography.titleMedium,

                        fontWeight = FontWeight.Bold,

                        modifier = Modifier.weight(1f)
                    )

                    Spacer(Modifier.width(8.dp))

                    Text(
                        relativeNotificationTime(
                            broadcast.createdAt
                        ),

                        style = MaterialTheme.typography.labelSmall,

                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Spacer(Modifier.height(6.dp))

                Text(
                    broadcast.message,

                    style = MaterialTheme.typography.bodyMedium,

                    color = MaterialTheme.colorScheme.onSurface,

                    lineHeight = MaterialTheme.typography.bodyMedium.lineHeight
                )

                Spacer(Modifier.height(9.dp))

                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {

                    Surface(
                        shape = RoundedCornerShape(50),
                        color = MaterialTheme.colorScheme.primary.copy(
                            alpha = 0.08f
                        )
                    ) {
                        Text(
                            "Pizza Town",

                            style = MaterialTheme.typography.labelSmall,

                            color = MaterialTheme.colorScheme.primary,

                            fontWeight = FontWeight.SemiBold,

                            modifier = Modifier.padding(
                                horizontal = 8.dp,
                                vertical = 5.dp
                            )
                        )
                    }

                    Spacer(Modifier.width(8.dp))

                    Text(
                        formatNotificationDate(
                            broadcast.createdAt
                        ),

                        style = MaterialTheme.typography.labelSmall,

                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@Composable
private fun NotificationEmptyState(
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(32.dp),

            horizontalAlignment = Alignment.CenterHorizontally
        ) {

            Surface(
                shape = RoundedCornerShape(24.dp),

                color = MaterialTheme.colorScheme.surfaceVariant.copy(
                    alpha = 0.55f
                )
            ) {
                Icon(
                    Icons.Filled.Campaign,
                    contentDescription = null,

                    tint = MaterialTheme.colorScheme.primary,

                    modifier = Modifier
                        .size(76.dp)
                        .padding(20.dp)
                )
            }

            Spacer(Modifier.height(18.dp))

            Text(
                "You're all caught up",

                style = MaterialTheme.typography.titleLarge,

                fontWeight = FontWeight.Bold
            )

            Spacer(Modifier.height(7.dp))

            Text(
                "Offers, order updates and important messages from Pizza Town will appear here.",

                style = MaterialTheme.typography.bodyMedium,

                color = MaterialTheme.colorScheme.onSurfaceVariant,

                textAlign = TextAlign.Center
            )
        }
    }
}

private fun relativeNotificationTime(
    epochMillis: Long
): String {
    if (epochMillis <= 0L) return ""

    val diff = System.currentTimeMillis() - epochMillis

    val minute = 60_000L
    val hour = 60 * minute
    val day = 24 * hour

    return when {
        diff < minute ->
            "Now"

        diff < hour ->
            "${diff / minute}m"

        diff < day ->
            "${diff / hour}h"

        diff < 7 * day ->
            "${diff / day}d"

        else ->
            formatNotificationDate(epochMillis)
    }
}

private fun formatNotificationDate(
    epochMillis: Long
): String {
    if (epochMillis <= 0L) return ""

    return SimpleDateFormat(
        "d MMM, h:mm a",
        Locale.getDefault()
    ).format(Date(epochMillis))
}

