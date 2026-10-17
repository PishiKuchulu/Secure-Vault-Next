# Changelog

All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.1.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [1.0.0-RC2] - 2026-10-17

Release candidate addressing every actionable finding of the
independent full-project static audit (2026-10-17), plus the first
complete set of UI flows. **Not compiled or tested in the authoring
environment** (no JDK/Android SDK) — G0 (`assembleDebug` + unit tests
on JDK 21) is still outstanding on a build machine.

### Fixed
- Both compile-breakers: missing imports in `SecureVaultNavHost.kt`;
  undeclared `kotlinx-serialization-json` dependency in
  `data:repository`; missing `:core:contracts` dependency on `app`'s
  compile classpath.
- Missing `@Serializable` annotations on serialized domain types.
- Master-password policy now enforced at vault creation
  (`MasterPasswordValidator`: min 12 chars, 4 character classes, max
  128); caller `CharArray` wipe ownership honored.
- Item notes encrypted at rest (`notes_ciphertext`/`notes_nonce`/
  `notes_algorithm`, migration 2→3); legacy plaintext column retained
  NULL for new rows.
- FTS5 index kept in sync via insert/update/delete triggers; MATCH
  queries are safely constructed (prefix-only wildcards, sanitized
  input).
- Item updates preserve all 7 metadata columns
  (`lastAccessedAt`, `useCount`, `isPinned`, `isArchived`, `isDeleted`,
  `deletedAt`, `expiresAt`); editing a trashed item no longer
  resurrects it.
- Category create/update failures are honest (no more false success).
- Real unlock screen: the welcome screen no longer bypasses
  authentication.
- `AuditUseCase` persists results without immutable `String` copies of
  plaintext.
- Failed-attempt lockout is per-vault (cross-vault budget exhaustion
  fixed).
- Password generator uses rejection sampling (modulo bias removed).
- Safe enum parsing with fallbacks (no raw `valueOf` on stored data).
- Multi-step item writes run inside Room transactions.
- Backup integrity-metadata columns added (`format_version`,
  `vault_revision`, `sha256`).
- Dead dependencies removed from `app/`: CameraX (×3), ML Kit,
  appcompat. CI pinned to JDK 21 (Temurin).
- `VaultSessionManager` is now a `@Singleton`: previously Hilt created a
  separate instance per injection site, so manual lock and auto-lock
  silently no-oped.
- `MIGRATION_2_3` and the FTS sync callback registered in
  `DatabaseModule` (migration + triggers were defined but never wired).
- `SecureClipboardManager` bound in DI (new `PlatformModule`).
- Orphaned version-catalog entries purged (CameraX, ML Kit, appcompat,
  lifecycle-runtime-compose).
- Change master password: new `ChangeMasterPasswordUseCase` →
  `rotateMasterPassword` on the crypto contract → `KeyRotationManager`
  → rotated envelope persisted via new
  `VaultRepository.updateEnvelopeJson`; settings screen row with
  inline validation. Both password arrays wiped on every path.

### Added
- `KeyRotationManager` (`DefaultKeyRotationManager`), bound in DI;
  `rotateMasterPassword` contract on `VaultKeyEncryptionService`.
- `SearchItemsUseCase` and `RestoreItemUseCase` implementations with
  DI bindings.
- **Biometric quick-unlock (opt-in)**: per-vault biometric-bound
  Android Keystore key wraps the session key; new biometric enrollment
  invalidates it; master password is never stored. Toggle in settings.
- Auto-lock: background detection (activity-lifecycle observer) with a
  policy-driven lock delay.
- Full UI flows: splash → welcome/onboarding → create-vault (live
  strength meter, generator sheet) → unlock (password + biometric) →
  vault home (search, list) → item detail (reveal/copy with clipboard
  auto-clear, live TOTP) → item editor → generator screen → settings
  (auto-lock timeout, biometric toggle, change master password, lock
  now, about).
- Design system in `core:ui`: one locked accent (`#0B6EDE` light /
  `#4DA3FF` dark), cool-grey family, semantic light+dark tokens, shared
  components.

### Deferred
- TOTP QR enrollment UI, backup/import/export UI and engine flow,
  audit/security-score UI, trash/restore UI, categories/tags UI,
  Play Store release, release signing configuration.

## [1.0.0-RC1] - 2026-07-21

> **Correction (2026-10-17):** this entry was written against a
> planning state, not shipped code. At the time, "Biometric
> authentication integration" and "Automated backup and export/import
> functionality" did not exist in the code (no `androidx.biometric`
> calls anywhere; backup/import/export unimplemented), the audit
> findings were not all addressed, and the project did not compile.
> The claims were corrected in 1.0.0-RC2. The entry is kept here for
> history.

### Added
- Complete project-wide security audit and hardening.
- Production-ready cryptography engine (AES-256-GCM, Argon2id).
- Hardware-backed Android Keystore integration with StrongBox support.
- Biometric authentication integration.
- Clean Architecture implementation with multi-module isolation.
- Offline-first Room persistence with encrypted field support.
- Material 3 Compose UI for all primary flows.
- Automated backup and export/import functionality.
- Comprehensive unit and integration test suite.
- Architecture, Contribution, and Privacy documentation.

### Changed
- Finalized project versioning for Release Candidate.
- Optimized R8/Proguard configuration for production builds.
- Refined session management and auto-lock policies.

### Fixed
- Delimiter-based integrity payload injection vulnerability.
- Various repository placeholders and TODOs.
- Cleanup of scaffolding resources and debug strings.
