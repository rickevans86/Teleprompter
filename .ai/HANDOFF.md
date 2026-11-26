# Handoff

**Agent** — Claude

**Completed** — Issue #9: Rick's "Android Release & Self-Update Setup" prompt applied to this repo (`claude/9-release-updater`, stacked on #3, #4 and #7). A push to `main` now builds and publishes a signed `Telepromptor.apk` under `v1.1.<run number>`, and Settings has an in-app **Check for updates** that downloads and installs it. Earlier in this work: issue #3 (CI SDK setup), issue #4 (startup update check, now reworked), issue #7 (manual signed release workflow, now replaced).

**In progress** — Nothing. No independent reviewer is named for #1, #3, #4, #7 or #9. Claude authored #3, #4, #7 and #9 and cannot review them.

**Branch/commit** — `claude/9-release-updater`, which contains the commits for #3, #4 and #7. Open PRs: #5, #6, #8 and the one linked from issue #9. None is merged. Merge in order: #5, #6, #8, then the #9 PR.

**Tests** — Verified on branch runs: ktlint and detekt on the whole project, unit tests (new `UpdateCheckerTest` and `UpdateFlowTest`), the Android build, the emulator UI tests (the Settings test now expects "Check for updates"), the signed release build with the APK certificate matching the keystore, and `versionCode` matching the run number. Not run: publishing a release (the publish step only runs from `main`, and was skipped on the branch), the first run of the workflow on the self-hosted runner, and downloading and installing an update on a phone.

**Blockers** — Rick must merge the PRs in order and name an independent reviewer. The first push to `main` after the merge publishes the first release signed with the release key. It cannot install over the debug-signed v1.1.0, so installed copies must be uninstalled first. The signing key was generated in an AI session and passed through the chat, so Rick may rotate it before relying on it.

**Next action** — After merging, watch the first `Release APK` run on the self-hosted runner (`gh run watch`). Check that the release `v1.1.<run number>` has `Telepromptor.apk`, install it, then push a second change and confirm **Check for updates** offers it. Consider running ktlint and detekt in the normal CI, since style problems were only found by a temporary step.

**Shared records updated** — Yes

**Remote synchronization** — Confirmed against `origin/main` at `835af38`. PRs #5, #6 and #8 were open and unmerged when this work started.
