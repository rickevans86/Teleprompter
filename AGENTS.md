# Universal three-AI GitHub development protocol

## Project details

- **Purpose** — Telepromptor is an offline Android teleprompter. A host edits and plays scripts, and up to three Android display clients follow over Bluetooth Classic.
- **Stack** — Kotlin, Android Gradle Plugin, Jetpack Compose Material 3 and Navigation, Hilt, Room, coroutines, and kotlinx.serialization. The app targets Android API 37 and supports API 26 and later. Use JDK 17 or later and the checked-in Gradle wrapper.
- **Install** — Run `sdkmanager 'platforms;android-37.0' 'build-tools;36.0.0'`, then `./gradlew :app:installDebug` with an emulator or Android device connected by adb.
- **Run** — Run `adb shell am start -n dev.telepromptor/.MainActivity` after installation, or launch the app's `app` run configuration in Android Studio.
- **Test** — Run `./gradlew :app:testDebugUnitTest` for JVM tests and `./gradlew :app:connectedDebugAndroidTest` with an emulator or device for Compose UI tests. `./gradlew :app:assembleDebugAndroidTest` compiles the UI tests.
- **Lint and format** — Run `./gradlew :app:ktlintCheck :app:detekt` to check and `./gradlew :app:ktlintFormat` to format.

## Authority and roles

GitHub is the single source of truth for this collaboration among ChatGPT, Claude, and ChatGPT Codex. Rick assigns tasks by labeling Issues or directly in a session. Only Rick merges into main. Rick names the reviewer, and the agent who authored the change cannot review it.

| Agent | Default responsibilities |
| --- | --- |
| ChatGPT | Architecture, feature specifications, task creation, and prioritization |
| Claude | Independent review, code analysis, debugging, and architecture review |
| Codex | Implementation, refactoring, automated testing, feature branches, pull requests, and repository maintenance |

Any agent may perform an authorized task it has the tools to complete. Identify the actual agent responsible for each action.

## Shared memory

GitHub Issues are authoritative for assignments and status. Commits and pull requests are authoritative for implemented work. When a shared file conflicts with GitHub evidence, investigate and correct the file.

| File | Purpose |
| --- | --- |
| `.ai/PROJECT_STATE.md` | Current project overview |
| `.ai/TASK_BOARD.md` | Summary of active GitHub Issues |
| `.ai/AI_ACTIVITY_LOG.md` | Who completed each action |
| `.ai/HANDOFF.md` | Most recent handoff |
| `.ai/DECISIONS.md` | Architectural decisions |
| `.ai/FILE_OWNERSHIP.md` | Planned file changes for work in progress |

Record planned files when claiming a task and remove them in the PR that completes it. Refresh the rest of the shared records as the last step before opening a PR, after bringing the branch up to date with main. Preserve entries from other agents if the shared records conflict. The activity log and decisions are append-only. Add one dated entry at the end and preserve earlier entries and their order. Rebuild the task board from current GitHub Issues each time. Replace the handoff with the latest one and keep its history in the activity log.

## Session start

1. Identify yourself as ChatGPT, Claude, or Codex.
2. Check whether GitHub is accessible. If it is unavailable, use Website mode.
3. Retrieve the latest repository state. Read this file and the relevant `.ai/` files.
4. Review open Issues, active PRs, and recent commits. Identify other agents' completed work.
5. Check file ownership and open PR diffs for any overlap.
6. Confirm Rick's assignment. If there is none, recommend the next unclaimed task and wait for approval.

Use current repository evidence in each session.

## Tasks and ownership

Every task has a GitHub Issue with a task ID, description, acceptance criteria, responsible AI, status, dependencies, and related branch and PR. Use `ai:chatgpt`, `ai:claude`, `ai:codex`, `status:in-progress`, `status:blocked`, and `status:review`. An AI label records the agent regardless of which GitHub account applies it.

Work only on tasks Rick assigns. Before claiming an Issue, check for another agent's label, a file ownership entry, or an open PR affecting the same files. If you find another claim or overlap, stop and tell Rick. Apply your AI label and `status:in-progress`, leave a claim comment with the planned files, and re-read the Issue to confirm there is no competing claim. Propose a separate Issue for out-of-scope work. Ask Rick if ownership is ambiguous.

## Development workflow

1. Claim the assigned Issue and branch from current main. Use `<agent>/<issue-number>-<short-name>` and keep one Issue per branch.
2. Implement only the assigned changes. Run the project test and lint commands and verify the diff for scope and correctness.
3. Commit with the Issue and AI identity.
4. Bring the branch up to date with main, refresh the shared records, and open one reviewable PR.
5. Move the Issue to `status:review`. Request review from the independent reviewer Rick names. If none is named, ask Rick to name one.
6. Leave merging to Rick. Preserve history, avoid force-pushing shared branches, and only push to another agent's branch at Rick's request.

An implementation check is part of developing the change. Independent review is performed by someone other than the author.

Commit message example

```text
Add login endpoint (#42)

AI: Codex
```

## Reporting and handoff

Every development report identifies the AI, Issue, work performed, files changed, tests, commit SHA or PR, and outstanding issues. Write “Not run” with the reason for any omitted test. Claim only work and remote changes that you verified. Attribute work only to the agent who performed it.

At the end of the session, put the latest handoff in `.ai/HANDOFF.md` and include it in the PR description. Use these fields.

- **Agent**
- **Completed**
- **In progress**
- **Branch/PR**
- **Tests**
- **Blockers**
- **Next action**
- **Shared records updated**
- **Remote synchronization**

Use `Yes` or `No` for shared records and `Confirmed` or `Unavailable` for remote synchronization. Record a PR as pending when it has not yet been opened.

## Operating modes

**Codex mode** — Follow this file. Synchronize with GitHub, check Issues and open PRs, and carry out the assigned development workflow. Run the relevant tests and lint checks, commit and open the PR, and record the shared documentation, attribution, and handoff.

**Website mode** — If direct repository access is unavailable, say so at the start. Ask Rick for the minimum relevant files, Issue text, or shared records. State that your output has not been checked against the current repository. Provide complete files or a patch, a commit message, an activity entry, and handoff text for Rick or Codex to apply. Report remote writes only after they succeed.

## Bootstrap and safety

If the shared records are absent, set them up before feature work when Rick authorizes it. Fill in project details from the repository and tell Rick about anything undetermined. Create the agent and status labels when permission allows, or list the missing labels for Rick. Put the setup in one documentation-only PR and preserve all existing work. Start feature work only after coordination is established.

Keep credentials and secrets out of commits, Issues, PRs, and logs. If you find a committed secret, tell Rick immediately without repeating the value.
