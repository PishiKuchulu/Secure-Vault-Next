# Privacy Policy for SecureVault Next

**Last Updated: 2026-10-17**

SecureVault Next is an offline-first password manager designed with a "Privacy by Design" philosophy. Your security and privacy are our highest priorities.

## 1. Data Collection and Usage
- **No Personal Data Collection**: SecureVault Next does not collect, transmit, or store any personal data on its own servers. We do not have access to your passwords, email address, or any other identifying information.
- **Local Storage**: All your vault data is stored locally on your device in an encrypted database.
- **No Analytics**: This application does not include any third-party analytics or tracking libraries.

## 2. Data Encryption
- Your data is encrypted using industry-standard AES-256-GCM.
- Encryption keys are derived using Argon2id with high memory and iteration costs.
- Keys are protected by the Android Keystore system (Hardware-backed TEE or StrongBox).

## 3. Permissions and Device Features
- **Biometrics (optional, opt-in)**: Biometric quick-unlock is an
  opt-in convenience feature, never required. If you enable it, a
  biometric-bound key in the Android Keystore wraps your vault's
  session key; you can still always unlock with your master password.
  Biometric data is handled exclusively by the Android system; the app
  never sees your raw biometric data. Enrolling a new biometric
  permanently invalidates the stored key, after which you re-enroll
  with your master password.
- **Notifications**: Used to notify you when the vault is unlocked or when a background task is running.
- **Storage/Media**: Only used if you explicitly choose to export or import a backup file.

## 4. Third-Party Services
SecureVault Next is entirely self-contained and does not communicate with any third-party services, cloud providers, or advertisers.

## 5. Your Rights
As all data is stored locally, you have full control over your data. You can delete your vault at any time by clearing the application data or uninstalling the app.

## 6. Changes to This Policy
We may update our Privacy Policy from time to time. We will notify you of any changes by posting the new Privacy Policy on this page.

## 7. Contact Us
If you have any questions about this Privacy Policy, please contact the developer at: support@securevault.next
