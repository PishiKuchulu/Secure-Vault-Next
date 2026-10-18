package com.securevault.next.core.crypto.format

import com.securevault.next.core.domain.model.valueobject.EncryptedField
import java.io.ByteArrayOutputStream
import java.io.DataOutputStream
import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.descriptors.buildClassSerialDescriptor
import kotlinx.serialization.descriptors.element
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.encoding.decodeStructure
import kotlinx.serialization.encoding.encodeStructure

/**
 * Serializer for the envelope's embedded [EncryptedField] (the wrapped
 * vault key). The envelope needs to persist this value inside its JSON,
 * but [EncryptedField] itself is intentionally left un-serialized in this
 * milestone (field encryption is a separate concern). This serializer
 * encodes it as a JSON object of its three plain string fields without
 * modifying the [EncryptedField] type.
 */
object EncryptedFieldSerializer : KSerializer<EncryptedField> {
    override val descriptor: SerialDescriptor = buildClassSerialDescriptor("EncryptedField") {
        element<String>("ciphertext")
        element<String>("nonce")
        element<String>("algorithm")
    }

    override fun serialize(encoder: Encoder, value: EncryptedField) {
        encoder.encodeStructure(descriptor) {
            encodeStringElement(descriptor, 0, value.ciphertext)
            encodeStringElement(descriptor, 1, value.nonce)
            encodeStringElement(descriptor, 2, value.algorithm)
        }
    }

    override fun deserialize(decoder: Decoder): EncryptedField =
        decoder.decodeStructure(descriptor) {
            var ciphertext = ""
            var nonce = ""
            var algorithm = EncryptedField.DEFAULT_ALGORITHM
            while (true) {
                when (val index = decodeElementIndex(descriptor)) {
                    0 -> ciphertext = decodeStringElement(descriptor, 0)
                    1 -> nonce = decodeStringElement(descriptor, 1)
                    2 -> algorithm = decodeStringElement(descriptor, 2)
                    else -> break
                }
            }
            EncryptedField(ciphertext = ciphertext, nonce = nonce, algorithm = algorithm)
        }
}

/**
 * Which cipher protects [EncryptedVaultEnvelope.encryptedContent].
 * Currently always AES-256-GCM; a distinct type from a bare string for
 * symmetry with [KdfMetadata] and room to grow.
 */
@Serializable
data class CipherMetadata(
    val algorithm: String = EncryptedField.DEFAULT_ALGORITHM,
) {
    init {
        require(algorithm.isNotBlank()) { "CipherMetadata.algorithm must not be blank" }
    }
}

/**
 * The vault key, wrapped (encrypted) under the vault's master key. This
 * is the *only* place the vault key exists at rest — see the key
 * hierarchy documentation on `core.crypto.key.Keys.kt` for why the
 * indirection through a separately-wrapped vault key exists at all.
 */
@Serializable
data class EncryptionMetadata(
    @Serializable(with = EncryptedFieldSerializer::class)
    val wrappedVaultKey: EncryptedField,
)

/**
 * The HMAC-SHA256 protecting the rest of this envelope's unencrypted
 * metadata against tampering — see `core.crypto.integrity.IntegrityVerifier`
 * for the full threat model this defends against (primarily a KDF
 * parameter downgrade attack).
 */
@Serializable
data class IntegrityMetadata(
    /** Base64-encoded HMAC-SHA256 output. */
    val mac: String,
) {
    init {
        require(mac.isNotBlank()) { "IntegrityMetadata.mac must not be blank" }
    }
}

/**
 * The complete versioned, encrypted vault container — everything needed
 * to store a vault at rest and later open it again.
 *
 * [futureCompatibilityFields] exists purely for forward compatibility:
 * if a *newer* build of the app introduces a new envelope field, an
 * *older* build that doesn't know about it yet can still round-trip the
 * vault (read it, and if it writes the vault back out — e.g. after
 * editing an item — re-emit whatever fields it didn't understand)
 * instead of silently discarding data it doesn't recognize.
 */
@Serializable
data class EncryptedVaultEnvelope(
    val formatVersion: VaultFormatVersion,
    val kdfMetadata: KdfMetadata,
    val cipherMetadata: CipherMetadata,
    val encryptionMetadata: EncryptionMetadata,
    val integrityMetadata: IntegrityMetadata,
    /** The vault's serialized content (see `core:vault`, Milestone 4), encrypted under the unwrapped vault key. */
    @Serializable(with = EncryptedFieldSerializer::class)
    val encryptedContent: EncryptedField,
    val futureCompatibilityFields: Map<String, String> = emptyMap(),
) {
    /**
     * The exact byte sequence that [integrityMetadata]'s MAC is computed
     * over — every field that could be tampered with to weaken or
     * misdirect decryption *except* the MAC itself. Both computing and
     * verifying the MAC must call this same function so the two can
     * never accidentally drift apart.
     *
     * Every field is written length-prefixed (a 4-byte big-endian byte
     * count immediately before the field's UTF-8 bytes) rather than
     * joined with a delimiter character like `;` or `=`. A
     * delimiter-based scheme is only safe as long as no field's content
     * can ever contain the delimiter itself; [futureCompatibilityFields]
     * is an open `Map<String, String>` with no such restriction, so a
     * value containing `;` or `=` could shift where one field ends and
     * the next begins, letting two different metadata sets serialize to
     * an identical byte sequence — which would defeat the entire point
     * of MAC-protecting this data. Length-prefixing is immune to this
     * regardless of what any field contains, because the boundary
     * between fields is determined by explicit byte counts rather than
     * by scanning for a character that might also appear inside a
     * value.
     */
    fun canonicalIntegrityPayload(): ByteArray {
        val buffer = ByteArrayOutputStream()
        val out = DataOutputStream(buffer)

        fun writeField(name: String, value: String) {
            out.writeUTF(name)
            val bytes = value.toByteArray(Charsets.UTF_8)
            out.writeInt(bytes.size)
            out.write(bytes)
        }

        writeField("formatVersion", formatVersion.value.toString())
        writeField("kdf.algorithm", kdfMetadata.algorithm.name)
        writeField("kdf.parameterVersion", kdfMetadata.parameterVersion.toString())
        writeField("kdf.memoryCostKib", kdfMetadata.memoryCostKib.toString())
        writeField("kdf.iterations", kdfMetadata.iterations.toString())
        writeField("kdf.parallelism", kdfMetadata.parallelism.toString())
        writeField("kdf.outputLengthBytes", kdfMetadata.outputLengthBytes.toString())
        writeField("kdf.salt", kdfMetadata.salt)
        writeField("cipher.algorithm", cipherMetadata.algorithm)
        writeField("encryption.wrappedVaultKey.ciphertext", encryptionMetadata.wrappedVaultKey.ciphertext)
        writeField("encryption.wrappedVaultKey.nonce", encryptionMetadata.wrappedVaultKey.nonce)

        // Sorted for determinism (Map iteration order is not guaranteed),
        // and the entry count is written first so the reader — here,
        // nothing actually parses this back, but a future one could —
        // knows how many follow without relying on a sentinel value.
        val sortedFuture = futureCompatibilityFields.toSortedMap()
        out.writeInt(sortedFuture.size)
        sortedFuture.forEach { (key, value) ->
            writeField("future.$key", value)
        }

        out.flush()
        return buffer.toByteArray()
    }
}
