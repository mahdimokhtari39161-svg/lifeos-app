# ShareNow — offline phone-to-phone transfer

ShareNow is an Android-to-Android offline transfer app. It creates a local-only Wi-Fi hotspot or uses an existing local Wi-Fi network, then moves data directly between devices. No cloud account or internet server is involved in a transfer.

## Features

- Transfer photos, videos, music, documents, archives, and any other file type.
- Select multiple files or a complete folder tree; preserve folder structure.
- Built-in QR pairing: the receiver displays a QR code with its local IP, temporary PIN and (when using a ShareNow hotspot) Wi-Fi credentials. The sender scans it inside ShareNow. On Android 10+, Android's system Wi-Fi approval dialog can connect to the hotspot; on Android 8–9, connect from Wi-Fi settings after scanning.
- Discover receivers on a shared local network; manual IP entry remains available as fallback.
- Transfer installed user-facing apps and games as a ShareNow package bundle. The bundle includes the base APK and available split APKs.
- Install received app bundles through Android PackageInstaller; Android displays the system installation confirmation and the user must approve it.
- PIN-protected sessions, AES-256-GCM encryption, and SHA-256 verification per transferred file.
- Large 1 MiB streaming buffers and no image/video recompression.

## Important Android limitations

- This transfers application installation files, not private app data, logins, game saves, or protected cache/OBB data. Those locations are controlled by Android and are not generally available to ordinary apps.
- Some apps cannot be exported because of device policy, work profile restrictions, licensing, or storage protections. Paid apps may require the same store account and entitlement on the second device.
- The receiver must allow ShareNow to install unknown apps in Android settings, then approve Android's system installation prompt. This permission is part of the core app-transfer feature.
- Apps distributed as split APKs are sent with their base and split APK files. A compatible package signature and version are required by Android for a successful installation.
- Android 10+ can request a local Wi-Fi connection after scanning QR. Android 8–9 require the user to join the shown Wi-Fi network manually.
- Both devices need ShareNow installed. This build targets Android 8.0 (API 26) and newer; iOS requires a separate app.

## Package identity and signing

The 2.1.0 build uses the new package id `com.sharenow.transfer` so it can be installed alongside earlier ShareNow test builds that used `com.sharenow.android.debug`. Earlier copies can be removed after the new app is verified.

The debug signing key is restored from a stable GitHub Actions cache before each build. This keeps future APK updates signed with the same certificate while that cache remains available. Keep workflow builds serialized; do not clear the `sharenow-debug-signing-key-v1` cache or future updates may require reinstalling.

## Build

GitHub Actions workflow: `.github/workflows/build-sharenow.yml`.
It builds the debug APK and uploads the artifact as `ShareNow-Android-APK`.

## How to connect and transfer

1. Install the same ShareNow APK on both Android phones.
2. Receiver taps **Create offline network** or **Use current Wi-Fi**.
3. Receiver taps **Show QR** and keeps the QR visible.
4. Sender taps **Scan receiver QR** in ShareNow and scans the receiver screen.
5. On Android 10+, approve the system Wi-Fi connection dialog when shown. On older Android, open Wi-Fi settings and connect to the SSID/password shown by the receiver.
6. Sender chooses files, folders, or installed apps/games and taps **Send**.
7. Received regular files are saved in `Downloads/ShareNow`. Received app bundles can be installed from the receiver's **Install received apps and games** button.

## Privacy and security

- Transfers are encrypted in transit with AES-256-GCM using a key derived from the one-time PIN and a random per-session salt.
- Each completed file is checked with SHA-256 before it is marked as received.
- Do not share the receiver QR publicly: it includes the local Wi-Fi password and current session PIN.
- No file transfer is sent to an online server.
