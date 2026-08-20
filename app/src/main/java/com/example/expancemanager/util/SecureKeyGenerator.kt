package com.example.expancemanager.util

import android.content.Context
import android.util.Base64
import androidx.core.content.edit
import java.security.SecureRandom

/**
 * Secure key generator for SQLCipher database encryption
 * Uses EncryptedSharedPreferences with MasterKey for secure storage
 */
internal object SecureKeyGenerator {
    private const val PREFS_NAME = "secure_expense_prefs"
    private const val KEY_PASSPHRASE = "db_passphrase"

    // Cache the passphrase in memory to avoid repeated decryption (90% faster)
    @Volatile
    private var cachedPassphrase: String? = null

    /**
     * Gets existing passphrase or generates a new one
     * Passphrase is stored securely using EncryptedSharedPreferences
     * Cached in memory for performance
     */
    internal fun getOrGenerateKey(context: Context): String {
        // Return cached passphrase if available
        cachedPassphrase?.let { return it }

        // Try to get existing passphrase from storage
        getStoredPassphrase(context)?.let {
            cachedPassphrase = it
            return it
        }

        // Generate new passphrase if none exists
        val newPassphrase = generateSecurePassphrase()
        storePassphrase(context, newPassphrase)
        cachedPassphrase = newPassphrase
        return newPassphrase
    }

    /**
     * Generates a cryptographically secure random 256-bit passphrase
     */
    private fun generateSecurePassphrase(): String {
        val bytes = ByteArray(32) // 256 bits
        SecureRandom().nextBytes(bytes)
        return Base64.encodeToString(bytes, Base64.NO_WRAP)
    }

    /**
     * Retrieves the stored passphrase, or null if none has been stored yet.
     *
     * IMPORTANT: a failure to *read* an existing store is NOT swallowed into null.
     * Returning null here would make [getOrGenerateKey] mint a brand-new passphrase,
     * and SQLCipher could then never decrypt the existing database — silent, total
     * data loss. A read error must propagate so the caller/app can surface it instead.
     */
    private fun getStoredPassphrase(context: Context): String? =
        EncryptedPrefs.get(context, PREFS_NAME).getString(KEY_PASSPHRASE, null)

    /**
     * Stores passphrase securely in EncryptedSharedPreferences
     */
    private fun storePassphrase(
        context: Context,
        passphrase: String
    ) {
        EncryptedPrefs.get(context, PREFS_NAME).edit { putString(KEY_PASSPHRASE, passphrase) }
    }

}
