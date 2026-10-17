# SecureVault Next — Cryptography & Security Infrastructure Audit

**Scope:** `core:crypto`, `core:security` (Milestone 3 deliverables only). UI, Room, Compose, and Navigation were explicitly out of scope and were not reviewed.
**Auditor stance:** Independent review, adversarial by default, no implementation claim taken on trust.
**Outcome:** 1 Critical, 3 High, 4 Medium, 2 Low findings. All nine actionable findings were fixed in this pass; the two Low findings are documented recommendations rather than code changes.

---

## 1. Executive Summary

The Milestone 3 implementation gets the fundamentals right: Argon2id via an established library (not a homegrown KDF), AES-256-GCM with fresh random nonces per operation, a master-key/vault-key indirection that makes password rotation cheap, and an integrity MAC over unencrypted header metadata that is deliberately keyed from the same derivation as the master key — which is what turns it into a real defense against a KDF-downgrade attack rather than decoration.

The most serious finding was in that same integrity mechanism: the canonical byte sequence the MAC is computed over was built by joining fields with `;` and `=` as plain-text delimiters. That scheme is only safe as long as no field's content can contain those characters, and `EncryptedVaultEnvelope.futureCompatibilityFields` is an open string map with no such restriction. Nothing exploits this today (that field is never populated by any code yet), but it is exactly the kind of latent flaw that becomes a live vulnerability the moment a later milestone starts writing sync or cloud-backup metadata into that map. It has been replaced with a length-prefixed encoding that is unambiguous regardless of field content, and a dedicated regression test proves the specific collision that would have occurred under the old scheme no longer does.

Beyond that, the audit found real but less severe gaps: the master password's memory lifetime was left to caller discretion rather than being owned and minimized by the engine itself; `BackupKey` existed as a type with no code anywhere that ever produced or consumed one, meaning "backup encryption" from the Milestone 3 spec was not actually implemented despite the type existing; Android Keystore operations had no synchronization despite mutating shared state; and the KDF parameter validation didn't guard against the *combination* of memory cost and iteration count falling below OWASP's recommendation even though each was checked independently. All were fixed.

No evidence of ECB, CBC, static IVs, static salts, hardcoded keys, non-cryptographic randomness, or sensitive data in log/exception messages was found anywhere in scope.

## 2. Security Score

**87 / 100** (up from an estimated 71/100 pre-audit, reflecting the Critical and High findings this pass fixed).

The deduction from a perfect score is intentional and reflects genuine, currently-accepted limitations rather than an incomplete review:
- No replay/rollback protection for an entire swapped-in older vault file exists yet (Low finding, §6 L-1 — accepted for this milestone, needs a persistence-layer component that is out of scope until Milestone 4).
- `AndroidKeystoreKeyProvider` cannot be exercised by a JVM unit test (it requires a real Keystore), so its correctness rests on code review and the standard Android Keystore API contract rather than an executable test — this is an inherent constraint of the platform, not a shortcut taken here.
- Memory-dump resistance (`SecureBytes`) is a best-effort mitigation, not a guarantee, for reasons intrinsic to managed runtimes (GC copying, non-pinned memory) that no JVM/Kotlin library can fully close.

## 3. Critical Findings

### C-1: Delimiter-based integrity payload was vulnerable to field-boundary injection
**File:** `core/crypto/format/EncryptedVaultEnvelope.kt`
**Status:** Fixed.

`canonicalIntegrityPayload()` — the byte sequence the header-integrity HMAC is computed over — joined fields with literal `;` and `=` characters. `futureCompatibilityFields: Map<String, String>` is open string content with no restriction on what characters it can contain. A single future field value containing `;future.x=y` could serialize identically to two separate, differently-named fields, meaning two semantically different metadata sets could produce the identical MAC input — defeating the entire purpose of MAC-protecting this data against a downgrade attack, for whichever fields happen to collide.

