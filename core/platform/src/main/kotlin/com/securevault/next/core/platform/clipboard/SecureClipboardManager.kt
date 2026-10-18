package com.securevault.next.core.platform.clipboard

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.os.Build
import android.os.Handler
import android.os.Looper

/**
 * Android-specific clipboard management with automatic clearing.
 *
 * Part 7: "Security Hardening... Clipboard clearing".
 */
class SecureClipboardManager(private val context: Context) {
    private val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
    private val handler = Handler(Looper.getMainLooper())
    private var clearRunnable: Runnable? = null

    /**
     * Copies [text] to clipboard and schedules a clear after [timeoutMillis].
     *
     * On Android 13+, it also marks the content as sensitive so it
     * isn't shown in the clipboard preview.
     */
    fun copyToClipboard(label: String, text: String, timeoutMillis: Long = 30_000L) {
        val clip = ClipData.newPlainText(label, text)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            clip.description.extras =
                android.os.PersistableBundle().apply {
                    if (Build.VERSION.SDK_INT >= 33) {
                        putBoolean("android.content.extra.IS_SENSITIVE", true)
                    }
                }
        }

        clipboard.setPrimaryClip(clip)

        // Cancel previous clear task if any
        clearRunnable?.let { handler.removeCallbacks(it) }

        val runnable = Runnable { clearClipboard() }
        clearRunnable = runnable
        handler.postDelayed(runnable, timeoutMillis)
    }

    /** Immediately clears the primary clip. */
    fun clearClipboard() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            clipboard.clearPrimaryClip()
        } else {
            clipboard.setPrimaryClip(ClipData.newPlainText("", ""))
        }
    }
}
