package com.example.expancemanager.util

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import java.util.concurrent.ConcurrentHashMap

/**
 * Single source of truth for building [EncryptedSharedPreferences]. Both the theme
 * preferences ([com.example.expancemanager.data.PreferenceRepository]) and the DB
 * passphrase store ([SecureKeyGenerator]) go through here so their MasterKey and
 * encryption schemes can never drift apart (a mismatch would corrupt decryption).
 *
 * Instances are cached per file name; [EncryptedSharedPreferences.create] is relatively
 * expensive and safe to reuse.
 */
internal object EncryptedPrefs {
    private val cache = ConcurrentHashMap<String, SharedPreferences>()

    internal fun get(
        context: Context,
        fileName: String
    ): SharedPreferences = cache.getOrPut(fileName) { create(context.applicationContext, fileName) }

    private fun create(
        context: Context,
        fileName: String
    ): SharedPreferences {
        val masterKey = MasterKey
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
