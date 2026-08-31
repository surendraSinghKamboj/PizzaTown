package com.pizzatown.admin.ui.theme

import android.content.Context
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton
import androidx.datastore.preferences.core.edit

enum class ThemeMode {
    SYSTEM,
    LIGHT,
    DARK
}

private val Context.adminAppearanceDataStore by preferencesDataStore(
    name = "admin_appearance"
)

@Singleton
class AppearancePreferences @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private companion object {
        val THEME_MODE = stringPreferencesKey("theme_mode")
    }

    val themeMode: Flow<ThemeMode> =
        context.adminAppearanceDataStore.data.map { preferences ->
            runCatching {
                ThemeMode.valueOf(
                    preferences[THEME_MODE] ?: ThemeMode.SYSTEM.name
                )
            }.getOrDefault(ThemeMode.SYSTEM)
        }

    suspend fun setThemeMode(mode: ThemeMode) {
        context.adminAppearanceDataStore.edit { preferences ->
            preferences[THEME_MODE] = mode.name
        }
    }
}
