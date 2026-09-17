package com.example.expensemanager.backup

import android.content.Context
import androidx.core.content.edit
import com.example.expensemanager.data.ExpenseDatabase
import dagger.Lazy
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.conflate
import kotlinx.coroutines.launch
import java.util.concurrent.atomic.AtomicBoolean
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Persistent bookkeeping for automatic backup: whether it is on, and enough state to answer
 * "does the data on disk still match the last backup we wrote?".
 *
 * Backed by [android.content.SharedPreferences], not the app database: the worker reads this
 * before deciding whether to act, and unlocking the encrypted database for a boolean would
 * undo the saving that early exit is there to make.
 */
@Singleton
internal class AutoBackupStore
    @Inject
    constructor(
        @ApplicationContext context: Context,
        // Lazy: observing the database must not be what forces it open. The first real
        // data access (or the flow below, once collected) opens it instead.
        private val database: Lazy<ExpenseDatabase>
    ) {
        private val prefs = context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

        /** Guards [startObservingDatabase] so repeated calls attach only one collector. */
        private val observingDatabase = AtomicBoolean(false)

        private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

        /**
         * Process-lifetime scope for work that must finish even if the caller goes away (e.g.
         * enqueueing into WorkManager): a `viewModelScope` would be cancelled on leaving
         * Settings, losing the schedule.
         */
        internal val backgroundScope: CoroutineScope get() = scope

        private val _isEnabled = MutableStateFlow(prefs.getBoolean(KEY_ENABLED, false))
        internal val isEnabled: StateFlow<Boolean> = _isEnabled.asStateFlow()

        internal fun setEnabled(enabled: Boolean) {
            if (_isEnabled.value == enabled) return
            prefs.edit { putBoolean(KEY_ENABLED, enabled) }
            _isEnabled.value = enabled
        }

        /**
         * Flags the data dirty on any write to a backed-up table. Only sets a cheap bit — the
         * worker does the expensive comparison later. [conflate] collapses write bursts (an
         * import, a bulk delete) into a single flag; idempotent, so callers need not coordinate.
         */
        internal fun startObservingDatabase() {
            if (!observingDatabase.compareAndSet(false, true)) return
            scope.launch {
                database
                    .get()
                    .invalidationTracker
                    .createFlow(*OBSERVED_TABLES, emitInitialState = false)
                    .conflate()
                    .collect { markDirty() }
            }
        }

        internal fun state(): State =
            State(
                dirty = prefs.getBoolean(KEY_DIRTY, false),
                contentSignature = prefs.getString(KEY_SIGNATURE, null),
                lastBackupUri = prefs.getString(KEY_LAST_URI, null),
                lastVerifiedAt = prefs.getLong(KEY_VERIFIED_AT, 0L)
            )

        internal fun markDirty() {
            // Checked first so a burst of writes does not turn into a burst of disk commits.
            if (prefs.getBoolean(KEY_DIRTY, false)) return
            prefs.edit { putBoolean(KEY_DIRTY, true) }
        }

        /**
         * Records that the data was inspected at [at] and needed no new backup. Clears the
         * dirty bit without touching the signature, so the next run can still short-circuit.
         */
        internal fun recordVerified(at: Long) {
            prefs.edit {
                putBoolean(KEY_DIRTY, false)
                putLong(KEY_VERIFIED_AT, at)
            }
        }

        /** Records a backup that was actually written, so the next run can compare against it. */
        internal fun recordBackup(
            contentSignature: String,
            uri: String,
            at: Long
        ) {
            prefs.edit {
                putBoolean(KEY_DIRTY, false)
                putString(KEY_SIGNATURE, contentSignature)
                putString(KEY_LAST_URI, uri)
                putLong(KEY_VERIFIED_AT, at)
            }
        }

        /**
         * Forgets which file was last written and what it contained, leaving the data marked
         * dirty. Used when auto-backup is switched on: the previous file may be long deleted,
         * so the next run must write a fresh one rather than trust stale bookkeeping.
         */
        internal fun resetChangeTracking() {
            prefs.edit {
                putBoolean(KEY_DIRTY, true)
                remove(KEY_SIGNATURE)
                remove(KEY_LAST_URI)
                putLong(KEY_VERIFIED_AT, 0L)
            }
        }

        /** Snapshot of the change-detection bookkeeping, read in one go by the worker. */
        internal data class State(
            val dirty: Boolean,
            val contentSignature: String?,
            val lastBackupUri: String?,
            val lastVerifiedAt: Long
        )

        private companion object {
            /** Exactly the tables whose contents end up in a backup file. */
            val OBSERVED_TABLES = arrayOf(
                "expenses",
                "monthly_budgets",
                "budget_excluded_categories",
                "categories"
            )

            const val PREFS_NAME = "auto_backup_state"
            const val KEY_ENABLED = "enabled"
            const val KEY_DIRTY = "dirty"
            const val KEY_SIGNATURE = "content_signature"
            const val KEY_LAST_URI = "last_backup_uri"
            const val KEY_VERIFIED_AT = "last_verified_at"
        }
    }
