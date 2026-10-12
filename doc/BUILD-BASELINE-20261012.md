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

Versions recorded when the baseline was built (historical reference, NOT a toolchain pinning policy):
- Gradle Wrapper: 9.5.0
- Android Gradle Plugin: 9.0.1
- Kotlin Gradle Plugin and Compose Compiler plugin: 2.3.20
- KSP: 2.3.7
- Java toolchain: 17 (CI executes Gradle on JDK 21)
- compileSdk 34, targetSdk 33, minSdk 26

Ongoing policy: keep the original automatic toolchain refresh behavior.
Both build workflows run `scripts/sync-gradle-wrapper.sh` immediately
before building; it queries `https://services.gradle.org/versions/current`
and selects the latest stable Gradle at CI runtime. Regular CI uses
`ubuntu-latest`; Docker CI uses `ubuntu:devel`. Do not remove this
automatic Gradle refresh simply to freeze an earlier successful build.

Scope note: AGP (9.0.1), Kotlin/Compose compiler plugin (2.3.20),
KSP (2.3.7), JDK (21 for CI), and Android SDK platforms still follow
the repository's existing explicitly declared versions. The existing
automation updates Gradle only, not those plugins or SDK targets.
If a newer Gradle is incompatible with them, report the incompatibility
rather than silently claiming that the entire toolchain is dynamically
upgraded.

Security note: the historical Docker workflow is privileged and uses
`ubuntu:devel`. Restore this only to honor the existing CI behavior;
runner/container privilege reduction remains a separate security task.

The records above prove that APK build jobs completed, not that on-device UI,
DoH failure isolation, media playback, DLNA, or permission flows passed.
