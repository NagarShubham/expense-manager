# R8 rules for ExpanceManager
# ProGuard-only flags (-optimizationpasses, -dontusemixedcaseclassnames,
# -dontskipnonpubliclibraryclasses, -verbose) are intentionally omitted —
# they are no-ops or unsupported under R8.

# ============================================================================
# SQLCipher
# ============================================================================

-keep class net.zetetic.database.** { *; }
-keep class net.sqlcipher.** { *; }
-dontwarn net.zetetic.database.**
-dontwarn net.sqlcipher.**

# ============================================================================
# Room
# ============================================================================

-keep class * extends androidx.room.RoomDatabase
-keep @androidx.room.Entity class *

# ============================================================================
# Android Security Crypto (EncryptedSharedPreferences + Tink)
# ============================================================================

-keep class androidx.security.crypto.** { *; }
-keep class com.google.crypto.tink.** { *; }
-dontwarn com.google.crypto.tink.**

# ============================================================================
# Android Keystore / JCA
# ============================================================================

-keep class android.security.keystore.** { *; }
-keep class javax.crypto.** { *; }
-keep class java.security.** { *; }

# ============================================================================
# App encryption classes
# ============================================================================

-keep class com.example.expancemanager.util.SecureKeyGenerator { *; }
-keep class com.example.expancemanager.data.** { *; }

# Gson maps backup JSON by field name. Keep the payload type; entities are already
# covered by data.** above.
-keep class com.example.expancemanager.util.BackupManager$BackupData { *; }

# ============================================================================
# Kotlin Coroutines
# ============================================================================

-keepnames class kotlinx.coroutines.internal.MainDispatcherFactory {}
-keepnames class kotlinx.coroutines.CoroutineExceptionHandler {}
-keepclassmembers class kotlinx.** {
    volatile <fields>;
}

# ============================================================================
# Native methods & exceptions
# ============================================================================

-keepclasseswithmembernames class * {
    native <methods>;
}

-keep public class * extends java.lang.Exception