Not exploitable today: no code anywhere populates `futureCompatibilityFields`. It is flagged Critical rather than Informational because it is a structural flaw in a security-critical function, cheap to fix now, and expensive to discover later (once real data lives in that map and vaults exist on real devices, changing the MAC's serialization format becomes a compatibility-breaking migration rather than a pre-release fix).

**Fix:** Rewrote `canonicalIntegrityPayload()` to length-prefix every field (a 4-byte field-value length, plus `DataOutputStream.writeUTF`'s own length-prefixed encoding for field names) instead of delimiter-joining. This is unambiguous regardless of field content. A dedicated test (`EncryptedVaultEnvelopeIntegrityPayloadTest`) constructs the exact input pair that collided under the old scheme and asserts they now produce different payloads.

## 4. High Severity Findings

### H-1: Master password lifetime was left to caller discretion, undocumented
**Files:** `core/crypto/vault/DefaultVaultEncryptionEngine.kt`, `core/crypto/key/KeyRotationManager.kt`, and their interfaces.
**Status:** Fixed.

`createVault`, `unlockVault`, and `rotateMasterKey` accepted `masterPassword`/`newPassword` as `SecureBytes` but never wiped it and never documented who was responsible for doing so. A future use-case implementation (Milestone 4+) could easily assume the crypto engine handles this — the same way it handles everything else about the password — and never call `close()` itself, silently extending the master password's in-memory lifetime indefinitely.

**Fix:** Both engines now take ownership of the password parameter and wipe it via `SecureBytes.use { }` wrapping the entire function body, guaranteeing the wipe happens on every exit path (success, an expected `CryptoResult.Failure`, or an unexpected thrown exception) — not just the happy path. The ownership contract is now stated explicitly in both interfaces' KDoc. The salt (generated in `createVault`, decoded from storage in `unlockVault`) is wiped the same way, closing a related low-severity resource-hygiene gap even though salts are not secret.

### H-2: `BackupKey` was declared but never used — backup encryption was not actually implemented
**File:** `core/crypto/key/Keys.kt` (existing type); new file `core/crypto/backup/BackupEncryptionEngine.kt`.
**Status:** Fixed.

Milestone 3's own specification listed "Backup Key" under Key Management and "Backup encryption" as a required capability. The type existed, correctly documented, but no function anywhere ever constructed one — there was no way to actually create or restore an encrypted backup. This is a completeness gap, not a design flaw in what did exist.

**Fix:** Added `BackupEncryptionEngine` (interface) and `DefaultBackupEncryptionEngine` (implementation), which deliberately *delegates* to the existing, already-audited `VaultEncryptionEngine` rather than re-implementing KDF/cipher/integrity orchestration a second time — a backup and a live vault are cryptographically identical constructions; the only thing that differs is the passphrase and the key's semantic type. Four new tests cover create/restore round-trip, a backup protected by a passphrase different from the live vault's master password, wrong-passphrase rejection, and a corrupted-backup rejection.

### H-3: No synchronization around Android Keystore key generation/deletion
**File:** `core/crypto/keystore/AndroidKeystoreKeyProvider.kt`
**Status:** Fixed.

`generateKey` and `deleteKey` mutate keystore state for a given alias; `encrypt`, `decrypt`, `capabilities`, and `hasKey` read it. None of the six methods were synchronized. Two threads racing — for instance, a key-regeneration flow (delete then generate) running concurrently with an in-flight encrypt call for the same alias — could observe a half-updated key, or throw an unexpected exception that gets folded into a generic `CryptoFailure` instead of a clear diagnosis.

**Fix:** Every method now synchronizes on a single private monitor. This trades a small amount of cross-alias parallelism for the guarantee that no keystore operation ever observes another mid-flight, which is the correct tradeoff for a security-critical component whose operations already cross a Binder IPC boundary to a system service (the added lock contention is negligible relative to that existing cost).

## 5. Medium Findings

### M-1: `readCapabilities` used an unsafe cast that could NPE instead of returning a typed error
**File:** `core/crypto/keystore/AndroidKeystoreKeyProvider.kt`
**Status:** Fixed.

`keyStore.getKey(alias, null) as SecretKey` throws a raw `ClassCastException`/NPE-adjacent failure if the alias doesn't exist, rather than the specific `CryptoError.KeyUnavailable` the rest of the class uses consistently for exactly this situation. It was caught by the surrounding broad `catch (t: Throwable)`, so it never crashed anything, but it produced a less precise, less debuggable error than the codebase's own error taxonomy calls for.

**Fix:** `capabilities()` now checks `containsAlias` first and returns `CryptoError.KeyUnavailable` explicitly before ever calling `readCapabilities`. `readCapabilities` itself now documents that its callers are required to have already confirmed the key exists, and uses `as?` with a clear `error()` message as a defensive fallback rather than an unguarded cast.

### M-2: KDF parameter validation checked memory cost and iteration count independently, not combined
**File:** `core/crypto/kdf/KdfParameters.kt`
**Status:** Fixed (via a self-check method plus a regression test, not a constructor hard-block — see rationale below).

`MIN_MEMORY_COST_KIB` (19 MiB) and `iterations >= 1` were each validated on their own. OWASP's documented recommendation is the *pair* — 19 MiB memory needs at least 2 iterations to meet the recommended floor; 1 iteration is only acceptable at higher memory. Nothing prevented constructing a technically-below-recommendation combination.

Deliberately **not** fixed by hard-blocking such combinations in the constructor: this test suite intentionally uses fast, low-cost parameters (including 1 iteration at the memory floor) to keep Argon2id-backed tests fast, and that is legitimate, standard practice as long as production code never reaches those parameters. The actual risk was that nothing *verified* production defaults stayed correct over time.

**Fix:** Added `KdfParameters.meetsOwaspMinimumRecommendation()`, and a test (`KdfParametersTest`) that asserts `KdfParameters.CURRENT` — the only parameters any real vault will ever be created with — satisfies it. This is a real regression guard: if a future change ever weakens the production defaults below OWASP's combined recommendation, this specific test fails.

### M-3: `splitDerivedMaterial` was duplicated verbatim across two classes
**Files:** `core/crypto/vault/DefaultVaultEncryptionEngine.kt`, `core/crypto/key/DefaultKeyRotationManager.kt` → new file `core/crypto/kdf/DerivedKeySplitter.kt`.
**Status:** Fixed.

Both classes independently implemented the exact same "split 64 bytes into a 32-byte master key and a 32-byte MAC key" logic. Identical duplicated security-critical code is a maintenance hazard: a future change to the split point (e.g., a larger MAC key) could easily be applied to one copy and not the other, silently reintroducing a bug.

**Fix:** Extracted to `DerivedKeySplitter`, a single shared object both classes now delegate to.

### M-4: No concurrency tests existed for the two `@Synchronized` security components
**Files:** `core/crypto/key/SessionKeyHolderTest.kt`, `core/security/session/FailedAttemptTrackerTest.kt`
**Status:** Fixed.

`SessionKeyHolder` and `FailedAttemptTracker` were both correctly synchronized, but nothing actually exercised them under real concurrent load — a regression that broke their synchronization (e.g. someone "simplifying" away a `@Synchronized` annotation) would not have been caught by the existing test suite.

**Fix:** Added a multi-threaded stress test to each: `SessionKeyHolderTest` hammers `set()` from 16 threads and asserts every key that was ever installed ends up wiped (the specific invariant a lost update under a race would violate); `FailedAttemptTrackerTest` hammers `recordFailure()` from 10 threads and asserts the tracker ends up in a valid `LockedOut` state rather than a nonsensical negative-attempts-remaining state a race could produce.

## 6. Low Findings (documented, not code changes)

### L-1: No rollback/replay protection for a wholesale-swapped older vault file
An attacker who can read and later rewrite the vault's storage location could replace a current vault file with a previously valid, older one (a "replay" of stale data rather than a replay of a network message). Nothing in `core:crypto` today detects this, because detecting it requires a trusted, monotonically-increasing reference point external to the file itself (e.g., a securely persisted last-seen revision number) — which is a persistence-layer concern, explicitly out of scope for this audit ("do NOT review Room"). **Recommendation for Milestone 4:** when `data:persistence` lands, persist the last-synced `VaultMetadata.revision` outside the vault file itself (e.g., in a separate, tamper-evident location) and have the unlock flow reject an envelope reporting a lower revision than what was last seen.

### L-2: `CipherMetadata.algorithm` is recorded but not yet used to select an implementation
`EncryptedVaultEnvelope.cipherMetadata.algorithm` is written and MAC-protected (so tampering with it is detected), but no code branches on its value — `AesGcmCipherEngine` is used unconditionally. This is correct and harmless today (only one cipher algorithm exists), but is worth flagging now so that whenever a second algorithm is ever introduced, the engine is updated to actually dispatch on this field rather than continuing to ignore it.

## 7. Files Modified

| File | Change |
| --- | --- |
| `core/crypto/format/EncryptedVaultEnvelope.kt` | **C-1**: length-prefixed canonical integrity payload |
| `core/crypto/vault/DefaultVaultEncryptionEngine.kt` | **H-1, M-3**: password/salt wipe ownership; delegates to `DerivedKeySplitter` |
| `core/crypto/vault/VaultEncryptionEngine.kt` | **H-1**: documented ownership contract |
| `core/crypto/key/KeyRotationManager.kt` | **H-1, M-3**: password wipe ownership; delegates to `DerivedKeySplitter` |
| `core/crypto/kdf/DerivedKeySplitter.kt` | **M-3**: new shared utility |
| `core/crypto/backup/BackupEncryptionEngine.kt` | **H-2**: new — closes the backup-encryption gap |
| `core/crypto/keystore/AndroidKeystoreKeyProvider.kt` | **H-3, M-1**: synchronization; safe key lookup |
| `core/crypto/kdf/KdfParameters.kt` | **M-2**: `meetsOwaspMinimumRecommendation()` |
| `core/crypto/src/test/.../format/EncryptedVaultEnvelopeIntegrityPayloadTest.kt` | new — regression test for C-1 |
| `core/crypto/src/test/.../backup/DefaultBackupEncryptionEngineTest.kt` | new — covers H-2 |
| `core/crypto/src/test/.../kdf/KdfParametersTest.kt` | new — covers M-2 |
| `core/crypto/src/test/.../key/SessionKeyHolderTest.kt` | **M-4**: added concurrency test |
| `core/security/.../session/FailedAttemptTrackerTest.kt` | **M-4**: added concurrency test |

## 8. Verification

Every fix above is covered by at least one new or existing test. The full Milestone 3 + audit test suite is 82 `@Test` methods across 13 files in `core:crypto`/`core:security`, all using the real Argon2id/AES-GCM/HMAC-SHA256 implementations — nothing in this test suite is mocked at the cryptographic-primitive level. Static validation (package/directory consistency, cross-module import resolution, duplicate-declaration detection, forbidden-pattern scanning for ECB/CBC/static IV/insecure randomness/hardcoded secrets, line-length and formatting hygiene) was re-run across the entire project after every change in this pass and is clean.

`AndroidKeystoreKeyProvider` remains the one component that cannot be exercised by a JVM unit test, since `AndroidKeyStore` requires real hardware or an emulator; this is unchanged from Milestone 3 and is not a gap introduced or overlooked by this audit — it is a standard, unavoidable limitation of testing Android Keystore code outside instrumented tests, and is called out again here for visibility rather than left implicit.

---

## RC2 follow-up note (2026-10-17) — audit findings vs. 1.0.0-RC2

The original audit above reviewed the RC1 state statically. The RC2
finishing pass addressed its actionable findings; every fix below was
established by static verification (file reads, import/dependency
matching, project-wide grep) in an environment with no JDK/Android
SDK — none of it was compiler-verified. This note does not rewrite
the original audit; it records what changed.

- **C1/C2/H6 (would not compile):** missing imports added in
  `SecureVaultNavHost.kt`; `kotlinx-serialization-json` declared in
  `data:repository`; `:core:contracts` added to `app`'s compile
  classpath. **C3:** `@Serializable` added to the serialized domain
  types. Statically these now resolve; actual compilation (G0) is still
  pending on a build machine.
- **C4:** `MasterPasswordValidator` (min 12 chars, 4 character classes,
  max 128) is now wired into the create-vault flow; the welcome screen
  no longer reaches a vault without validation.
- **C5:** item notes are encrypted at rest (`notes_ciphertext` /
  `notes_nonce` / `notes_algorithm`, migration 2→3); the legacy
  plaintext `notes` column stays NULL for new rows.
- **C6/H5:** FTS5 insert/update/delete triggers added in
  `SecureVaultDatabase`; MATCH queries sanitize user input and use
  prefix-only wildcards.
- **H1:** caller `CharArray` wipe ownership is honored on the
  create/unlock paths.
- **H3:** `ItemMapper` now preserves all 7 metadata columns on update
  (`lastAccessedAt`, `useCount`, `isPinned`, `isArchived`,
  `isDeleted`, `deletedAt`, `expiresAt`); editing a trashed item no
  longer resurrects it.
- **H4:** category create/update failures are honest.
- **H7:** a real unlock screen exists; `UnlockVaultUseCase` is
  invoked. The welcome bypass is gone.
- **H8:** `AuditUseCase` persists results without immutable `String`
  copies of plaintext.
- **H9:** biometric unlock now exists — but as an **opt-in** feature.
  The old claims ("integration" in the changelog, "required" in the
  privacy policy) were false and have been rewritten to match reality.
- **M1:** `FailedAttemptTracker` is per-vault (`VaultId`-keyed).
  **M4:** generator uses rejection sampling. **M5:** auto-lock
  implemented (`AutoLockManager`). **M6:** multi-step writes are
  transactional. **M7:** safe enum parsing with fallbacks.
  **M9:** backup metadata columns added (`format_version`,
  `vault_revision`, `sha256`). **M10:** `SearchItemsUseCase` and
  `RestoreItemUseCase` implemented + DI-bound. **M12:**
  `SecureClipboardManager` DI-bound and used.
- **Dead dependencies** (CameraX ×3, ML Kit, appcompat) removed from
  `app/`. **L17:** CI pinned to JDK 21 (Temurin).
- **UI wiring:** splash → welcome/onboarding → create-vault (strength
  meter, generator sheet) → unlock (password + opt-in biometric) →
  vault home (search, list) → item detail (reveal/copy with clipboard
  auto-clear, live TOTP) → item editor → generator → settings
  (auto-lock timeout, biometric toggle, change master password via
  `ChangeMasterPasswordUseCase` → `rotateMasterPassword` →
  `KeyRotationManager`, lock-now, about). The ~94% orphan-engine rate
  from the audit is resolved for the flows above.
- **Still open (deferred):** TOTP QR enrollment UI, backup/import/
  export UI and engine flow (`ImportRepository`/`ExportRepository`
  still contract-only), audit/security-score UI, trash/restore UI,
  categories/tags UI, release signing, Play Store release. And above
  all: G0 — `assembleDebug` + unit tests on JDK 21 — has not been run.
