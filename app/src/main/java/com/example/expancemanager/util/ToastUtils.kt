package com.example.expancemanager.util

import android.content.Context
import android.widget.Toast

/**
 * Shows a long-duration toast. Use for important feedback (e.g. export/import results).
 */
fun Context.showToast(message: String) {
    Toast.makeText(this, message, Toast.LENGTH_LONG).show()
}

/**
 * Shows a short-duration toast. Use for quick acknowledgments (e.g. budget saved).
 */
fun Context.showShortToast(message: String) {
    Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
}
