# Android runtime smoke tests

The standard `Build APK` and `Build APK (Docker)` workflows already build
Debug/Release artifacts, automatically update Gradle, and execute JVM tests.
They cannot establish Android runtime correctness.

The isolated `Android Emulator Smoke Tests` workflow tests Android API 34
with a Google APIs x86_64 image and `connectedDebugAndroidTest`, while
continuing to query the latest stable Gradle before the run. AndroidX
instrumentation also exercises the existing application-context smoke test.

`Media3NetworkSmokeTest` checks two bounded runtime behaviors:
1. A Media3 OkHttp-backed HTTP DataSource observes a strict DoH resolver and
   fails rather than querying a hostname with system DNS on invalid config.
2. A Media3 ExoPlayer instance prepares a local generated PCM WAV file without
   network access.

This is not a proof of packet-level DNS isolation, active-video CDN playback,
signed URL handling, HLS/DASH, caching/seek/quality switching, DLNA, or
navigation across real devices. Run those separately before production use.
The workflow retains test reports for seven days. Main/testing stay unchanged.
