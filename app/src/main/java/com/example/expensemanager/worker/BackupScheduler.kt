package com.example.expensemanager.worker

import android.content.Context
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import java.util.concurrent.TimeUnit

/**
 * Schedules the recurring [AutoBackupWorker] with WorkManager (no AlarmManager).
 *
 * The job repeats every 15 minutes — the minimum interval WorkManager allows for periodic
 * work. WorkManager persists the schedule across app restarts and device reboots and honors
 * Doze/battery constraints, so exact timing isn't guaranteed. Each run still writes a backup
 * only when the data changed since the last one (see [AutoBackupWorker]).
 *
 * Scheduling on every app start is safe: [ExistingPeriodicWorkPolicy.UPDATE] keeps the single
 * unique job and just refreshes its spec, so it never stacks duplicate workers.
 */
internal object BackupScheduler {
    private const val WORK_NAME = "auto_backup_periodic"
    private const val REPEAT_INTERVAL_MINUTES = 15L

    fun schedule(context: Context) {
        val constraints = Constraints.Builder()
            .setRequiresStorageNotLow(true)
            .build()

        val request = PeriodicWorkRequestBuilder<AutoBackupWorker>(
            REPEAT_INTERVAL_MINUTES,
            TimeUnit.MINUTES
        ).setConstraints(constraints).build()

        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
            WORK_NAME,
            ExistingPeriodicWorkPolicy.UPDATE,
            request
        )
    }
}
