package com.example.expensemanager.backup

import android.content.Context
import androidx.core.content.edit
import androidx.work.BackoffPolicy
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkRequest
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.time.Duration
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Auto-backup settings, last-backup bookkeeping and the WorkManager schedule, in one place.
 * Uses SharedPreferences so checking "is it on?" never opens the encrypted database.
 */
@Singleton
internal class AutoBackupStore
    @Inject
    constructor(
        @ApplicationContext context: Context
    ) {
        private val appContext = context.applicationContext
        private val prefs = appContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

        /** Process-lifetime, off-main scope so leaving a screen can't cancel an enqueue. */
        private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

        private val _isEnabled = MutableStateFlow(prefs.getBoolean(KEY_ENABLED, false))
        internal val isEnabled: StateFlow<Boolean> = _isEnabled.asStateFlow()

        /**
         * Turns auto-backup on/off. Enabling resets tracking and runs one backup immediately.
         * Always (re)schedules, even if unchanged; the WorkManager calls are idempotent.
         */
        internal fun setEnabled(enabled: Boolean) {
            if (_isEnabled.value != enabled) {
                prefs.edit { putBoolean(KEY_ENABLED, enabled) }
                _isEnabled.value = enabled
            }
            if (enabled) resetChangeTracking()
            scope.launch {
                val workManager = WorkManager.getInstance(appContext)
                if (enabled) {
                    enqueuePeriodic(workManager)
                    enqueueOneShot(workManager)
                } else {
                    // By tag, so a pending one-shot is cancelled too.
                    workManager.cancelAllWorkByTag(WORK_TAG)
                }
            }
        }

        /** Re-creates the periodic schedule if it went missing (e.g. after a force stop). */
        internal fun ensureScheduled() {
            if (!_isEnabled.value) return
            scope.launch { enqueuePeriodic(WorkManager.getInstance(appContext)) }
        }

        internal fun state(): State =
            State(
                contentSignature = prefs.getString(KEY_SIGNATURE, null),
                lastBackupUri = prefs.getString(KEY_LAST_URI, null)
            )

        /** Records a written backup for the next run to compare against. */
        internal fun recordBackup(
            contentSignature: String,
            uri: String
        ) {
            prefs.edit {
                putString(KEY_SIGNATURE, contentSignature)
                putString(KEY_LAST_URI, uri)
            }
        }

        /** Forces the next run to write a fresh file. */
        private fun resetChangeTracking() {
            prefs.edit {
                remove(KEY_SIGNATURE)
                remove(KEY_LAST_URI)
                // Leftovers from the old dirty-flag scheme.
                remove(LEGACY_KEY_DIRTY)
                remove(LEGACY_KEY_VERIFIED_AT)
            }
        }

        private fun enqueuePeriodic(workManager: WorkManager) {
            val request = PeriodicWorkRequestBuilder<AutoBackupWorker>(REPEAT_INTERVAL)
                .applyCommonPolicy()
                .build()
            // UPDATE creates a missing schedule and keeps the existing run window
            // (REPLACE would push the next run out on every launch).
            workManager.enqueueUniquePeriodicWork(PERIODIC_WORK_NAME, ExistingPeriodicWorkPolicy.UPDATE, request)
        }

        private fun enqueueOneShot(workManager: WorkManager) {
            val request = OneTimeWorkRequestBuilder<AutoBackupWorker>()
                .applyCommonPolicy()
                .build()
            workManager.enqueueUniqueWork(ONE_SHOT_WORK_NAME, ExistingWorkPolicy.REPLACE, request)
        }

        /** Shared constraints, backoff and tag. */
        private fun <B : WorkRequest.Builder<B, *>> B.applyCommonPolicy(): B =
            setConstraints(WORK_CONSTRAINTS)
                .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, BACKOFF_DELAY_MS, TimeUnit.MILLISECONDS)
                .addTag(WORK_TAG)

        /** Last-backup bookkeeping read by the worker. */
        internal data class State(
            val contentSignature: String?,
            val lastBackupUri: String?
        )

        private companion object {
            const val PREFS_NAME = "auto_backup_state"
            const val KEY_ENABLED = "enabled"
            const val KEY_SIGNATURE = "content_signature"
            const val KEY_LAST_URI = "last_backup_uri"
            const val LEGACY_KEY_DIRTY = "dirty"
            const val LEGACY_KEY_VERIFIED_AT = "last_verified_at"

            const val WORK_TAG = "auto_backup"
            const val PERIODIC_WORK_NAME = "auto_backup_periodic"
            const val ONE_SHOT_WORK_NAME = "auto_backup_now"
            const val BACKOFF_DELAY_MS = 10_000L

            /**
             * Minimum gap between checks; Doze/App Standby can defer runs. No flex window, so
             * the first run isn't held to the end of the period.
             */
            val REPEAT_INTERVAL: Duration = Duration.ofHours(12)

            /** No battery constraint: a backup is a small write and matters most on low battery. */
            val WORK_CONSTRAINTS: Constraints = Constraints
                .Builder()
                .setRequiresStorageNotLow(true)
                .build()
        }
    }
