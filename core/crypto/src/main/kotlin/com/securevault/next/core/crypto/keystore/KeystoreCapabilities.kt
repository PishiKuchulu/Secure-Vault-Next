package com.securevault.next.core.crypto.keystore

/**
 * What hardware security guarantees a Keystore-generated key actually
 * ended up with. Requesting StrongBox or hardware backing is a
 * best-effort request (Milestone 3 requirement: "Generate hardware-backed
 * keys whenever supported. Gracefully fallback when hardware support is
 * unavailable.") — this is how a caller finds out what it actually got,
 * e.g. to inform a security-status display.
 */
data class KeystoreCapabilities(val isInsideSecureHardware: Boolean, val isStrongBoxBacked: Boolean)
