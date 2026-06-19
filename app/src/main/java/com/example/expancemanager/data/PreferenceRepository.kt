package com.example.expancemanager.data

import android.content.Context
import android.content.SharedPreferences
import android.content.res.Configuration
import androidx.core.content.edit
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.concurrent.ConcurrentHashMap
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PreferenceRepository @Inject constructor(
    @ApplicationContext context: Context
) {
    private val prefs = EncryptedPreferences.get(context, PREFS_NAME)
    private val legacyPrefs by lazy {
        context.getSharedPreferences(LEGACY_PREFS_NAME, Context.MODE_PRIVATE)
    }
    private val defaultDarkTheme =
        (context.resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK) ==
            Configuration.UI_MODE_NIGHT_YES

    private val _isDarkTheme = MutableStateFlow(loadDarkTheme())
    val isDarkTheme: StateFlow<Boolean> = _isDarkTheme.asStateFlow()

    fun setDarkTheme(enabled: Boolean) {
        if (_isDarkTheme.value == enabled) return
        prefs.edit { putBoolean(KEY_DARK_THEME, enabled) }
        _isDarkTheme.value = enabled
    }

    private fun loadDarkTheme(): Boolean {
        if (prefs.contains(KEY_DARK_THEME)) {
            return prefs.getBoolean(KEY_DARK_THEME, defaultDarkTheme)
        }
        if (!legacyPrefs.contains(KEY_DARK_THEME)) {
            return defaultDarkTheme
        }
        return legacyPrefs.getBoolean(KEY_DARK_THEME, defaultDarkTheme).also { value ->
            prefs.edit { putBoolean(KEY_DARK_THEME, value) }
            legacyPrefs.edit { remove(KEY_DARK_THEME) }
        }
    }

    private companion object {
        const val PREFS_NAME = "encrypted_app_preferences"
        const val LEGACY_PREFS_NAME = "app_preferences"
        const val KEY_DARK_THEME = "dark_theme"
    }
}

private object EncryptedPreferences {
    private val cache = ConcurrentHashMap<String, SharedPreferences>()

    fun get(
        context: Context,
        fileName: String
    ): SharedPreferences =
        cache.getOrPut(fileName) { create(context.applicationContext, fileName) }

    private fun create(
        context: Context,
        fileName: String
    ): SharedPreferences {
        val masterKey =
            MasterKey
                .Builder(context)
                .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
                .build()

        return EncryptedSharedPreferences.create(
            context,
            fileName,
            masterKey,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        )
    }
}
