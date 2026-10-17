# SecureVault Next

An offline-first, security-first Android password manager: AES-256-GCM
encryption, Argon2id key derivation, Android Keystore-backed key
wrapping, opt-in biometric quick-unlock, Jetpack Compose UI, and a Clean
Architecture multi-module structure that keeps the business core
platform-independent.

**Status: 1.0.0-RC2** (versionCode 2, 2026-10-17). This release fixes
all actionable findings of an independent full-project static audit
(see [`SECURITY_AUDIT.md`](./SECURITY_AUDIT.md), RC2 follow-up note)
and adds the complete core UI flows. It has **not been compiled or
tested in the authoring environment** (no JDK/Android SDK here) — the
G0 gate (`./gradlew assembleDebug` + unit tests on JDK 21) must run on
a build machine before any release claim.

## What works (engine wired to UI)

- **Vault lifecycle**: splash → welcome/onboarding → create vault with
  live strength meter and generator sheet → unlock.
- **Master-password policy**: enforced at creation — min 12 chars,
  4 character classes, max 128 — by the engine's
  `MasterPasswordValidator`.
- **Unlock**: password unlock via `UnlockVaultUseCase` with per-vault
  failed-attempt lockout; **opt-in biometric quick-unlock** — a
  biometric-bound Keystore key wraps the session key per vault (new
  biometric enrollment permanently invalidates it).
- **Items**: vault home (search + list), item detail (reveal/copy with
  clipboard auto-clear, live TOTP), item editor (password + secure
  note), generator screen.
- **Change master password**: verifies the current password and
  rotates the vault key envelope (`KeyRotationManager`).
- **Auto-lock**: background detection with a policy-driven lock delay
  (configurable in settings, `NEVER` disables).
- **Search**: FTS5 with DB triggers and safely constructed MATCH
  queries.

## Module map

```
app/                    Composition root: Hilt app, MainActivity, navigation, DI wiring,
                        Compose screens, BiometricKeyManager, AutoLockManager
core/
  common/                Pure Kotlin/JVM — dispatchers, result types, memory wiping
  contracts/             Pure Kotlin/JVM — engine service interfaces
  crypto/                Android library — Argon2id, AES-256-GCM, Keystore, password generator,
                         TOTP engine, key rotation
  domain/                Pure Kotlin/JVM — entities, use cases, repository interfaces,
                         MasterPasswordValidator
  platform/              Android library — SecureClipboardManager, biometric prompt host
  security/              Pure Kotlin/JVM — session, per-vault attempt lockout, auto-lock evaluation
  ui/                    Android library — design system (semantic tokens light+dark, shared components)
  vault/                 Pure Kotlin/JVM — vault lifecycle, item use cases, audit
  testing/               Pure Kotlin/JVM — shared JUnit rules
data/
  persistence/           Android library — Room, FTS5 triggers, migrations
  repository/            Android library — repository implementations, item mapper
```

`core:common`, `core:contracts`, `core:domain`, `core:security`,
`core:vault` are pure Kotlin/JVM with zero Android dependency — the
business core stays reusable outside Android.

## Build

```bash
./gradlew assembleDebug      # build the debug APK (needs Android SDK + JDK 21)
./gradlew testDebugUnitTest  # run unit tests
./gradlew ktlintCheck        # static style analysis
```

CI (`.github/workflows/ci.yml`) runs on JDK 21 (Temurin), matching the
modules' `jvmToolchain(21)`.

### A note on the Gradle wrapper JAR

`gradle/wrapper/gradle-wrapper.jar` is a binary that could not be
generated in the sandbox this project was finished in (no network
access). Everything else — `gradlew`, `gradlew.bat`,
`gradle-wrapper.properties` — is in place. The first time you open this
project in Android Studio, or run `gradle wrapper` with a local Gradle
install, the wrapper jar will be generated automatically.

### Version stack

| Tool | Version |
| --- | --- |
| Android Gradle Plugin | 8.7.3 |
| Kotlin | 2.1.20 |
| KSP | 2.1.20-1.0.32 |
| Hilt / Dagger | 2.58 |
| Room | 2.7.1 |
| `androidx.biometric` | 1.2.0-alpha05 |
| kotlinx-serialization | 1.7.3 |
| compileSdk / targetSdk | 36 |
| minSdk | 29 (Android 10+) |

## Deferred (not in RC2)

TOTP QR enrollment UI · backup/import/export UI and engine flow ·
audit and security-score UI · trash/restore UI · categories/tags UI ·
Play Store release and release signing configuration.

## Governance

- [`FINAL_REPORT.md`](./FINAL_REPORT.md) — honest account of what was
  done, deferred, and still to verify. No release claim is made before
  `assembleDebug` and the unit tests pass on a build machine with JDK 21.
