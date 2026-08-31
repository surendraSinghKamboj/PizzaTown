package com.pizzatown.customer.core.preferences

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

private val Context.notificationDataStore by preferencesDataStore(name = "pizza_town_notifications")

/**
 * Tracks when the customer last opened their notification inbox, so the
 * bell icon badge can show how many messages arrived since then —
 * without needing a per-message "read" flag in Firestore.
 *
 * This is an interface (rather than just a class) so ViewModels can be
 * unit-tested with a plain in-memory fake instead of needing a real
 * Android Context for DataStore.
 */
interface NotificationPreferences {
    val lastSeenAt: Flow<Long>
    suspend fun markSeenNow()
}

@Singleton
class NotificationPreferenceRepository @Inject constructor(
    @dagger.hilt.android.qualifiers.ApplicationContext private val context: Context
) : NotificationPreferences {
    private val lastSeenKey = longPreferencesKey("last_seen_broadcasts_at")

    override val lastSeenAt: Flow<Long> = context.notificationDataStore.data.map { prefs -> prefs[lastSeenKey] ?: 0L }

    override suspend fun markSeenNow() {
        context.notificationDataStore.edit { prefs -> prefs[lastSeenKey] = System.currentTimeMillis() }
    }
}
