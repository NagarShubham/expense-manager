package com.example.expancemanager.ui.screen

import androidx.activity.ComponentActivity
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.unit.dp
import com.example.expancemanager.R
import com.example.expancemanager.ui.components.AppSpacing
import com.example.expancemanager.ui.theme.AppRadius
import com.example.expancemanager.ui.theme.appColors
import com.example.expancemanager.util.BiometricAuthenticator

/**
 * Tracks in-flight system flows (document picker, credential screen, etc.) where [ON_STOP]
 * must not trigger the biometric lock, otherwise activity-result callbacks are lost.
 */
internal interface BiometricLockHandle {
    fun beginExternalFlow()

    fun endExternalFlow()
}

internal val LocalBiometricLockHandle = compositionLocalOf<BiometricLockHandle> {
    error("BiometricLockHandle not provided")
}

@Composable
internal fun BiometricAppGate(
    isBiometricLockEnabled: Boolean,
    activity: ComponentActivity,
    biometricAuthenticator: BiometricAuthenticator,
    content: @Composable () -> Unit
) {
    var isUnlocked by remember { mutableStateOf(false) }
    var externalFlowCount by remember { mutableIntStateOf(0) }

    val lockHandle = remember {
        object : BiometricLockHandle {
            override fun beginExternalFlow() {
                externalFlowCount++
            }

            override fun endExternalFlow() {
                externalFlowCount = (externalFlowCount - 1).coerceAtLeast(0)
            }
        }
    }

    LaunchedEffect(isBiometricLockEnabled) {
        isUnlocked = !isBiometricLockEnabled
    }

    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner, isBiometricLockEnabled) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_STOP &&
                isBiometricLockEnabled &&
                externalFlowCount == 0
            ) {
                isUnlocked = false
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    CompositionLocalProvider(LocalBiometricLockHandle provides lockHandle) {
        Box(modifier = Modifier.fillMaxSize()) {
            content()
            if (isBiometricLockEnabled && !isUnlocked) {
                BiometricLockGate(
                    activity = activity,
                    biometricAuthenticator = biometricAuthenticator,
                    onAuthenticated = { isUnlocked = true }
                )
            }
        }
    }
}

@Composable
private fun BiometricLockGate(
    activity: ComponentActivity,
    biometricAuthenticator: BiometricAuthenticator,
    onAuthenticated: () -> Unit
) {
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var authAttempt by remember { mutableIntStateOf(0) }
    val authFailedMessage = stringResource(R.string.biometric_auth_failed)
    val currentOnAuthenticated by rememberUpdatedState(onAuthenticated)

    LaunchedEffect(authAttempt) {
        biometricAuthenticator.authenticate(
            activity = activity,
            onSuccess = { currentOnAuthenticated() },
            onError = { errorMessage = it },
            onFailed = { errorMessage = authFailedMessage }
        )
    }

    BiometricLockScreen(
        errorMessage = errorMessage,
        onUnlockClick = {
            errorMessage = null
            authAttempt++
        }
    )
}

/**
 * Full-screen lock gate. Painted on the brand gradient so the very first thing the
 * app shows is its own identity rather than a blank surface.
 */
@Composable
private fun BiometricLockScreen(
    errorMessage: String? = null,
    onUnlockClick: () -> Unit
) {
    val appColors = MaterialTheme.appColors
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(appColors.heroGradient))
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(AppSpacing.xxlarge),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Surface(
                modifier = Modifier.size(88.dp),
                shape = AppRadius.hero,
                color = appColors.onHero.copy(alpha = 0.16f)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text(text = "🔒", fontSize = 40.sp)
                }
            }
            Spacer(modifier = Modifier.height(AppSpacing.xlarge))
            Text(
                text = stringResource(R.string.biometric_lock_title),
                style = MaterialTheme.typography.headlineMedium,
                color = appColors.onHero,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(AppSpacing.small))
            Text(
                text = stringResource(R.string.biometric_lock_subtitle),
                style = MaterialTheme.typography.bodyMedium,
                color = appColors.onHeroMuted,
                textAlign = TextAlign.Center
            )
            if (errorMessage != null) {
                Spacer(modifier = Modifier.height(AppSpacing.default))
                Surface(
                    shape = AppRadius.chip,
                    color = MaterialTheme.colorScheme.errorContainer
                ) {
                    Text(
                        text = errorMessage,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onErrorContainer,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(
                            horizontal = AppSpacing.default,
                            vertical = AppSpacing.small
                        )
                    )
                }
            }
            Spacer(modifier = Modifier.height(AppSpacing.xxlarge))
            Button(
                onClick = onUnlockClick,
                shape = AppRadius.pill,
                colors = ButtonDefaults.buttonColors(
                    containerColor = appColors.onHero,
                    contentColor = MaterialTheme.colorScheme.primary
                ),
                modifier = Modifier.height(52.dp)
            ) {
                Text(
                    text = stringResource(R.string.biometric_unlock_button),
                    style = MaterialTheme.typography.labelLarge,
                    modifier = Modifier.padding(horizontal = AppSpacing.large)
                )
            }
        }
    }
}
