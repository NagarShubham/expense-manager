package com.example.expensemanager

import android.app.Application
import androidx.hilt.work.HiltWorkerFactory
import androidx.work.Configuration
import com.example.expensemanager.worker.BackupScheduler
import dagger.hilt.android.HiltAndroidApp
import javax.inject.Inject

@HiltAndroidApp
class ExpenseManagerApplication :
    Application(),
    Configuration.Provider {
    @Inject
    lateinit var workerFactory: HiltWorkerFactory

    // On-demand WorkManager initialization: supply the Hilt-aware WorkerFactory so the
    // scheduled worker can be constructor-injected. The default initializer is removed
    // in AndroidManifest.xml so this configuration is the one that takes effect.
    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder()
            .setWorkerFactory(workerFactory)
            .build()

    override fun onCreate() {
        super.onCreate()
        BackupScheduler.schedule(this)
    }
}
