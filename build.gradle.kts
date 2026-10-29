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
        // TODO(agent): Only filter APK dependency classpaths, not AGP/Kotlin compiler tool classpaths.
        // Risk: Kotlin and AGP internal tools legitimately request preview-classified dependencies.
        // Verify: no previews in app compile/runtime reports; Kotlin compiler configs still resolve.
        if (!name.endsWith("CompileClasspath", ignoreCase = true) &&
            !name.endsWith("RuntimeClasspath", ignoreCase = true)) {
            return@configureEach
        }
        // TODO(agent): Revisit these two exact transitive dependencies when upstream metadata stabilizes.
        // Risk: substituting a stable ABI for a preview dependency may expose runtime incompatibilities.
        // Verify: inspect resolved Kotlin/JetBrains Lifecycle versions and all three APK builds.
        // Koin 4.2.2 requests JetBrains Lifecycle beta while its stable release exists.
        // Kotlin stdlib preview can otherwise dominate version conflict selection.
        // The settings wrapper still requests legacy mmkv-static; use maintained 64-bit MMKV.
        // minSdk 26 and existing arm64-v8a/x86_64 ABIs are unchanged. Device migration remains to verify.
        resolutionStrategy.dependencySubstitution {
            substitute(module("com.tencent:mmkv-static")).using(module("com.tencent:mmkv:+"))
        }
        resolutionStrategy.eachDependency {
            val requestedVersion = requested.version
            val stableReplacement =
                (requested.group == "org.jetbrains.kotlin" && requested.name == "kotlin-stdlib") ||
                (requested.group == "org.jetbrains.androidx.lifecycle" &&
                    requested.name == "lifecycle-viewmodel-compose")
            if (stableReplacement && requestedVersion != null &&
                prereleaseVersion.containsMatchIn(requestedVersion)) {
                useVersion("+")
                because("Replace known preview transitive request with latest stable candidate")
            }
        }
        resolutionStrategy.componentSelection.all {
            if (prereleaseVersion.containsMatchIn(candidate.version)) {
                reject("Awara forbids prerelease artifacts: ${candidate.version}")
            }
        }
    }
}
