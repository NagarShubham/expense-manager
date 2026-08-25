package com.example.expancemanager.util

import android.app.Activity
import android.app.KeyguardManager
import android.content.Context
import android.content.Intent
import android.hardware.biometrics.BiometricManager
import android.os.Build
import android.os.CancellationSignal
import androidx.activity.ComponentActivity
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import com.example.expancemanager.R
import dagger.hilt.android.qualifiers.ApplicationContext
import java.util.concurrent.Executor
import javax.inject.Inject
import javax.inject.Singleton
import android.hardware.biometrics.BiometricPrompt as PlatformBiometricPrompt

@Singleton
class BiometricAuthenticator
    @Inject
    constructor(
        @ApplicationContext context: Context
    ) {
        private val mainExecutor: Executor = ContextCompat.getMainExecutor(context)
        private val keyguardManager: KeyguardManager? = context.getSystemService(KeyguardManager::class.java)
        private val promptTitle: String = context.getString(R.string.biometric_prompt_title)
        private val promptSubtitle: String = context.getString(R.string.biometric_prompt_subtitle)
        private val unavailableMessage: String = context.getString(R.string.settings_biometric_unavailable)
        private val canceledMessage: String = context.getString(R.string.biometric_auth_canceled)

        private var boundActivity: ComponentActivity? = null
        private var credentialLauncher: ActivityResultLauncher<Intent>? = null
        private var pendingCallbacks: AuthCallbacks? = null
        private var activeCancellation: CancellationSignal? = null

        private data class AuthCallbacks(
            val onSuccess: () -> Unit,
            val onError: (String) -> Unit
        )

        internal fun bindActivity(activity: ComponentActivity) {
            if (boundActivity === activity) return
            releaseActivityBinding()
            boundActivity = activity
            credentialLauncher = activity.registerForActivityResult(
                ActivityResultContracts.StartActivityForResult()
            ) { result ->
                val callbacks = pendingCallbacks
                pendingCallbacks = null
                when {
                    callbacks == null -> Unit
                    result.resultCode == Activity.RESULT_OK -> callbacks.onSuccess()
                    else -> callbacks.onError(canceledMessage)
                }
            }
        }

        internal fun unbindActivity(activity: ComponentActivity) {
            if (boundActivity !== activity) return
            releaseActivityBinding()
        }

        internal fun canAuthenticate(): Boolean = keyguardManager?.isDeviceSecure == true

        internal fun authenticate(
            activity: ComponentActivity,
            title: String = promptTitle,
            subtitle: String = promptSubtitle,
            onSuccess: () -> Unit,
            onError: (String) -> Unit,
            onFailed: () -> Unit = {}
        ) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                showPlatformPrompt(activity, title, subtitle, onSuccess, onError, onFailed)
            } else {
                showDeviceCredential(title, subtitle, onSuccess, onError)
            }
        }

        private fun releaseActivityBinding() {
            activeCancellation?.cancel()
            activeCancellation = null
            pendingCallbacks = null
            boundActivity = null
            credentialLauncher = null
        }

        @Suppress("NewApi")
        private fun showPlatformPrompt(
            activity: ComponentActivity,
            title: String,
            subtitle: String,
            onSuccess: () -> Unit,
            onError: (String) -> Unit,
            onFailed: () -> Unit
        ) {
            activeCancellation?.cancel()
            val cancellation = CancellationSignal().also { activeCancellation = it }

            val callback = object : PlatformBiometricPrompt.AuthenticationCallback() {
                override fun onAuthenticationSucceeded(result: PlatformBiometricPrompt.AuthenticationResult) {
                    clearActiveCancellation(cancellation)
                    onSuccess()
                }

                override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                    clearActiveCancellation(cancellation)
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
                builder.setAllowedAuthenticators(AUTHENTICATORS)
            } else {
                @Suppress("DEPRECATION")
                builder.setDeviceCredentialAllowed(true)
            }
            builder.build().authenticate(cancellation, mainExecutor, callback)
        }

        private fun showDeviceCredential(
            title: String,
            subtitle: String,
            onSuccess: () -> Unit,
            onError: (String) -> Unit
        ) {
            if (!canAuthenticate()) {
                onError(unavailableMessage)
                return
            }
            val intent = keyguardManager?.createConfirmDeviceCredentialIntent(title, subtitle)
                ?: run {
                    onError(unavailableMessage)
                    return
                }
            val launcher = credentialLauncher ?: run {
                onError(unavailableMessage)
                return
            }
            pendingCallbacks = AuthCallbacks(onSuccess, onError)
            launcher.launch(intent)
        }

        private fun clearActiveCancellation(cancellation: CancellationSignal) {
            if (activeCancellation === cancellation) {
                activeCancellation = null
            }
        }

        private companion object {
            const val AUTHENTICATORS =
                BiometricManager.Authenticators.BIOMETRIC_STRONG or
                    BiometricManager.Authenticators.DEVICE_CREDENTIAL
        }
    }
