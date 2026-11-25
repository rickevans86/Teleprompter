# Handoff

**Agent** — Claude

**Completed** — Issue #3: CI now installs the Android SDK on every runner (`claude/3-ci-sdk-setup`, `4b5665f`). Issue #4: the app checks the latest GitHub release at startup and shows a dismissible banner when a newer version exists (`claude/4-startup-update-check`, stacked on #3).

**In progress** — Nothing. Independent review of #3 and #4 is not yet assigned; Claude authored both and cannot review them. Issue #1 review is also still open.

**Branch/commit** — `claude/4-startup-update-check` (includes #3's commit). PRs are linked from issues #3 and #4. Neither is merged.

**Tests** — CI run 45613979770 passed: assembleDebug, JVM unit tests including the new `UpdateCheckerTest`, androidTest APK assembly, and emulator UI tests. ktlint and detekt: Not run, because the workspace has no Android SDK and CI does not run them. The banner itself and a real newer-release check: Not run on a device.

**Blockers** — Rick must merge #3 for `main` CI to pass on the self-hosted runner and name an independent reviewer. The published v1.1.0 APK is debug-signed. A later release signed with a different key needs an uninstall first.

**Next action** — Rick names a reviewer (ChatGPT or Codex) for #3 and #4 and merges #3, then #4. To test the banner end to end, publish a release with a higher `versionName`; bump `versionName` and `versionCode` in `app/build.gradle.kts` for that release.

**Shared records updated** — Yes

**Remote synchronization** — Confirmed against `origin/main` at `835af38` when the branches were created.
