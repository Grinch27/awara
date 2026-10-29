> Historical staged upgrade record. Current modules and dependency policy are documented in [module-subscription-plan.md](module-subscription-plan.md); these old version numbers are not current pins.

# Compose BOM and official Navigation migration (P1-B / P1-C)

- Stable Compose BOM `2024.06.00` is used in `:app`, `:feature:player`, and
  `:feature:search`. Individual Compose UI, Material3, and icons dependencies
  no longer pin different versions. Android instrumentation tests use the
  same Compose BOM.
- This first upgrade lane intentionally selects Compose UI 1.6.8 and
  Material3 1.2.1 while preserving `compileSdk 34`, AGP 9.0.1 and the
  existing automatic **latest stable Gradle** workflow at CI runtime.
- `accompanist-navigation-animation` is removed; explicit
  `androidx.navigation:navigation-compose:2.7.7` replaces it. Existing
  `RouterActivity` already uses the official `NavHost`,
  `rememberNavController` and `composable` APIs, so no route graph rewrite
  is required.
- `accompanist-permissions` remains until its call sites receive separate
  UI-level tests. Other libraries (Activity, lifecycle, Paging) are
  deliberately deferred to P3.
- This is a staged compatibility choice, **not** a promise to stop upgrading
  Compose. The 2026.09 BOM requires a newer compileSdk/AGP combination and is
  tracked for a subsequent independent SDK-toolchain migration.
- GitHub Actions compile is not device UI testing. Validate phone/tablet
  layouts, transitions, back navigation, rotation, all search tabs, screen
  state recovery, dark theme and locales on actual devices.
