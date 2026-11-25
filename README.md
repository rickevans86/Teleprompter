# Telepromptor

A native Android teleprompter. One phone controls playback and up to three Android displays follow over Bluetooth Classic. Teleprompting works offline and requires no app account or remote storage.

## Build and run

Use Android Studio, a JDK 17 or later, and the Android 17 SDK (API 37). The app supports Android 8.0 and later. Open this repository as a Gradle project or run:

```sh
sdkmanager 'platforms;android-37.0' 'build-tools;36.0.0'
./gradlew :app:assembleDebug :app:testDebugUnitTest
```

The debug APK is written to `app/build/outputs/apk/debug/app-debug.apk`. Run Compose instrumentation tests with `./gradlew :app:connectedDebugAndroidTest` on an emulator or connected device. GitHub Actions builds the APK, runs the JVM and emulator suites, and uploads the debug APK. Run `./gradlew ktlintCheck detekt` for style and static analysis.

## Install and update

Download `Telepromptor.apk` from the [latest release](https://github.com/rickevans86/Teleprompter/releases/latest). In the app, open **Settings → Update app** to get the latest signed APK in your browser. Sign in to GitHub if prompted and open the download to confirm installation in Android. Updates between signed releases keep your local scripts.

When the app starts it checks the latest GitHub release and shows an **Update available** banner on the home screen if a newer version exists. Tap **Download** to fetch the APK or **Later** to dismiss it for that session. The check is silent when the phone is offline or GitHub cannot be reached, and it is the only network use in the app. Teleprompting itself still works offline.

Earlier debug builds use a separate signing key. Save a copy of each script before removing a debug build to install the signed release.

For future releases, increment `versionCode`, sign the release APK with the original release key, and upload it to GitHub Releases as `Telepromptor.apk`. The update button uses that filename.

## CI runner

Pushes to `main` and `native-teleprompter` use a repository runner with the standard `self-hosted`, `Linux`, and `X64` labels. Pull requests and pushes to other branches use GitHub-hosted runners. Keep write access to the self-hosted branches limited to people you trust with the runner machine.

The runner needs a supported version of the GitHub Actions runner software and access to GitHub, Maven and Android SDK downloads. The UI job uses an x86_64 emulator and requires hardware virtualization with read and write access to `/dev/kvm` for the runner account. The APK build and JVM tests can run without KVM.

## Connect two phones

1. Install the APK on both phones. On the first phone, choose **HOST**, paste a script, optionally save it to the local script library, and tap **Go live**.
2. Tap **Clients**, grant Bluetooth access if prompted, then tap **Start hosting**. Tap **Make discoverable** for an unpaired client and accept the Android dialog.
3. On the second phone, choose **CLIENT**. Select the paired host or tap **Scan nearby** and select the host by its Bluetooth device name. Confirm Android’s pairing prompts on both phones if they appear. You can also pair both phones in Android Bluetooth Settings first.
4. Return to the host’s live panel and test play/pause, speed, font size, mirror, theme, alignment and seeking. Enable the 3-second countdown and try restarting. Verify that the client keeps the display awake and hides the system bars.
5. Temporarily move the phones out of range. The client keeps its script and retries the same host when the link returns. Verify that it resynchronizes to the host. Then disconnect the client from the host’s list and verify that it returns to device selection.

The host foreground notification can stop hosting. The live panel can preview a script before Bluetooth hosting starts.

## Voice scrolling

In the host’s live panel, tap **Start voice scrolling** and grant microphone access. The script moves at the selected speed while you speak and pauses after about half a second of quiet. Connected clients follow the same playback. Tap **Stop voice** to pause and turn off the microphone. Use **Play** or **Restart** for manual playback and its optional countdown.

Voice scrolling listens only while the host’s live panel is in the foreground. It stops when you leave the panel or lock the phone. The microphone audio is processed on the host and is never saved or sent to clients. The detector reacts to sound level and sustained background noise can trigger it. It works best close to the host microphone in a quiet room.

## Android permissions

- **Android 12 and newer** requests Nearby devices access to connect, scan on the client, and make the host discoverable.
- **Android 8 through 11** declares Bluetooth and Bluetooth Admin. Client discovery also needs Android’s Location permission and may require the system Location setting to be on. The app never reads location coordinates.
- **Microphone** is requested only when you start voice scrolling. Manual playback works without it.
- The host uses a connected-device foreground service and holds a partial wake lock while at least one client is connected so playback sync can continue with its screen off. Android may show its notification in the active apps panel depending on notification settings.

## Playback behavior and limitations

Positions are fractions of measured text height. The host derives the fractional playback rate from its preview, so devices stay close in overall progress even though line wrapping, speed in physical pixels, and the visible line can differ. An intentional seek or a fresh connection resynchronizes immediately. During playback, small drift is ignored.

A disconnected client continues its last playback state until reconnection or until it reaches the end. Client reconnection lasts while the client screen is active. The host service survives the screen turning off, but a force-stop or process termination ends the session. Room preserves explicitly saved scripts; the live session and unsaved drafts are in memory. Scripts are limited to 2 MB of UTF-8, and titles to 200 characters.

RFCOMM and pairing behavior vary by device. Emulator tests cover UI and the in-memory transport is used for protocol tests. Bluetooth discovery, foreground survival, and multi-phone sync require physical phones.
