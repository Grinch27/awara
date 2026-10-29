> Historical staged upgrade record. Current modules and dependency policy are documented in [module-subscription-plan.md](module-subscription-plan.md); these old version numbers are not current pins.

# P3 AndroidX dependency upgrade — conservative stable lane

This stage upgrades AndroidX libraries without changing minSdk (26),
targetSdk (33), compileSdk (34), Kotlin, AGP, or the existing automated
latest stable Gradle refresh in GitHub Actions.

- Core KTX: 1.13.1 (app, player, search)
- Lifecycle runtime KTX: 2.8.4
- Activity Compose: 1.9.1
- WorkManager runtime KTX: 2.9.1
- Paging runtime + Paging Compose: 3.3.2 (removes alpha Paging Compose)
- ProfileInstaller: 1.3.1
- AndroidX test JUnit: 1.2.1 and Espresso Core: 3.6.1
- `:core:model` uses the same Compose BOM 2024.06.00 and no longer
  requests Compose runtime 1.4.2 explicitly.

Room 2.8.4 and Splashscreen 1.0.1 were not changed. SDK 35–37 and
more recent AndroidX versions belong in a separately tested toolchain
migration, not an unverified version sweep.

These upgrades require Debug/Release + R8 build checks and real-device
regression tests for notifications, downloads, lifecycle, paging/search,
permissions, rotation, cold start, and process recreation.

The ordinary GitHub Actions Debug lane now invokes `testDebugUnitTest` after APK
compilation, so JVM tests such as strict DoH configuration validation and
trusted-host checks must pass before artifacts are accepted. Instrumented
Android UI/device tests are **not** run by this workflow.
