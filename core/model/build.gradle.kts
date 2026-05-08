// TODO(user): Decide whether core:model should stay Android-only for now or be downgraded to a pure JVM/KMP module later.
// TODO(agent): If more shared domain code lands here, extract common Android library conventions into build-logic instead of copying them per module.

plugins {
    id("com.android.library")
    kotlin("plugin.serialization")
}

android {
    namespace = "me.rerere.awara.core.model"
    compileSdk = 34

    defaultConfig {
        minSdk = 26
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
}

kotlin {
    jvmToolchain(17)
}

dependencies {
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.5.0")
}