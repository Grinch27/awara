// TODO(user): Confirm whether preview-only libraries will ever need an explicit allowlist.
// TODO(agent): If a stable library requires a preview transitive, diagnose with dependencyInsight;
// never force an incompatible ABI or silently accept a preview version.
// Risk: newly published stable artifacts may expose previously hidden preview transitive requirements.
// Verify: Debug, Release and Docker Debug builds and resolved classpaths, without application tests.
// Resolve latest stable candidates during the build in every project, including transitive dependencies.
val prereleaseVersion = Regex(
    "(?i)(?:^|[._-])(?:alpha|beta|rc|cr|snapshot|preview|canary|dev|eap|milestone|m[0-9]+)(?:[._-]?[0-9]+)?(?=$|[._-])"
)
allprojects {
    configurations.configureEach {
        resolutionStrategy.componentSelection.all {
            if (prereleaseVersion.containsMatchIn(candidate.version)) {
                reject("Awara forbids prerelease artifacts: ${candidate.version}")
            }
        }
    }
}
