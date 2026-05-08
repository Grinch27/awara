// TODO(user): Decide whether core:model should stay Android-only for now or be downgraded to a pure JVM/KMP module later.
// TODO(agent): If more shared domain code lands here, extract common Android library conventions into build-logic instead of copying them per module.

plugins {
    id("com.android.library")
    kotlin("plugin.serialization")
}

android {
    id("awara.android.library")
    id("awara.kotlin.serialization")

    defaultConfig {
        minSdk = 26
    }