package com.securevault.next.core.crypto.format

import com.google.common.truth.Truth.assertThat
import com.securevault.next.core.crypto.kdf.KdfAlgorithm
import com.securevault.next.core.domain.model.valueobject.EncryptedField
import org.junit.Test

class EncryptedVaultEnvelopeIntegrityPayloadTest {

    private fun envelope(futureFields: Map<String, String>): EncryptedVaultEnvelope = EncryptedVaultEnvelope(
        formatVersion = VaultFormatVersion.CURRENT,
        kdfMetadata = KdfMetadata(
            algorithm = KdfAlgorithm.ARGON2ID,
            parameterVersion = 1,
            memoryCostKib = 65536,
            iterations = 3,
            parallelism = 4,
            outputLengthBytes = 64,
            salt = "c2FsdA==",
        ),
        cipherMetadata = CipherMetadata(),
        encryptionMetadata = EncryptionMetadata(
            wrappedVaultKey = EncryptedField(ciphertext = "Y2lwaGVy", nonce = "bm9uY2U="),
        ),
        integrityMetadata = IntegrityMetadata(mac = "cGxhY2Vob2xkZXI="),
        encryptedContent = EncryptedField(ciphertext = "Y29udGVudA==", nonce = "bm9uY2Uy"),
        futureCompatibilityFields = futureFields,
    )

    @Test
    fun `identical envelopes produce identical canonical payloads`() {
        val a = envelope(emptyMap()).canonicalIntegrityPayload()
        val b = envelope(emptyMap()).canonicalIntegrityPayload()

        assertThat(a).isEqualTo(b)
    }

    @Test
    fun `future fields with embedded delimiter characters do not collide with a differently-shaped field set`() {
        // This is the regression test for the Critical audit finding: the
        // old scheme joined fields with ';' and '=' as plain delimiters.
        // A single future field whose value contained those characters
        // could serialize identically to a completely different set of
        // fields. Length-prefixed encoding must keep these distinct no
        // matter what characters appear inside the values.
        val singleFieldWithEmbeddedDelimiters = envelope(
            mapOf("a" to "1;future.b=2"),
        ).canonicalIntegrityPayload()

        val twoSeparateFields = envelope(
            mapOf("a" to "1", "b" to "2"),
        ).canonicalIntegrityPayload()

        assertThat(singleFieldWithEmbeddedDelimiters).isNotEqualTo(twoSeparateFields)
    }

    @Test
    fun `future compatibility fields are order-independent (sorted deterministically)`() {
        val insertedAThenB = envelope(linkedMapOf("a" to "1", "b" to "2")).canonicalIntegrityPayload()
        val insertedBThenA = envelope(linkedMapOf("b" to "2", "a" to "1")).canonicalIntegrityPayload()

        assertThat(insertedAThenB).isEqualTo(insertedBThenA)
    }

    @Test
    fun `changing any single KDF field changes the canonical payload`() {
        val base = envelope(emptyMap())
        val tamperedIterations = base.copy(
            kdfMetadata = base.kdfMetadata.copy(iterations = base.kdfMetadata.iterations + 1),
        )
        val tamperedMemory = base.copy(
            kdfMetadata = base.kdfMetadata.copy(memoryCostKib = base.kdfMetadata.memoryCostKib + 1),
        )
        val tamperedSalt = base.copy(kdfMetadata = base.kdfMetadata.copy(salt = "ZGlmZmVyZW50"))

        val basePayload = base.canonicalIntegrityPayload()
        assertThat(tamperedIterations.canonicalIntegrityPayload()).isNotEqualTo(basePayload)
        assertThat(tamperedMemory.canonicalIntegrityPayload()).isNotEqualTo(basePayload)
        assertThat(tamperedSalt.canonicalIntegrityPayload()).isNotEqualTo(basePayload)
    }

    @Test
    fun `changing the format version changes the canonical payload`() {
        val base = envelope(emptyMap())
        val tampered = base.copy(formatVersion = VaultFormatVersion(base.formatVersion.value + 1))

        assertThat(tampered.canonicalIntegrityPayload()).isNotEqualTo(base.canonicalIntegrityPayload())
    }

    @Test
    fun `the encrypted content itself is not part of the canonical payload`() {
        // Deliberate: content is already self-authenticated by its own
        // AES-GCM tag (see AesGcmCipherEngineTest's tamper tests), so
        // duplicating that protection here isn't necessary. This test
        // documents that design choice explicitly rather than leaving it
        // implicit.
        val base = envelope(emptyMap())
        val differentContent = base.copy(
            encryptedContent = EncryptedField(ciphertext = "ZGlmZmVyZW50Y29udGVudA==", nonce = "bm9uY2Uz"),
        )

        assertThat(differentContent.canonicalIntegrityPayload()).isEqualTo(base.canonicalIntegrityPayload())
    }
}
