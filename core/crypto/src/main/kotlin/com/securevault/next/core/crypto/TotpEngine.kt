package com.securevault.next.core.crypto

import java.nio.ByteBuffer
import java.security.GeneralSecurityException
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec
import kotlin.math.pow

/**
 * RFC 6238 TOTP (Time-based One-Time Password) implementation.
 *
 * The shared secret is Base32-encoded per RFC 4648 (the convention used
 * by Google Authenticator and most otpauth provisioning URIs), so the
 * secret string must be decoded from Base32 — not Base64 — before being
 * used as the HMAC key. [algorithm] is one of the RFC 6238-mandated
 * SHA-family names: SHA1 (default, 160-bit), SHA256 or SHA512.
 */
object TotpEngine {
    /**
     * Generates the current TOTP for [secretBase32] at [timeMillis].
     *
     * @param secretBase32 the Base32-encoded shared secret (RFC 4648)
     * @param timeMillis the reference time; defaults to the system clock
     * @param digits the number of OTP digits (RFC 6238 allows 6–8)
     * @param periodSeconds the time-step size in seconds (RFC 6238 default 30)
     * @param algorithm one of "SHA1", "SHA256", "SHA512" (default "SHA1")
     * @throws IllegalArgumentException if [secretBase32] is not valid
     *   Base32, [algorithm] is unsupported, or the digits/period are invalid
     * @throws GeneralSecurityException if the underlying HMAC fails
     */
    fun generateTotp(
        secretBase32: String,
        timeMillis: Long = System.currentTimeMillis(),
        digits: Int = 6,
        periodSeconds: Int = 30,
        algorithm: String = DEFAULT_ALGORITHM
    ): String {
        require(digits in MIN_DIGITS..MAX_DIGITS) {
            "TotpEngine requires digits in $MIN_DIGITS..$MAX_DIGITS, was $digits"
        }
        require(periodSeconds > 0) { "TotpEngine requires a positive period, was $periodSeconds" }

        val key = Base32.decode(secretBase32)
        val macName = normalizeAlgorithm(algorithm)

        val counter = timeMillis / 1000 / periodSeconds
        val message = ByteBuffer.allocate(8).putLong(counter).array()

        // Finding L9: the decoded key bytes are wiped in the finally block
        // once the HMAC has consumed them, so the raw secret does not
        // linger in memory after the OTP is computed.
        try {
            val mac = Mac.getInstance(macName)
            mac.init(SecretKeySpec(key, macName))
            val hash = mac.doFinal(message)

            val offset = hash[hash.size - 1].toInt() and 0x0f
            val binary =
                ((hash[offset].toInt() and 0x7f) shl 24) or
                    ((hash[offset + 1].toInt() and 0xff) shl 16) or
                    ((hash[offset + 2].toInt() and 0xff) shl 8) or
                    (hash[offset + 3].toInt() and 0xff)

            val otp = binary % 10.0.pow(digits.toDouble()).toInt()
            return otp.toString().padStart(digits, '0')
        } finally {
            key.fill(0)
        }
    }

    /** Returns the seconds remaining until the end of the current [periodSeconds] window. */
    fun getRemainingSeconds(timeMillis: Long = System.currentTimeMillis(), periodSeconds: Int = 30): Int =
        periodSeconds - ((timeMillis / 1000) % periodSeconds).toInt()

    /**
     * Maps the provisioning-URI algorithm names ("SHA1"/"SHA256"/"SHA512")
     * to their standard `Hmac` transformation names. Throws
     * [IllegalArgumentException] for anything else, since a TOTP secret
     * must only ever be used with the RFC 6238 SHA-family algorithms.
     */
    private fun normalizeAlgorithm(algorithm: String): String = when (algorithm.uppercase()) {
        "SHA1", "HMACSHA1" -> "HmacSHA1"
        "SHA256", "HMACSHA256" -> "HmacSHA256"
        "SHA512", "HMACSHA512" -> "HmacSHA512"
        else -> throw IllegalArgumentException(
            "Unsupported TOTP algorithm '$algorithm'; expected SHA1, SHA256 or SHA512"
        )
    }

    private const val DEFAULT_ALGORITHM = "SHA1"
    private const val MIN_DIGITS = 6
    private const val MAX_DIGITS = 8
}

/**
 * A minimal, dependency-free RFC 4648 Base32 decoder.
 *
 * Handles upper-case (and, leniently, lower-case) input, optional `=`
 * padding, and rejects characters outside the standard alphabet. The
 * encoded input is validated for length and trailing-remainder validity
 * so an ill-formed secret fails fast instead of silently truncating.
 */
private object Base32 {
    private const val ALPHABET = "ABCDEFGHIJKLMNOPQRSTUVWXYZ234567"

    // ALPHABET.max() == 'Z' (code 90); 128 comfortably covers any ASCII input.
    private val LOOKUP: IntArray =
        IntArray(128) { -1 }.also { table ->
            ALPHABET.forEachIndexed { index, char -> table[char.code] = index }
        }

    /** Decodes [encoded] (with or without `=` padding) into its bytes. */
    fun decode(encoded: String): ByteArray {
        val cleaned =
            encoded
                .filterNot { it == '=' }
                .uppercase()

        require(cleaned.isNotEmpty()) { "Base32 input must not be empty" }

        // RFC 4648 §3.4: the final quantum holds 2, 4, 5 or 7 symbols;
        // any other remainder (1, 3 or 6) cannot align to whole bytes.
        val remainder = cleaned.length % 8
        require(remainder == 0 || remainder == 2 || remainder == 4 || remainder == 5 || remainder == 7) {
            "Base32 input has an invalid length: ${cleaned.length}"
        }
        require(cleaned.all { it.code < LOOKUP.size && LOOKUP[it.code] >= 0 }) {
            "Base32 input contains characters outside the [$ALPHABET] alphabet"
        }

        val bytes = ArrayList<Byte>(cleaned.length * 5 / 8)
        var buffer = 0
        var bits = 0

        for (char in cleaned) {
            buffer = buffer shl 5 or LOOKUP[char.code]
            bits += 5
            if (bits >= 8) {
                bits -= 8
                bytes.add(((buffer shr bits) and 0xff).toByte())
            }
        }

        // RFC 4648 §3.4: unused trailing bits must be zero. Rejecting a
        // non-zero remainder prevents malformed secrets from silently
        // producing a different key than the caller intended.
        if (bits > 0 && (buffer and ((1 shl bits) - 1)) != 0) {
            throw IllegalArgumentException("Base32 input has non-zero unused trailing bits")
        }

        return bytes.toByteArray()
    }
}
