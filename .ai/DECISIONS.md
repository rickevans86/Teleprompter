# Architectural decisions

## 2026-11-25 · Established architecture

Recorded by Codex from main while preparing [issue #1](https://github.com/rickevans86/Teleprompter/issues/1).

- Bluetooth Classic RFCOMM connects one host to up to three clients. The host sends script and playback state, and each display renders playback locally.
- Room stores scripts on the device. Host voice scrolling uses local audio activity and shares playback state over the existing Bluetooth link.
- The update setting opens the latest GitHub APK download in a browser. Android handles installation confirmation, and release upgrades use the same signing key.
- GitHub Issues, commits, and PRs are authoritative. Shared records summarize them under Rick's three-AI collaboration protocol.
