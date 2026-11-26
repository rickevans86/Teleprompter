# Architectural decisions

## 2026-11-25 · Established architecture

Recorded by Codex from main while preparing [issue #1](https://github.com/rickevans86/Teleprompter/issues/1).

- Bluetooth Classic RFCOMM connects one host to up to three clients. The host sends script and playback state, and each display renders playback locally.
- Room stores scripts on the device. Host voice scrolling uses local audio activity and shares playback state over the existing Bluetooth link.
- The update setting opens the latest GitHub APK download in a browser. Android handles installation confirmation, and release upgrades use the same signing key.
- GitHub Issues, commits, and PRs are authoritative. Shared records summarize them under Rick's three-AI collaboration protocol.

## 2026-11-25 · Direct development on main

Rick directed that all work and shared records go directly to `main`. Issue assignment, AI attribution, file claims, and independent review still apply. Codex recorded this decision under issue #1.

## 2026-11-25 · Startup update check and CI SDK setup

Recorded by Claude under [issue #3](https://github.com/rickevans86/Teleprompter/issues/3) and [issue #4](https://github.com/rickevans86/Teleprompter/issues/4).

- The app now declares the `INTERNET` permission solely to read the latest GitHub release at startup. The check is unauthenticated, silent on any failure, and shows a dismissible home-screen banner rather than a dialog so it cannot block navigation or UI tests. Bluetooth sync and script storage stay offline.
- Update detection compares the release tag with `BuildConfig.VERSION_NAME` as dotted numbers. Non-version tags never trigger the banner.
- CI runs `android-actions/setup-android` on every runner with `packages: platform-tools`, so GitHub-hosted runners have `sdkmanager` and the removed `tools` package is not requested.
- Current releases are debug-signed. A later release signed with a different key will not install over them, so users must uninstall first.

## 2026-11-26 · Signed releases from GitHub Actions

Recorded by Claude under [issue #7](https://github.com/rickevans86/Teleprompter/issues/7).

- Release APKs are built by a manual `Release` workflow on a GitHub-hosted runner, not on the self-hosted runner, so the signing key is only decoded to a temporary file for the job and removed at the end.
- The key lives in four repository secrets. `app/build.gradle.kts` defines the `release` signing config only when `RELEASE_KEYSTORE_PATH` is set, so debug builds and pull requests need no secrets.
- The workflow attaches `Telepromptor.apk` to an existing tag only when a tag is entered, and replaces a file of that name, so a release is never changed by accident.
- The first release signed with this key cannot install over the debug-signed v1.1.0; users uninstall first. The keystore was generated in an AI session, so Rick may rotate it before relying on it long term.

## 2026-11-26 · Automatic releases and the in-app updater

Recorded by Claude under [issue #9](https://github.com/rickevans86/Teleprompter/issues/9), which applies Rick's release and self-update prompt.

- Every push to `main` publishes a release. `versionCode` is the workflow run number, so it must keep increasing: do not delete and recreate `release.yml`. Tags are `v1.1.<run number>`, so the tag's trailing number is the versionCode. The `1.1` prefix is `VERSION_PREFIX` in the workflow, chosen to continue from the existing v1.1.0 instead of the prompt's `1.0`.
- Pushes that change only `.ai/**` or Markdown are skipped, because the protocol commits shared records to `main` and each would otherwise publish a release.
- `main` jobs run on the self-hosted runner with labels `self-hosted`, `Linux`, `X64`; other branches use `ubuntu-latest` and never publish. Publishing uses the `softprops/action-gh-release@v2` action from the prompt.
- The workflow refuses to publish unless the APK certificate equals the keystore certificate, so a missing key cannot silently publish a debug-signed APK, even though local release builds fall back to the debug key.
- The existing secret names (`RELEASE_KEYSTORE_B64`, `RELEASE_KEYSTORE_PATH`) were kept instead of the prompt's `RELEASE_KEYSTORE_BASE64` and `RELEASE_KEYSTORE_FILE`, because the secrets already exist and were verified.
- Updates compare the release tag's trailing number with `BuildConfig.VERSION_CODE`. The startup check is skipped in debug builds, which are signed with a different key and would be offered an APK that cannot install over them.
- The first release published by this workflow is the first one signed with the release key, so it cannot install over the debug-signed v1.1.0 and installed copies must be uninstalled first.
