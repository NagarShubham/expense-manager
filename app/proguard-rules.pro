# Add project specific ProGuard rules here.
# You can control the set of applied configuration files using the
# proguardFiles setting in build.gradle.
#
# For more details, see
#   http://developer.android.com/guide/developing/tools/proguard.html

# If your project uses WebView with JS, uncomment the following
# and specify the fully qualified class name to the JavaScript interface
# class:
#-keepclassmembers class fqcn.of.javascript.interface.for.webview {
#   public *;
#}

# Uncomment this to preserve the line number information for
# debugging stack traces.
#-keepattributes SourceFile,LineNumberTable

# If you keep the line number information, uncomment this to
# hide the original source file name.
#-renamesourcefileattribute SourceFile

# ============================================================================
# SQLCipher Database Encryption Rules
# ============================================================================

# Keep SQLCipher native library
-keep class net.zetetic.database.** { *; }
-keep class net.sqlcipher.** { *; }

# Keep database classes
-keep class * extends androidx.room.RoomDatabase
-keep @androidx.room.Entity class *
-dontwarn net.zetetic.database.**
-dontwarn net.sqlcipher.**

# ============================================================================
# Android Security Crypto (EncryptedSharedPreferences)
# ============================================================================

-keep class androidx.security.crypto.** { *; }
-keep class com.google.crypto.tink.** { *; }
-dontwarn com.google.crypto.tink.**

# ============================================================================
# Android Keystore
# ============================================================================

-keep class android.security.keystore.** { *; }
-keep class javax.crypto.** { *; }
-keep class java.security.** { *; }

# ============================================================================
# App-specific encryption classes
# ============================================================================

# Keep security utilities - these handle encryption
-keep class com.example.expancemanager.util.SecureKeyGenerator { *; }
-keepclassmembers class com.example.expancemanager.util.SecureKeyGenerator {
    public *;
}

# Keep database classes
-keep class com.example.expancemanager.data.** { *; }
-keepclassmembers class com.example.expancemanager.data.** {
    public *;
}

# ============================================================================
# Kotlin Coroutines
# ============================================================================

-keepnames class kotlinx.coroutines.internal.MainDispatcherFactory {}
-keepnames class kotlinx.coroutines.CoroutineExceptionHandler {}
-keepclassmembers class kotlinx.** {
    volatile <fields>;
}

# ============================================================================
# General Optimization Settings
# ============================================================================

-optimizationpasses 5
-dontusemixedcaseclassnames
-dontskipnonpubliclibraryclasses
-verbose

# Keep native methods
-keepclasseswithmembernames class * {
    native <methods>;
}

# Keep custom exceptions
-keep public class * extends java.lang.Exception