package com.securevault.next.core.crypto.integrity

import com.google.common.truth.Truth.assertThat
import com.securevault.next.core.common.result.Result
import com.securevault.next.core.common.result.isFailure
import com.securevault.next.core.crypto.error.CryptoError
import com.securevault.next.core.crypto.random.SystemSecureRandomProvider
import org.junit.Test

class HmacSha256IntegrityVerifierTest {

    private val verifier = HmacSha256IntegrityVerifier()

    private fun randomMacKey() = SystemSecureRandomProvider().nextBytes(32)

    @Test
    fun `verifyMac succeeds when the data has not changed`() {
        val macKey = randomMacKey()
        val data = "vault metadata payload".toByteArray()
        val mac = verifier.computeMac(macKey.copy(), data)
        check(mac is Result.Success)

        val result = verifier.verifyMac(macKey, data, mac.value, subject = "test")

        assertThat(result.isFailure).isFalse()
    }

    @Test
    fun `verifyMac fails when the data has been tampered with`() {
        val macKey = randomMacKey()
        val originalData = "vault metadata payload".toByteArray()
        val mac = verifier.computeMac(macKey.copy(), originalData)
        check(mac is Result.Success)

        val tamperedData = "vault METADATA payload".toByteArray()
        val result = verifier.verifyMac(macKey, tamperedData, mac.value, subject = "test")

        check(result is Result.Failure)
        assertThat(result.error).isInstanceOf(CryptoError.IntegrityCheckFailed::class.java)
    }

    @Test
    fun `verifyMac fails when the wrong key is used`() {
        val correctKey = randomMacKey()
        val wrongKey = randomMacKey()
        val data = "vault metadata payload".toByteArray()
        val mac = verifier.computeMac(correctKey, data)
        check(mac is Result.Success)

        val result = verifier.verifyMac(wrongKey, data, mac.value, subject = "test")

        assertThat(result.isFailure).isTrue()
    }

    @Test
    fun `computeMac is deterministic for the same key and data`() {
        val macKey = randomMacKey()
        val data = "same payload".toByteArray()

        val first = verifier.computeMac(macKey.copy(), data)
        val second = verifier.computeMac(macKey, data)
        check(first is Result.Success && second is Result.Success)

        assertThat(first.value).isEqualTo(second.value)
    }

    @Test
    fun `identifies which subject failed integrity verification`() {
        val macKey = randomMacKey()
        val mac = verifier.computeMac(macKey.copy(), "a".toByteArray())
        check(mac is Result.Success)

        val result = verifier.verifyMac(macKey, "b".toByteArray(), mac.value, subject = "kdf parameters")

        check(result is Result.Failure)
        val error = result.error as CryptoError.IntegrityCheckFailed
        assertThat(error.subject).isEqualTo("kdf parameters")
    }
}
