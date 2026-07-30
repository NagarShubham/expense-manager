package com.example.expancemanager.util

import android.app.Activity
import android.app.KeyguardManager
import android.content.Context
import android.content.Intent
import android.hardware.biometrics.BiometricPrompt as PlatformBiometricPrompt
import android.os.Build
import android.os.CancellationSignal
import androidx.activity.ComponentActivity
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import com.example.expancemanager.R
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class BiometricAuthenticator @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private var boundActivity: ComponentActivity? = null
    private var credentialLauncher: ActivityResultLauncher<Intent>? = null
    private var pendingSuccess: (() -> Unit)? = null
    private var pendingError: ((String) -> Unit)? = null

    fun bindActivity(activity: ComponentActivity) {
        if (boundActivity === activity) return
        boundActivity = activity
        credentialLauncher = activity.registerForActivityResult(
            ActivityResultContracts.StartActivityForResult()
        ) { result ->
            if (result.resultCode == Activity.RESULT_OK) {
                pendingSuccess?.invoke()
            } else {
                pendingError?.invoke(context.getString(R.string.biometric_auth_canceled))
            }
            pendingSuccess = null
            pendingError = null
        }
    }

    fun canAuthenticate(): Boolean =
        context.getSystemService(KeyguardManager::class.java)?.isDeviceSecure == true

    fun authenticate(
        activity: ComponentActivity,
        title: String = context.getString(R.string.biometric_prompt_title),
        subtitle: String = context.getString(R.string.biometric_prompt_subtitle),
        onSuccess: () -> Unit,
        onError: (String) -> Unit,
        onFailed: () -> Unit = {}
    ) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            authenticateWithPlatformPrompt(activity, title, subtitle, onSuccess, onError, onFailed)
        } else {
            authenticateWithDeviceCredential(activity, title, subtitle, onSuccess, onError)
        }
    }

    @Suppress("NewApi")
    private fun authenticateWithPlatformPrompt(
        activity: ComponentActivity,
        title: String,
        subtitle: String,
        onSuccess: () -> Unit,
        onError: (String) -> Unit,
        onFailed: () -> Unit
    ) {
        val executor = ContextCompat.getMainExecutor(activity)
        val callback = object : PlatformBiometricPrompt.AuthenticationCallback() {
            override fun onAuthenticationSucceeded(result: PlatformBiometricPrompt.AuthenticationResult) {
                onSuccess()
            }

            override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                if (errorCode != PlatformBiometricPrompt.BIOMETRIC_ERROR_USER_CANCELED &&
                    errorCode != PlatformBiometricPrompt.BIOMETRIC_ERROR_CANCELED
                ) {
                    onError(errString.toString())
                }
            }

            override fun onAuthenticationFailed() {
                onFailed()
            }
        }

        val builder = PlatformBiometricPrompt.Builder(activity)
            .setTitle(title)
            .setSubtitle(subtitle)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            builder.setAllowedAuthenticators(allowedAuthenticators())
        } else {
            @Suppress("DEPRECATION")
            builder.setDeviceCredentialAllowed(true)
        }

        builder.build().authenticate(CancellationSignal(), executor, callback)
    }

    private fun authenticateWithDeviceCredential(
        activity: ComponentActivity,
        title: String,
        subtitle: String,
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        val unavailable = context.getString(R.string.settings_biometric_unavailable)
        val keyguardManager = activity.getSystemService(KeyguardManager::class.java)
        if (keyguardManager == null || !keyguardManager.isDeviceSecure) {
            onError(unavailable)
            return
        }
        val intent = keyguardManager.createConfirmDeviceCredentialIntent(title, subtitle)
            ?: run {
                onError(unavailable)
                return
            }
        val launcher = credentialLauncher ?: run {
            onError(unavailable)
            return
        }
        pendingSuccess = onSuccess
        pendingError = onError
        launcher.launch(intent)
    }

    private fun allowedAuthenticators(): Int =
        android.hardware.biometrics.BiometricManager.Authenticators.BIOMETRIC_STRONG or
            android.hardware.biometrics.BiometricManager.Authenticators.DEVICE_CREDENTIAL
}
