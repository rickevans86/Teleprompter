# Universal three-AI GitHub development protocol

## Project details

- **Purpose** — Telepromptor is an offline Android teleprompter. A host edits and plays scripts, and up to three Android display clients follow over Bluetooth Classic.
- **Stack** — Kotlin, Android Gradle Plugin, Jetpack Compose Material 3 and Navigation, Hilt, Room, coroutines, and kotlinx.serialization. The app targets Android API 37 and supports API 26 and later. Use JDK 17 or later and the checked-in Gradle wrapper.
- **Install** — Run `sdkmanager 'platforms;android-37.0' 'build-tools;36.0.0'`, then `./gradlew :app:installDebug` with an emulator or Android device connected by adb.
- **Run** — Run `adb shell am start -n dev.telepromptor/.MainActivity` after installation, or launch the app's `app` run configuration in Android Studio.
- **Test** — Run `./gradlew :app:testDebugUnitTest` for JVM tests and `./gradlew :app:connectedDebugAndroidTest` with an emulator or device for Compose UI tests. `./gradlew :app:assembleDebugAndroidTest` compiles the UI tests.
- **Lint and format** — Run `./gradlew :app:ktlintCheck :app:detekt` to check and `./gradlew :app:ktlintFormat` to format.

## Authority and roles

GitHub is the single source of truth for this collaboration among ChatGPT, Claude, and ChatGPT Codex. Rick assigns tasks by labeling Issues or directly in a session. On Rick’s instruction, authorized work is committed and pushed directly to `main`. Rick names the independent reviewer. The agent who authored the change cannot review it.

| Agent | Default responsibilities |
| --- | --- |
| ChatGPT | Architecture, feature specifications, task creation, and prioritization |
| Claude | Independent review, code analysis, debugging, and architecture review |
| Codex | Implementation, refactoring, automated testing, feature branches, pull requests, and repository maintenance |

Any agent may perform an authorized task it has the tools to complete. Identify the actual agent responsible for each action.

## Shared memory

GitHub Issues are authoritative for assignments and status. Commits on `main` are authoritative for implemented work. Existing pull requests record earlier work. When a shared file conflicts with GitHub evidence, investigate and correct the file.

| File | Purpose |
| --- | --- |
| `.ai/PROJECT_STATE.md` | Current project overview |
| `.ai/TASK_BOARD.md` | Summary of active GitHub Issues |
| `.ai/AI_ACTIVITY_LOG.md` | Who completed each action |
| `.ai/HANDOFF.md` | Most recent handoff |
| `.ai/DECISIONS.md` | Architectural decisions |
| `.ai/FILE_OWNERSHIP.md` | Planned file changes for work in progress |

Record planned files when claiming a task and remove them in the commit that completes it. Refresh the shared records as the last step before committing and pushing, after synchronizing with the latest `main`. Preserve entries from other agents if the shared records conflict. The activity log and decisions are append-only. Add one dated entry at the end and preserve earlier entries and their order. Rebuild the task board from current GitHub Issues each time. Replace the handoff with the latest one and keep its history in the activity log.

## Session start

1. Identify yourself as ChatGPT, Claude, or Codex.
2. Check whether GitHub is accessible. If it is unavailable, use Website mode.
3. Retrieve the latest repository state. Read this file and the relevant `.ai/` files.
4. Review open Issues, active PRs, and recent commits. Identify other agents' completed work.
5. Check file ownership and open PR diffs for any overlap.
6. Confirm Rick's assignment. If there is none, recommend the next unclaimed task and wait for approval.

Use current repository evidence in each session.

## Tasks and ownership

Every task has a GitHub Issue with a task ID, description, acceptance criteria, responsible AI, status, dependencies, and related commits on `main`. Use `ai:chatgpt`, `ai:claude`, `ai:codex`, `status:in-progress`, `status:blocked`, and `status:review`. An AI label records the agent regardless of which GitHub account applies it.

Work only on tasks Rick assigns. Before claiming an Issue, check for another agent's label, a file ownership entry, or an open PR affecting the same files. If you find another claim or overlap, stop and tell Rick. Apply your AI label and `status:in-progress`, leave a claim comment with the planned files, and re-read the Issue to confirm there is no competing claim. Propose a separate Issue for out-of-scope work. Ask Rick if ownership is ambiguous.

## Development workflow

1. Claim the assigned Issue and fast-forward your local `main` to `origin/main`. Check other agents’ file claims before editing.
2. Implement only the assigned changes. Run the project test and lint commands and verify the diff for scope and correctness.
3. Fetch `main` again. Integrate any intervening work without dropping another agent’s changes. Stop and coordinate with Rick if the changes overlap.
4. Update the shared records, commit with the Issue and AI identity, and push directly to `main`. Use a regular push. If another push moved `main`, fetch and synchronize again.
5. Link the commit from the Issue and move it to `status:review`. Request review on the Issue from the independent reviewer Rick names. If none is named, ask Rick to name one.

An implementation check is part of developing the change. Independent review is performed by someone other than the author.

Commit message example

```text
Add login endpoint (#42)

AI: Codex
```

## Reporting and handoff

Every development report identifies the AI, Issue, work performed, files changed, tests, commit SHA or PR, and outstanding issues. Write “Not run” with the reason for any omitted test. Claim only work and remote changes that you verified. Attribute work only to the agent who performed it.

At the end of the session, put the latest handoff in `.ai/HANDOFF.md` and link to it and the commit from the Issue. Use these fields.

- **Agent**
- **Completed**
- **In progress**
- **Branch/commit**
- **Tests**
- **Blockers**
- **Next action**
- **Shared records updated**
- **Remote synchronization**

Use `Yes` or `No` for shared records and `Confirmed` or `Unavailable` for remote synchronization. If the commit is pending, record `main` and the linked Issue in the handoff, then link the verified commit from the Issue after pushing.

## Operating modes

**Codex mode** — Follow this file. Synchronize with GitHub, check Issues and open PRs, and carry out the assigned development workflow. Run the relevant tests and lint checks, record the shared documentation, attribution, and handoff, and commit and push the assigned work directly to `main`.

**Website mode** — If direct repository access is unavailable, say so at the start. Ask Rick for the minimum relevant files, Issue text, or shared records. State that your output has not been checked against the current repository. Provide complete files or a patch, a commit message, an activity entry, and handoff text for Rick or Codex to apply. Report remote writes only after they succeed.

## Bootstrap and safety

If the shared records are absent, set them up before feature work when Rick authorizes it. Fill in project details from the repository and tell Rick about anything undetermined. Create the agent and status labels when permission allows, or list the missing labels for Rick. Keep the setup documentation-only on `main` and preserve all existing work. Start feature work only after coordination is established.

Keep credentials and secrets out of commits, Issues, PRs, and logs. If you find a committed secret, tell Rick immediately without repeating the value.
