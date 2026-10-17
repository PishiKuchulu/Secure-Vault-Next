# SecureVault Next — Final Engineering Report (1.0.0-RC2)

**Date:** 2026-10-17 · **Version:** 1.0.0-RC2 (versionCode 2) ·
**Method:** static source finishing + static verification (file reads,
import/dependency matching, project-wide grep).

## 1. What this release is

SecureVault Next is an offline-first Android password manager
(AES-256-GCM, Argon2id, Android Keystore key wrapping, Jetpack
Compose, Clean Architecture, 12 Gradle modules). 1.0.0-RC2 addresses
every actionable finding of the independent full-project static audit
of 2026-10-17 (the RC1 state) and wires the engine to a complete set
of UI flows.

**Hard constraint on everything below:** this release was finished and
statically verified in an environment with **no JDK and no Android
SDK**. It has not been compiled, and no test has been executed here.
Gate G0 (`./gradlew assembleDebug` + `./gradlew testDebugUnitTest`,
JDK 21) must pass on a build machine before any release claim.
There are no scores in this report — nothing was measured at runtime.

## 2. What was fixed (audit findings → code)

| Finding | Resolution |
|---|---|
| C1/C2/H6 — would not compile (missing NavHost imports, undeclared kotlinx.serialization, missing `:core:contracts` on app classpath) | Imports added; `kotlinx-serialization-json` declared in `data:repository`; `:core:contracts` dependency added to `app` |
| C3 — missing `@Serializable` on serialized types | Annotations added |
| C4 — master-password policy never enforced | `MasterPasswordValidator` (min 12, 4 classes, max 128) wired into create-vault |
| H1 — caller `CharArray` never wiped | Wipe ownership honored on create/unlock paths |
| C5 — notes stored in plaintext | New `notes_` encrypted columns (`notes_ciphertext`/`notes_nonce`/`notes_algorithm`); migration 2→3; legacy column stays NULL for new rows |
| C6/H5 — FTS index never populated; leading-`*` crash | insert/update/delete FTS triggers in `SecureVaultDatabase`; safe MATCH construction (prefix-only wildcards, sanitized input) |
| H3 — item updates silently reset 7 columns (and resurrected trashed items) | Mapper preserves `lastAccessedAt`, `useCount`, `isPinned`, `isArchived`, `isDeleted`, `deletedAt`, `expiresAt` |
| H4 — category create/update reported success while doing nothing | Honest failures |
| H7 — welcome screen bypassed authentication entirely | Real unlock screen; `UnlockVaultUseCase` is invoked |
| H8 — audit copied passwords into un-wipeable `String`s; results never persisted | Persistence without `String` copies |
| H9 — false biometric claims in docs | Biometric unlock now exists (opt-in); Privacy Policy rewritten to match reality (see §5) |
| M1 — lockout tracker was a global singleton | Per-vault `FailedAttemptTracker` (`VaultId`-keyed) |
| M4 — password generator modulo bias | Rejection sampling |
| M5 — no auto-lock at all | `AutoLockManager`: activity-lifecycle background detection + policy-delayed lock |
| M6 — multi-step writes without transactions | Room transactions on create/update |
| M7 — raw `valueOf` on stored data | Safe enum parsing with fallbacks |
| M9 — backup metadata lacked integrity fields | `format_version`, `vault_revision`, `sha256` columns (migration 2→3) |
| M10 — `SearchItemsUseCase` / `RestoreItemUseCase` contract-only | Implemented and DI-bound |
| M12 — `SecureClipboardManager` unused | DI-bound in `PlatformModule`, used in item detail (auto-clear on copy) |
| Dead deps (CameraX ×3, ML Kit, appcompat) | Removed from `app/` |
| L17 — CI on JDK 17 vs `jvmToolchain(21)` | CI pinned to JDK 21 (Temurin) |

Also added: `KeyRotationManager` (`DefaultKeyRotationManager`) bound
in `CryptoModule`; `rotateMasterPassword` contract on
`VaultKeyEncryptionService`; password-history write path.

## 3. What was built (UI)

Complete flows, all wired to the engine through DI with no decorative
controls:

- **Splash → welcome/onboarding → create-vault**: live strength
  meter, generator sheet, policy enforced before the engine call.
- **Unlock**: password unlock + **opt-in biometric quick-unlock**
  (`BiometricKeyManager`): a per-vault biometric-bound Keystore key
  wraps the session key; only the wrapped blob (ciphertext) is
  persisted; new biometric enrollment permanently invalidates the key;
  the master password is never stored.
- **Vault home**: search + item list. **Item detail**: reveal/copy
  with clipboard auto-clear, live TOTP. **Item editor**: password +
  secure note. **Generator screen**.
- **Settings**: auto-lock timeout, biometric toggle, change master
  password (verifies current password, rotates the key envelope),
  lock-now, about.

Design system in `core:ui`: one locked accent (`#0B6EDE` light /
`#4DA3FF` dark), cool-grey family, semantic light+dark tokens, locked
shapes, shared components used by every screen.

## 4. What was deferred — explicitly not in RC2

- TOTP **QR enrollment** UI (live TOTP display in item detail works;
  enrollment from QR is not built).
- Backup / import / export **UI and end-to-end engine flow**
  (`ImportRepository` / `ExportRepository` are still contract-only;
  only the integrity-metadata model exists).
- Audit and security-score UI (engines exist, no screens).
- Trash / restore UI (`RestoreItemUseCase` implemented, no screen).
- Categories / tags UI.
- Play Store release and release signing configuration.

## 5. Corrections to prior documents

- `CHANGELOG.md`: the 1.0.0-RC1 entry (2026-07-21) was written against
  a planning state, not shipped code — it claimed biometric
  integration, backup/export/import functionality, addressed audit
  findings, and production readiness that did not exist. A correction
  note is attached to that entry; the 1.0.0-RC2 entry lists only what
  was actually done.
- `PRIVACY_POLICY.md`: "Biometrics: Required for quick unlock" was
  false (no biometric code existed at all). Rewritten: biometric
  quick-unlock is opt-in, never required, and invalidated on new
  enrollment.
- `README.md`: the stale "Nothing beyond project scaffolding exists
  yet" paragraph removed; describes the real RC2 state, the module
  map, build instructions, the G0 gate, and the deferred list.

## 6. What still needs verification (build machine / device)

1. **G0 — compile + tests.** `./gradlew assembleDebug` and
   `./gradlew testDebugUnitTest` on JDK 21. This is the single biggest
   open item: everything here is statically verified, not compiler
   verified.
2. **Biometric flow on a real device**: enrollment prompt ordering
   (`authenticate` before `enable`), `setInvalidatedByBiometricEnrollment`
   behavior on new enrollment, `BiometricPrompt` integration on API 29
   devices (minSdk 29; `AUTH_BIOMETRIC_STRONG` paths are API 30+).
3. **Auto-lock timing**: background → delay → lock observed with the
   configured policy; no premature locking on activity rotation.
4. **Migration 2→3 on a real device DB**: encrypted-notes columns,
   category columns, backup metadata columns, FTS rebuild.
5. **FTS search behavior**: special-character input, empty results,
   prefix queries.
6. `ktlintCheck` and the CI workflow on JDK 21.

## 7. Governance

Release discipline: no release claim is made before `assembleDebug` and
the unit test suite pass on a build machine with JDK 21. Coding rules
drawn from the audit are enforced via the ktlint/detekt configuration
in this repo. Security decisions and accepted risks are documented in
[`SECURITY_AUDIT.md`](./SECURITY_AUDIT.md).
