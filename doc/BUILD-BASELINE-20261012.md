# Build baseline — 2026-10-12

The last build baseline before dependency upgrades is commit `7e0f3db8db92d6c48109f4e4985f3cd364d36bc0`.
The same commit was verified on `dev`, `main`, and copied to `testing`.
Do not modify `testing` as part of the dependency migration.

| Branch | Workflow | Outcome | Run |
| --- | --- | --- | --- |
| dev | Build APK (debug + release) | success | https://github.com/Grinch27/awara/actions/runs/38213837609 |
| dev | Build APK (Docker debug) | success | https://github.com/Grinch27/awara/actions/runs/38213837624 |
| main | Build APK (debug + release) | success | https://github.com/Grinch27/awara/actions/runs/38213856676 |
| main | Build APK (Docker debug) | success | https://github.com/Grinch27/awara/actions/runs/38213856638 |

Toolchain pinned by the repository at the baseline:
- Gradle Wrapper: 9.5.0
- Android Gradle Plugin: 9.0.1
- Kotlin Gradle Plugin and Compose Compiler plugin: 2.3.20
- KSP: 2.3.7
- Java toolchain: 17 (CI executes Gradle on JDK 21)
- compileSdk 34, targetSdk 33, minSdk 26

The update immediately after this snapshot removes automatic "latest stable"
Gradle Wrapper rewrites from both Actions workflows. Use
`scripts/sync-gradle-wrapper.sh` only as an explicitly reviewed manual
toolchain change; do not invoke it in CI during normal builds.

The records above prove that APK build jobs completed, not that on-device UI,
DoH failure isolation, media playback, DLNA, or permission flows passed.
