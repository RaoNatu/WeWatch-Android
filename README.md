# WeWatch Android

Native Android companion app for [WeWatch](https://github.com/RaoNatu/WeWatch) on Windows.

## What It Does

- Connects to the Windows WebSocket server at `ws://<PC IP>:3000`.
- Joins as an Android member using the same `hello`, `status`, `action`, `sync`, `clients`, `control`, `event`, and `ping/pong` messages as the Electron app.
- Opens selected Android media in the official VLC app instead of embedding a separate player.
- Bridges VLC Android Remote Access / VLC HTTP status into the Windows sync server.
- Publishes VLC playback state once per second.
- Applies Windows host play, pause, seek, and sync commands to the VLC app.
- Can send VLC play, pause, seek, and file events back to the Windows VLC user.
- Includes light/dark mode, seek controls, 10-second skip controls, and keyboard/status-bar safe layout handling.

The WebSocket server does not stream the video file. For synced watching, open the same media on Android VLC and on the Windows VLC side.

## Requirements

- Android Studio
- Android SDK Platform 35
- JDK 17, or the JBR bundled with Android Studio
- VLC for Android
- Android phone/emulator on the same network as the Windows host

The app package is `com.wewatch.android`, with minimum SDK 23 and target SDK 35.

## Open In Android Studio

1. Open this folder in Android Studio.
2. If Android Studio asks to install an SDK, install:
   - Android SDK Platform 35
   - Android SDK Build-Tools
   - Android SDK Platform-Tools
3. Let Android Studio create `local.properties`, or create it manually:

   ```properties
   sdk.dir=C\:\\Users\\<you>\\AppData\\Local\\Android\\Sdk
   ```

4. Sync Gradle, then run the `app` configuration on an emulator or Android phone.

## Use With The Windows App

1. Start the Windows Electron app ([WeWatch](https://github.com/RaoNatu/WeWatch)).
2. Click `Start hosting`; keep the port as `3000` unless you changed it.
3. Allow Windows Firewall access for the app/server on your private network.
4. On Android, enter the Windows PC IPv4 address and port.
5. In VLC for Android, enable Remote Access and note the URL plus OTP.
6. In the Android app, enter the VLC Remote URL and OTP/password, then tap `Connect VLC`.
7. Open VLC and choose the matching video there, or use `Send file to VLC` from the Android app.
8. Tap `Join Windows`.

If the phone cannot connect, confirm both devices are on the same Wi-Fi, the Windows IP is correct, and TCP port `3000` is allowed through the firewall.

## Build From Terminal

After the Android SDK is installed and `local.properties` exists:

Debug APK:

```powershell
$env:JAVA_HOME="C:\Program Files\Android\Android Studio\jbr"
.\gradlew.bat :app:assembleDebug
```

The debug APK is generated at `app\build\outputs\apk\debug\app-debug.apk`.

Release APK:

```powershell
$env:JAVA_HOME="C:\Program Files\Android\Android Studio\jbr"
.\gradlew.bat :app:assembleRelease
```

The release APK is generated at `app\build\outputs\apk\release\app-release.apk`.

If you have not added a release keystore yet, Gradle will create `app-release-unsigned.apk` instead. Do not upload the unsigned APK for real users; create the keystore first.

## Release Keystore

Before sharing release APKs widely, create one Android release keystore and reuse it forever:

```powershell
keytool -genkeypair -v -keystore wewatch-release.jks -alias wewatch -keyalg RSA -keysize 2048 -validity 10000
```

Create `keystore.properties`:

```properties
storeFile=wewatch-release.jks
storePassword=your-store-password
keyAlias=wewatch
keyPassword=your-key-password
```

`keystore.properties` and `.jks` files are ignored by git.

## Versioning

Edit `gradle.properties` directly to set the version:

```properties
WEWATCH_VERSION_NAME=1.1.0
WEWATCH_VERSION_CODE=10100
```

Or use the helper script:

```powershell
.\scripts\set-version.bat 1.1.0
```

The `versionCode` formula is: `major * 10000 + minor * 100 + patch`.

This version is **independent** from the Windows app version. You can ship Android 1.1.0 while Windows is on 2.0.0.

Yes — you can publish this app and README publicly. If your repo is public, share the signed release APK through GitHub Releases or another distribution channel once the version is updated and the app is built for release.

## Release Workflow

1. Set the version in `gradle.properties`.
2. Test the app.
3. Commit and push the code.
4. Build the release APK.
5. Create a GitHub Release named/tagged `v1.1.0`.
6. Rename the APK (e.g. `WeWatch-Android-1.1.0.apk`) and upload it as a release asset.

Important notes:

- The version must go up every release, or users will not be offered an update.
- Android updates only install over apps signed with the same signing key. Keep your release keystore safe forever once you start sharing release APKs.
- A public GitHub repo does not need a token for users to receive updates.

## Troubleshooting

- If Android cannot connect, check that both devices are on the same network and Windows Firewall allows TCP port `3000`.
- If VLC control fails, re-check VLC Remote Access URL and OTP/password.
- If sync works but video differs, make sure every device opened the same media file.

## License

ISC
