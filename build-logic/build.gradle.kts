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
    implementation("com.android.tools.build:gradle:9.0.1")
    implementation("org.jetbrains.kotlin:kotlin-gradle-plugin:2.3.20")
    implementation("com.google.devtools.ksp:com.google.devtools.ksp.gradle.plugin:2.3.7")
}