# Android runtime acceptance scope

The current ordinary workflow builds Debug and Release and runs JVM regression tests in its Debug lane. The Docker workflow builds Debug. APK generation and JVM tests do not establish device runtime correctness.

The repository contains Media3NetworkSmokeTest and ExampleInstrumentedTest. The current main/dev workflows do not run connectedDebugAndroidTest; no new emulator/device success is claimed. Historical branch-specific emulator experiments are not this delivery evidence.

For this improvement, remaining runtime acceptance covers:

| Area | Necessary scenario | Status |
| --- | --- | --- |
| Material 3 density | narrow phone, tablet, large fonts, dark theme, 48 dp actions | Device verification pending |
| Session | legacy migration, lost key, expired login, logout during refresh | Mocked refresh tests; device verification pending |
| Search | all seven types, rapid switching, paging retry, process recreation | Request tests; device verification pending |
| Backup | preview merge/replace, interrupted transaction and next-launch recovery | Codec tests; runtime interruption verification pending |
| Playback | real CDN, quality/seek/speed, expired source, saved position, audio focus | Build only; device verification pending |
| Gallery | empty/error/retry, zoom versus swipe, restored page | Build only; device verification pending |
| Downloads | background notification, pause/resume/restart, full/partial/range rejection, low storage, export | Range-policy tests; real service/device verification pending |
| Local data | Room 1/2/6 to 7 on existing installs, missing files, bulk deletion | Explicit migration and compiler schema; device verification pending |
| Native dependency | existing MMKV preference migration and 64-bit devices | Build only; device verification pending |
| DLNA | discover, select, disconnect, dispose | Build only; real device verification pending |

Do not infer 16 KiB-device compatibility, packet-level DNS isolation, performance improvement percentages, production signing, or a production release from APK generation. Follow AGENTS.md minimal verification; do not add artifact hash/certificate/DEX scans.
