package com.securevault.next.core.crypto

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class TotpEngineTest {

    // The RFC 6238 Appendix B test seed: the ASCII bytes of the string
    // "12345678901234567890", Base32-encoded (RFC 4648). Appendix B uses
    // algorithm-specific seed lengths (20/32/64 bytes for SHA1/SHA256/SHA512,
    // each spelling the same "12345678901234567890" digits), so the correct
    // key material for a given algorithm is its own decoded Base32 string.
    private val secretBase32 = "GEZDGNBVGY3TQOJQGEZDGNBVGY3TQOJQ"

    private val secretBase32sha256 =
        "GEZDGNBVGY3TQOJQGEZDGNBVGY3TQOJQGEZDGNBVGY3TQOJQGEZA===="

    private val secretBase32sha512 =
        "GEZDGNBVGY3TQOJQGEZDGNBVGY3TQOJQGEZDGNBVGY3TQOJQGEZDGNBVGY3TQOJQ" +
            "GEZDGNBVGY3TQOJQGEZDGNBVGY3TQOJQGEZDGNA="

    // --- RFC 6238 Appendix B vectors (8 digits, period 30s) ---

    @Test
    fun `matches RFC 6238 SHA1 vector at time 59`() {
        assertThat(TotpEngine.generateTotp(secretBase32, timeMillis = 59_000, digits = 8))
            .isEqualTo("94287082")
    }

    @Test
    fun `matches RFC 6238 SHA256 vector at time 59`() {
        assertThat(TotpEngine.generateTotp(secretBase32sha256, timeMillis = 59_000, digits = 8, algorithm = "SHA256"))
            .isEqualTo("46119246")
    }

    @Test
    fun `matches RFC 6238 SHA512 vector at time 59`() {
        assertThat(TotpEngine.generateTotp(secretBase32sha512, timeMillis = 59_000, digits = 8, algorithm = "SHA512"))
            .isEqualTo("90693936")
    }

    @Test
    fun `matches RFC 6238 SHA1 vector at time 1111111109`() {
        assertThat(TotpEngine.generateTotp(secretBase32, timeMillis = 1_111_111_109_000, digits = 8))
            .isEqualTo("07081804")
    }

    @Test
    fun `matches RFC 6238 SHA256 vector at time 1111111109`() {
        assertThat(
            TotpEngine.generateTotp(
                secretBase32sha256,
                timeMillis = 1_111_111_109_000,
                digits = 8,
                algorithm = "SHA256",
            )
        ).isEqualTo("68084774")
    }

    @Test
    fun `matches RFC 6238 SHA512 vector at time 1111111109`() {
        assertThat(
            TotpEngine.generateTotp(
                secretBase32sha512,
                timeMillis = 1_111_111_109_000,
                digits = 8,
                algorithm = "SHA512",
            )
        ).isEqualTo("25091201")
    }

    @Test
    fun `matches RFC 6238 SHA1 vector at time 1111111111`() {
        assertThat(TotpEngine.generateTotp(secretBase32, timeMillis = 1_111_111_111_000, digits = 8))
            .isEqualTo("14050471")
    }

    // --- 6-digit behavior ---

    @Test
    fun `6-digit SHA1 code is the 8-digit code reduced to the low six digits`() {
        val eight = TotpEngine.generateTotp(secretBase32, timeMillis = 59_000, digits = 8)
        val six = TotpEngine.generateTotp(secretBase32, timeMillis = 59_000, digits = 6)

        assertThat(six).isEqualTo(eight.takeLast(6))
        assertThat(six).isEqualTo("287082")
    }

    // --- period / remaining-seconds ---

    @Test
    fun `remaining seconds honors a custom period`() {
        // Within the first period (period 60, t=30s) → 30 seconds remain.
        assertThat(TotpEngine.getRemainingSeconds(timeMillis = 30_000, periodSeconds = 60)).isEqualTo(30)
        // Default 30s period: t=30s marks the boundary → 30 remain in the next window.
        assertThat(TotpEngine.getRemainingSeconds(timeMillis = 30_000, periodSeconds = 30)).isEqualTo(30)
    }

    @Test
    fun `a custom period changes the generated code`() {
        val standard = TotpEngine.generateTotp(secretBase32, timeMillis = 59_000, periodSeconds = 30)
        val long = TotpEngine.generateTotp(secretBase32, timeMillis = 59_000, periodSeconds = 60)

        assertThat(standard).isNotEqualTo(long)
    }

    // --- input validation ---

    @Test
    fun `algorithm is case-insensitive`() {
        assertThat(
            TotpEngine.generateTotp(
                secretBase32sha256,
                timeMillis = 59_000,
                digits = 8,
                algorithm = "hmacsha256",
            ),
        )
            .isEqualTo("46119246")
    }

    @Test
    fun `unsupported algorithm is rejected`() {
        var threw = false
        try {
            TotpEngine.generateTotp(secretBase32, algorithm = "MD5")
        } catch (_: IllegalArgumentException) {
            threw = true
        }
        assertThat(threw).isTrue()
    }

    @Test
    fun `invalid base32 is rejected`() {
        var threw = false
        try {
            TotpEngine.generateTotp("!!!!!!!!!!!!")
        } catch (_: IllegalArgumentException) {
            threw = true
        }
        assertThat(threw).isTrue()
    }

    @Test
    fun `base32 length that cannot align to bytes is rejected`() {
        var threw = false
        try {
            TotpEngine.generateTotp("GEZDGNB") // 7 chars → remainder 3, invalid length
        } catch (_: IllegalArgumentException) {
            threw = true
        }
        assertThat(threw).isTrue()
    }

    @Test
    fun `digits outside the supported range is rejected`() {
        var threw = false
        try {
            TotpEngine.generateTotp(secretBase32, digits = 4)
        } catch (_: IllegalArgumentException) {
            threw = true
        }
        assertThat(threw).isTrue()
    }

    @Test
    fun `period of zero or less is rejected`() {
        var threw = false
        try {
            TotpEngine.generateTotp(secretBase32, periodSeconds = 0)
        } catch (_: IllegalArgumentException) {
            threw = true
        }
        assertThat(threw).isTrue()
    }
}
