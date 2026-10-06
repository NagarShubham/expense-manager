package com.snagar.expensetracker

import android.app.Application
import androidx.hilt.work.HiltWorkerFactory
import androidx.work.Configuration
import dagger.hilt.android.HiltAndroidApp
import javax.inject.Inject

@HiltAndroidApp
class ExpenseTrackerApplication :
    Application(),
    Configuration.Provider {
    @Inject
    internal lateinit var workerFactory: HiltWorkerFactory

    /** On-demand WorkManager init (startup provider removed in manifest), with Hilt worker injection. */
    override val workManagerConfiguration: Configuration
        get() = Configuration
            .Builder()
            .setWorkerFactory(workerFactory)
            .build()
}
