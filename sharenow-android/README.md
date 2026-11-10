# ShareNow

ShareNow is an Android-to-Android offline file-transfer app. It uses a local Wi-Fi network or an Android Local-Only Hotspot and transfers files directly over a TCP connection. No cloud account, mobile data, or internet server is used for the transfer.

## Current implementation

- Pick multiple files or entire folders from Android's document picker; preserve folder structure.
- Discover ShareNow receivers on the same local network using UDP broadcasts.
- Start a local-only hotspot on Android 8.0+ so two phones can connect without an internet connection.
- Manual IP entry is available when discovery is blocked by a router.
- PIN-protected sessions and AES-256-GCM encryption with a per-session derived key.
- SHA-256 integrity verification for each transferred file.
- Files are saved in Downloads/ShareNow on Android 10+.
- Transfer progress and local session status.

## Build

The GitHub Actions workflow at .github/workflows/build-sharenow.yml builds a debug APK and uploads it as the artifact `ShareNow-Android-APK`.

Minimum supported Android version: Android 8.0 (API 26).

## How to transfer

1. Install the APK on both Android phones.
2. On the receiving phone, choose **Create offline network**. ShareNow displays the Wi-Fi name, password, and one-time transfer PIN.
3. Connect the sending phone to that Wi-Fi network using Android Wi-Fi settings.
4. On the sending phone, choose files, discover the receiver (or enter its IP), enter the PIN, and send.
5. Received files appear in the Downloads/ShareNow folder.

You can also select **Use current Wi-Fi** on the receiving phone when both phones are already connected to the same local Wi-Fi network.

## Notes

- The first version targets Android phones. An Android APK cannot be installed on iPhones; a separate iOS app would be needed for iPhone-to-Android or iPhone-to-iPhone transfers.
- Wi-Fi discovery depends on the local router or hotspot allowing device-to-device traffic and UDP broadcasts. Manual IP entry is the fallback.
- The receiver must keep ShareNow open while the transfer is active.
