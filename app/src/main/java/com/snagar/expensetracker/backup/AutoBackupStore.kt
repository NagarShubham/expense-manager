package com.snagar.expensetracker.backup

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
import com.snagar.expensetracker.data.ExpenseRepository
import dagger.Lazy
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
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
        @ApplicationContext context: Context,
        private val expenseRepository: Lazy<ExpenseRepository>,
        private val fileStore: BackupFileStore
    ) {
        private val appContext = context.applicationContext
        private val prefs = appContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

        /** Process-lifetime, off-main scope so leaving a screen can't cancel an enqueue. */
        private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

        private val _isEnabled = MutableStateFlow(prefs.getBoolean(KEY_ENABLED, false))
        internal val isEnabled: StateFlow<Boolean> = _isEnabled.asStateFlow()

        private var expenseWatchJob: Job? = null

        /**
         * Turns auto-backup on/off. WorkManager is only scheduled when this is on **and**
         * there is at least one expense; an empty list never enqueues work.
         */
        internal fun setEnabled(enabled: Boolean) {
            if (_isEnabled.value != enabled) {
                prefs.edit { putBoolean(KEY_ENABLED, enabled) }
                _isEnabled.value = enabled
            }
            syncSchedule()
        }

        /** Re-creates the periodic schedule if it went missing (e.g. after a force stop). */
        internal fun ensureScheduled() {
            syncSchedule()
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

        /**
         * Stops WorkManager when a worker saw no expenses, after re-checking the live
         * count so a concurrent insert is not cancelled out of the schedule.
         */
        internal fun cancelIfStillEmpty() {
            scope.launch {
                val hasExpenses = expenseRepository.get().getExpenseCount().first() > 0
                if (!hasExpenses) {
                    WorkManager.getInstance(appContext).cancelAllWorkByTag(WORK_TAG)
                }
            }
        }

        /**
         * Enabled + expenses → 12-hour periodic work (and a one-shot if nothing is backed
         * up yet). Otherwise cancel every auto-backup worker, including a pending one-shot.
         */
        private fun syncSchedule() {
            expenseWatchJob?.cancel()
            expenseWatchJob = null
            if (!_isEnabled.value) {
                cancelAllWork()
                return
            }

            expenseWatchJob = scope.launch {
                expenseRepository.get()
                    .getExpenseCount()
                    .map { it > 0 }
                    .distinctUntilChanged()
                    .collect { hasExpenses -> applySchedule(hasExpenses) }
            }
        }

        private fun applySchedule(hasExpenses: Boolean) {
            val workManager = WorkManager.getInstance(appContext)
            if (!_isEnabled.value || !hasExpenses) {
                workManager.cancelAllWorkByTag(WORK_TAG)
                return
            }
            enqueuePeriodic(workManager)
            if (!isAlreadyBackedUp()) {
                enqueueOneShot(workManager)
            }
        }

        private fun isAlreadyBackedUp(): Boolean {
            val current = state()
            return !current.contentSignature.isNullOrEmpty() && fileStore.exists(current.lastBackupUri)
        }

        private fun cancelAllWork() {
            scope.launch { WorkManager.getInstance(appContext).cancelAllWorkByTag(WORK_TAG) }
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
            // KEEP: a launch/toggle must not restart a backup that is already queued.
            workManager.enqueueUniqueWork(ONE_SHOT_WORK_NAME, ExistingWorkPolicy.KEEP, request)
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
