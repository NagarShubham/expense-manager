package com.example.expensemanager

import android.app.Application
import androidx.hilt.work.HiltWorkerFactory
import androidx.work.Configuration
import dagger.hilt.android.HiltAndroidApp
import javax.inject.Inject

@HiltAndroidApp
class ExpenseManagerApplication :
    Application(),
    Configuration.Provider {
    @Inject
    internal lateinit var workerFactory: HiltWorkerFactory

    /**
     * WorkManager is initialised on demand (its default startup provider is removed in
     * the manifest), so the first `WorkManager.getInstance()` call picks this up instead
     * of every cold start paying for it. The Hilt factory is what lets
     * [com.example.expensemanager.backup.AutoBackupWorker] be constructor-injected.
     */
    override val workManagerConfiguration: Configuration
        get() = Configuration
            .Builder()
            .setWorkerFactory(workerFactory)
            .build()
}
