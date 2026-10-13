// TODO(user): Decide whether Detekt, Ktlint, or custom lint should join this build-logic layer in the next pass.
// TODO(agent): If convention coverage grows, split Android and Kotlin helpers into subpackages instead of keeping everything in one source set.

plugins {
    `kotlin-dsl`
}

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(17))
    }
}

dependencies {
    implementation("com.android.tools.build:gradle:latest.release")
    implementation("org.jetbrains.kotlin:kotlin-gradle-plugin:latest.release")
    implementation("org.jetbrains.kotlin.plugin.compose:org.jetbrains.kotlin.plugin.compose.gradle.plugin:latest.release")
    implementation("org.jetbrains.kotlin.plugin.serialization:org.jetbrains.kotlin.plugin.serialization.gradle.plugin:latest.release")
    implementation("com.google.devtools.ksp:com.google.devtools.ksp.gradle.plugin:latest.release")
}
// Resolve new releases on each build, but never opt into preview, RC or snapshots.
configurations.configureEach {
    resolutionStrategy.componentSelection.all {
        val unstable = Regex("(?i)(?:^|[.-])(?:alpha|beta|rc|cr|snapshot|preview|canary|dev|eap|milestone|m[0-9]+)(?:[.-]?[0-9]+)?(?:$|[.-])")
        if (unstable.containsMatchIn(candidate.version)) {
            reject("Awara uses stable releases only")
        }
    }
}

tasks.register("reportBuildPluginVersions") {
    group = "reporting"
    description = "Write the resolved Android, Kotlin and KSP plugin dependency versions."
    doLast {
        val versions = configurations.getByName("runtimeClasspath")
            .incoming.resolutionResult.allComponents
            .mapNotNull { it.id as? org.gradle.api.artifacts.component.ModuleComponentIdentifier }
            .map { "${it.group}:${it.module}:${it.version}" }
            .distinct().sorted()
        val report = layout.buildDirectory.file("reports/versions/build-plugins.txt").get().asFile
        report.parentFile.mkdirs()
        report.writeText(
            "gradle=${gradle.gradleVersion}\njava=${System.getProperty("java.version")}\n" +
                versions.joinToString(separator = "\n", postfix = "\n")
        )
    }
}
