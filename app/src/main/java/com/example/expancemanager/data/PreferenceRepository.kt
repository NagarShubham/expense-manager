package com.example.expancemanager.data

import android.content.Context
import android.content.res.Configuration
import androidx.core.content.edit
import com.example.expancemanager.util.EncryptedPrefs
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PreferenceRepository
    @Inject
    constructor(
        @ApplicationContext context: Context
    ) {
        private val prefs = EncryptedPrefs.get(context, PREFS_NAME)
        private val legacyPrefs by lazy {
            context.getSharedPreferences(LEGACY_PREFS_NAME, Context.MODE_PRIVATE)
        }
        private val defaultDarkTheme =
            (context.resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK) ==
                Configuration.UI_MODE_NIGHT_YES

        private val _isDarkTheme = MutableStateFlow(loadDarkTheme())
        internal val isDarkTheme: StateFlow<Boolean> = _isDarkTheme.asStateFlow()

        private val _isBiometricLockEnabled = MutableStateFlow(loadBiometricLockEnabled())
        internal val isBiometricLockEnabled: StateFlow<Boolean> = _isBiometricLockEnabled.asStateFlow()

        internal fun setDarkTheme(enabled: Boolean) {
            if (_isDarkTheme.value == enabled) return
            prefs.edit { putBoolean(KEY_DARK_THEME, enabled) }
            _isDarkTheme.value = enabled
        }

        internal fun setBiometricLockEnabled(enabled: Boolean) {
            if (_isBiometricLockEnabled.value == enabled) return
            prefs.edit { putBoolean(KEY_BIOMETRIC_LOCK, enabled) }
            _isBiometricLockEnabled.value = enabled
        }

        private fun loadBiometricLockEnabled(): Boolean =
            prefs.getBoolean(KEY_BIOMETRIC_LOCK, false)

        private fun loadDarkTheme(): Boolean {
            if (prefs.contains(KEY_DARK_THEME)) {
                return prefs.getBoolean(KEY_DARK_THEME, defaultDarkTheme)
            }
            if (!legacyPrefs.contains(KEY_DARK_THEME)) {
                return defaultDarkTheme
            }
            // Migrate legacy preference to encrypted storage.
            return legacyPrefs.getBoolean(KEY_DARK_THEME, defaultDarkTheme).also { value ->
                prefs.edit { putBoolean(KEY_DARK_THEME, value) }
                legacyPrefs.edit { remove(KEY_DARK_THEME) }
            }
        }

        private companion object {
            const val PREFS_NAME = "encrypted_app_preferences"
            const val LEGACY_PREFS_NAME = "app_preferences"
            const val KEY_DARK_THEME = "dark_theme"
            const val KEY_BIOMETRIC_LOCK = "biometric_lock_enabled"
        }
    }
