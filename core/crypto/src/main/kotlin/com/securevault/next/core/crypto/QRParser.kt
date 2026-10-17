package com.securevault.next.core.crypto

import android.net.Uri

/**
 * Parses otpauth:// URIs used in QR codes.
 *
 * Contract (Finding L10): returns `null` **only** for malformed input --
 * a URI that is not `otpauth://totp/...`, a missing or blank `secret`
 * parameter, or a present-but-invalid `digits`/`period`/`algorithm`
 * parameter. Optional parameters that are *absent* fall back to the RFC
 * 6238 defaults (SHA1, 6 digits, 30-second period); parameters that are
 * present but invalid are malformed, not defaulted. `null` therefore
 * always means "this is not a usable otpauth URI", never "a transient
 * parse failure".
 */
object QRParser {

    data class TotpConfig(
        val secret: String,
        val issuer: String? = null,
        val accountName: String? = null,
        val algorithm: String = "SHA1",
        val digits: Int = 6,
        val period: Int = 30
    )

    fun parseOtpAuthUri(uriString: String): TotpConfig? {
        return try {
            val uri = Uri.parse(uriString)
            if (uri.scheme != "otpauth" || uri.host != "totp") return null

            // Finding L10: an empty secret previously slipped past the
            // null check and produced an unusable config.
            val secret = uri.getQueryParameter("secret")?.takeIf { it.isNotBlank() } ?: return null
            val issuer = uri.getQueryParameter("issuer")?.takeIf { it.isNotBlank() }
            val accountName = uri.path?.removePrefix("/")?.takeIf { it.isNotBlank() }

            val algorithm = uri.getQueryParameter("algorithm")?.uppercase() ?: DEFAULT_ALGORITHM
            if (algorithm !in SUPPORTED_ALGORITHMS) return null

            // Absent -> RFC 6238 default; present but unparseable or out
            // of range -> malformed.
            val digits = uri.getQueryParameter("digits")?.let { raw ->
                raw.toIntOrNull()?.takeIf { it in MIN_DIGITS..MAX_DIGITS } ?: return null
            } ?: DEFAULT_DIGITS
            val period = uri.getQueryParameter("period")?.let { raw ->
                raw.toIntOrNull()?.takeIf { it > 0 } ?: return null
            } ?: DEFAULT_PERIOD

            TotpConfig(secret, issuer, accountName, algorithm, digits, period)
        } catch (e: Exception) {
            null
        }
    }

    private const val DEFAULT_ALGORITHM = "SHA1"
    private const val DEFAULT_DIGITS = 6
    private const val DEFAULT_PERIOD = 30
    private const val MIN_DIGITS = 6
    private const val MAX_DIGITS = 8
    private val SUPPORTED_ALGORITHMS = setOf("SHA1", "SHA256", "SHA512")
}
