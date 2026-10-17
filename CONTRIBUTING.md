# Contributing to SecureVault Next

Thank you for your interest in contributing to SecureVault Next! We welcome contributions that help make this password manager more secure, stable, and user-friendly.

## Our Philosophy
We prioritize:
1. **Security**: Every change must be audited for security implications.
2. **Privacy**: We do not collect user data.
3. **Architecture**: We strictly follow Clean Architecture and SOLID principles.
4. **Quality**: High test coverage and adherence to static analysis rules (Ktlint, Detekt).

## Getting Started
1. Fork the repository.
2. Create a feature branch: `git checkout -b feature/amazing-feature`.
3. Ensure your environment matches the version stack in `README.md`.

## Coding Standards
- **Kotlin**: Follow official Kotlin coding conventions.
- **Compose**: Use Material 3 components and follow "State Hoisting" patterns.
- **Testing**: Every new feature must include unit tests. Repositories and DAOs should include integration tests.

## Security Disclosures
Please do **not** open a public issue for security vulnerabilities. Instead, report them via email to: security@securevault.next.

## Pull Request Process
1. Run `./gradlew ktlintCheck detekt` to ensure code style compliance.
2. Run `./gradlew test` to ensure all tests pass.
3. Ensure the `CHANGELOG.md` is updated with your changes.
4. Submit your PR with a clear description of the problem and your solution.

## Licensing
By contributing, you agree that your contributions will be licensed under the project's Open Source License.
