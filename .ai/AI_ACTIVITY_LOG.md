# AI activity log

## 2026-11-25 · Codex · Issue #1

- Inspected main, Issues, and open PRs. Found no shared coordination records or overlapping PRs.
- Created the six AI and status labels, opened and claimed issue #1, and prepared AGENTS.md and the six shared records.
- Ran eight JVM tests successfully. The UI test APK assembly, ktlint, and detekt checks succeeded using up-to-date outputs. UI tests were not run because this workspace has no connected Android device or hardware-enabled emulator.
- Submitted the setup changes for Rick to assign an independent reviewer. The branch is `codex/1-collaboration-protocol`.

[Issue #1](https://github.com/rickevans86/Teleprompter/issues/1)

## 2026-11-25 · Codex · Issue #1

- Applied Rick’s direct-to-main instruction to the repository protocol and updated the shared records. Preserved the setup history from PR #2.
- Rebuilt the task board from current GitHub Issues. Android tests were not rerun for these documentation changes.

## 2026-11-25 · Claude · Issues #3 and #4

- Diagnosed the failing Android CI from job logs. The self-hosted runner lacked `unzip` (fixed by Rick) and `setup-android` then failed requesting the removed `tools` package. GitHub-hosted runners had no `sdkmanager` because `setup-android` only ran on self-hosted runners.
- Issue #3: ran `setup-android` on every runner with `packages: platform-tools` (commit `4b5665f` on `claude/3-ci-sdk-setup`). The branch build and UI tests passed on re-run. The first attempt failed resolving the Kotlin Compose plugin and passed unchanged on retry, which suggests a transient network error.
- Issue #4: added a startup update check (commit `df79803` on `claude/4-startup-update-check`, stacked on #3). CI run 45613979770 passed: `build` (assembleDebug, unit tests including the new `UpdateCheckerTest`, androidTest assembly) and `ui-tests` (emulator).
- Rick attached the CI debug APK to the v1.1.0 draft as `Telepromptor.apk` and published it. Claude could not download the artifact from its sandbox and did not attach or publish the release.
- ktlint and detekt: Not run. This workspace has no Android SDK, and CI does not run them. No other agent's work overlapped.

[Issue #3](https://github.com/rickevans86/Teleprompter/issues/3) · [Issue #4](https://github.com/rickevans86/Teleprompter/issues/4)

## 2026-11-26 · Claude · Issue #7

- Rick asked Claude to generate the release keystore. Claude generated a PKCS12 keystore in its sandbox, sent it to Rick as files, and securely deleted its copies. Claude cannot write repository secrets (the Actions secrets API is blocked for its session), so Rick added `RELEASE_KEYSTORE_B64`, `RELEASE_KEYSTORE_PASSWORD`, `RELEASE_KEY_ALIAS` and `RELEASE_KEY_PASSWORD` himself. The key and passwords passed through the chat.
- Added `.github/workflows/release.yml` and an env-driven release signing config in `app/build.gradle.kts` (branch `claude/7-signed-release`, stacked on #3 and #4).
- Verified with a branch run (Release run 45715101100): it succeeded, `apksigner` verified the APK, and its certificate SHA-256 `02c07fb525ec627739518433aafeb45b8b23392ab6f3f902962be04b7cfe625e` matches the generated keystore. The release-attach step was skipped (no tag given), so attaching to a release is Not run. A temporary push trigger used for this was removed afterwards, so `workflow_dispatch` can only be run once the workflow is on `main`.
- No release was published or changed by Claude in this issue. v1.1.0 still carries the debug-signed APK.

[Issue #7](https://github.com/rickevans86/Teleprompter/issues/7)
