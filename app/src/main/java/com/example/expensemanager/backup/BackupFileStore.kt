package com.example.expensemanager.backup

import android.content.ContentResolver
import android.content.ContentUris
import android.content.ContentValues
import android.content.Context
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import androidx.annotation.RequiresApi
import androidx.core.content.ContextCompat
import androidx.core.net.toUri
import com.example.expensemanager.util.BackupManager
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Creates, publishes and prunes the auto-backup files in the public
 * `Download/Expense Manager` folder.
 *
 * Two storage worlds are hidden behind one API: API 29+ goes through MediaStore (no
 * permission needed, and the file is created `IS_PENDING` so a half-written backup is never
 * visible to other apps), while API 26-28 writes an ordinary file and needs the legacy
 * storage grant. The public Downloads folder is deliberate - backups must outlive
 * uninstalling the app, which app-private storage would not.
 *
 * Every method swallows its failures and reports them as `null`/`false`/no-op rather than
 * throwing: a backup that cannot be written is the worker's business to retry, and must
 * never surface as a crash.
 */
@Singleton
internal class BackupFileStore
    @Inject
    constructor(
        @ApplicationContext private val context: Context,
        private val contentResolver: ContentResolver
    ) {
        /** Whether a backup could be written right now, ignoring whether anything needs writing. */
        internal fun isWritable(): Boolean =
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                true
            } else {
                Environment.getExternalStorageState() == Environment.MEDIA_MOUNTED && hasLegacyWritePermission()
            }

        /** Always true on API 29+, where writing via MediaStore needs no permission. */
        internal fun hasLegacyWritePermission(): Boolean =
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q ||
                ContextCompat.checkSelfPermission(
                    context,
                    android.Manifest.permission.WRITE_EXTERNAL_STORAGE
                ) == PackageManager.PERMISSION_GRANTED

        /**
         * Reserves a file to write into, or `null` if one could not be created. The returned
         * [Target] must be handed to exactly one of [publish] or [discard].
         */
        internal fun create(fileName: String): Target? =
            runCatching {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    createViaMediaStore(fileName)
                } else {
                    createLegacyFile(fileName)
                }
            }.getOrNull()

        /** Makes a fully written file visible to other apps. */
        internal fun publish(target: Target) {
            if (!target.pending || Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) return
            runCatching {
                contentResolver.update(
                    target.uri,
                    ContentValues().apply { put(MediaStore.MediaColumns.IS_PENDING, 0) },
                    null,
                    null
                )
            }
        }

        /** Removes a file whose write failed, so a partial backup is never left behind. */
        internal fun discard(target: Target) {
            runCatching {
                if (target.uri.scheme == ContentResolver.SCHEME_FILE) {
                    target.uri.path?.let { File(it).delete() }
                } else {
                    contentResolver.delete(target.uri, null, null)
                }
            }
        }

        /**
         * Whether the last backup is still there. The user can delete files from Downloads at
         * any time, so a matching content signature alone is not enough to skip a backup.
         */
        internal fun exists(uriString: String?): Boolean {
            val uri = uriString?.takeIf { it.isNotEmpty() }?.toUri() ?: return false
            return runCatching {
                if (uri.scheme == ContentResolver.SCHEME_FILE) {
                    uri.path?.let { File(it).exists() } ?: false
                } else {
                    contentResolver
                        .query(uri, arrayOf(MediaStore.MediaColumns._ID), null, null, null)
                        ?.use { cursor -> cursor.moveToFirst() } ?: false
                }
            }.getOrDefault(false)
        }

        /** Keeps the [keep] newest auto-backups and deletes the rest. */
        internal fun prune(keep: Int) {
            runCatching {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    pruneViaMediaStore(keep)
                } else {
                    pruneLegacyFiles(keep)
                }
            }
        }

        @RequiresApi(Build.VERSION_CODES.Q)
        private fun createViaMediaStore(fileName: String): Target? {
            val values = ContentValues().apply {
                put(MediaStore.MediaColumns.DISPLAY_NAME, fileName)
                put(MediaStore.MediaColumns.MIME_TYPE, MIME_TYPE)
                put(MediaStore.MediaColumns.RELATIVE_PATH, RELATIVE_PATH)
                // Hides the file until publish(); readers never see a truncated backup.
                put(MediaStore.MediaColumns.IS_PENDING, 1)
            }
            val uri = contentResolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, values) ?: return null
            return Target(uri, pending = true)
        }

        private fun createLegacyFile(fileName: String): Target? {
            val directory = File(
                Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS),
                BACKUP_DIRECTORY_NAME
            )
            if (!directory.isDirectory && !directory.mkdirs()) return null
            return Target(Uri.fromFile(File(directory, fileName)), pending = false)
        }

        @RequiresApi(Build.VERSION_CODES.Q)
        private fun pruneViaMediaStore(keep: Int) {
            // Matches both spellings of the folder: MediaStore has been inconsistent about
            // whether RELATIVE_PATH carries a trailing slash across versions.
            val selection = "${MediaStore.MediaColumns.RELATIVE_PATH} IN (?, ?) AND " +
                "${MediaStore.MediaColumns.DISPLAY_NAME} LIKE ?"
            val selectionArgs = arrayOf(
                RELATIVE_PATH,
                RELATIVE_PATH.trimEnd('/'),
                "${BackupManager.AUTO_BACKUP_FILE_PREFIX}%"
            )
            contentResolver.query(
                MediaStore.Downloads.EXTERNAL_CONTENT_URI,
                arrayOf(MediaStore.MediaColumns._ID),
                selection,
                selectionArgs,
                "${MediaStore.MediaColumns.DATE_MODIFIED} DESC, ${MediaStore.MediaColumns._ID} DESC"
            )?.use { cursor ->
                val idColumn = cursor.getColumnIndexOrThrow(MediaStore.MediaColumns._ID)
                var index = 0
                while (cursor.moveToNext()) {
                    if (index++ < keep) continue
                    val uri = ContentUris.withAppendedId(
                        MediaStore.Downloads.EXTERNAL_CONTENT_URI,
                        cursor.getLong(idColumn)
                    )
                    runCatching { contentResolver.delete(uri, null, null) }
                }
            }
        }

        private fun pruneLegacyFiles(keep: Int) {
            val directory = File(
                Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS),
                BACKUP_DIRECTORY_NAME
            )
            directory
                .listFiles { file ->
                    // Prefix check keeps the sweep off manual exports sharing this folder.
                    file.isFile &&
                        file.name.startsWith(BackupManager.AUTO_BACKUP_FILE_PREFIX) &&
                        file.name.endsWith(FILE_EXTENSION)
                }
                .orEmpty()
                .sortedByDescending { it.lastModified() }
                .drop(keep)
                .forEach { it.delete() }
        }

        /** A reserved destination for one backup file. */
        internal data class Target(
            val uri: Uri,
            val pending: Boolean
        )

        companion object {
            const val BACKUP_DIRECTORY_NAME = "Expense Manager"

            /** Human-readable form shown in Settings, e.g. `Download/Expense Manager`. */
            val DISPLAY_LOCATION = "${Environment.DIRECTORY_DOWNLOADS}/$BACKUP_DIRECTORY_NAME"

            private const val MIME_TYPE = "application/json"
            private const val FILE_EXTENSION = ".json"
            private val RELATIVE_PATH = "$DISPLAY_LOCATION/"
        }
    }
