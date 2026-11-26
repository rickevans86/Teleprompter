# Project state

Snapshot on 2026-11-25 from `main` at 835af38 plus the open branches below.

- Telepromptor is an Android app with an offline script library, Bluetooth Classic host and client modes, synchronized playback, and optional host voice scrolling.
- On `main`, Settings displays the installed version and opens the latest GitHub release APK in a browser, and the source identifies version 1.1.0. The open branches below replace this with an in-app updater and CI-derived versions.
- v1.1.0 is published as the latest release with a debug-signed `Telepromptor.apk`. `main` CI fails at the Android SDK setup step until `claude/3-ci-sdk-setup` (issue #3) is merged; that branch builds and passes UI tests on GitHub-hosted runners.
- `claude/4-startup-update-check` (issue #4) adds a startup update-check banner and the `INTERNET` permission. It is not on `main`.
- `claude/7-signed-release` (issue #7) adds a manual `Release` workflow that builds a signed `Telepromptor.apk` from repository secrets. A branch run built and verified it. It is not on `main`, and the release signing key has not yet been used for a published release.
- `claude/9-release-updater` (issue #9, stacked on #3, #4 and #7) applies Rick's release and self-update prompt: a signed APK is published on every push to `main` (skipping `.ai/**` and Markdown) under `v1.1.<run number>`, versions come from CI, and Settings has an in-app **Check for updates** that downloads and installs the APK. Its checks passed on a branch; nothing has been published by it, and it is not on `main`.
- The original app work through 6c3c251 is attributed to Codex in Git history. No work by ChatGPT or Claude was identified in the inspected history.
- Project commands and collaboration rules are in [AGENTS.md](../AGENTS.md). Current assignments are tracked by GitHub Issues. On Rick’s instruction, authorized changes and shared records are committed directly to `main`.

[Passing branch run for issue #4](https://github.com/rickevans86/Teleprompter/actions/runs/45613979770) · [Signed release run for issue #7](https://github.com/rickevans86/Teleprompter/actions/runs/45715101100) · [Release APK run for issue #9](https://github.com/rickevans86/Teleprompter/actions/runs/45758170297)
