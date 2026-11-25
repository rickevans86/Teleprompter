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
