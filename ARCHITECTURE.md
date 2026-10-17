# SecureVault Next Architecture

> **Current as of 1.0.0-RC2 (2026-10-17).** The module overview below
> remains accurate; the key flows (password unlock, opt-in biometric
> unlock, auto-lock, change master password, item lifecycle, search)
> are described in [`FINAL_REPORT.md`](./FINAL_REPORT.md).

SecureVault Next is built with a strict multi-module Clean Architecture approach to ensure maximum security, testability, and maintainability.

## Module Structure

### Layers
- **App Module (`:app`)**: The composition root. Handles Hilt DI, Navigation, and Activity hosting.
- **Domain Layer (`:core:domain`)**: Pure Kotlin. Contains Entities, Use Cases, and Repository Interfaces. No dependencies on other layers or Android.
- **Data Layer (`:data:persistence`, `:data:repository`)**: Implementation of repository interfaces. Room database management and data mapping.
- **UI Layer (`:core:ui`)**: Shared Compose components, themes, and design system.

### Functional Cores
- **Crypto Core (`:core:crypto`)**: Android-specific cryptography implementations (Keystore, AES-GCM, Argon2id).
- **Security Core (`:core:security`)**: Session management, auto-lock logic, and lockout trackers.
- **Vault Core (`:core:vault`)**: Serialization and versioning logic for vault envelopes.
- **Platform Core (`:core:platform`)**: Wrappers for Android system services (Clipboard, Biometrics).

## Key Design Principles
1. **Dependency Inversion**: High-level modules do not depend on low-level modules; both depend on abstractions.
2. **Interface-Based Design**: All external boundaries (Crypto, Persistence) are hidden behind interfaces defined in the Domain layer.
3. **Immutability**: Preference for immutable data classes and `StateFlow` for UI state.
4. **Secure Memory**: Use of `SecureBytes` and `MemoryWiper` to zero out sensitive data immediately after use.

## Data Flow
`UI (View) -> ViewModel -> Use Case -> Repository -> Data Source (DAO/Crypto)`

The ViewModel observes `Flow`s from Use Cases/Repositories to maintain a reactive UI.

## Security Indirection
We use a key hierarchy:
`Master Password -> Argon2id -> Master Key -> Wrapped Vault Key -> AES-GCM -> Content`
This allows for password changes and KDF parameter upgrades without re-encrypting the entire vault.
