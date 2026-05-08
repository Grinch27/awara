// TODO(user): Decide whether core:model should stay Android-only for now or be downgraded to a pure JVM/KMP module later.
// TODO(agent): If more shared domain code lands here, extract common Android library conventions into build-logic instead of copying them per module.

plugins {
    id("awara.android.library")
    id("awara.kotlin.serialization")
}

android {
    namespace = "me.rerere.awara.core.model"
}

dependencies {
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.5.0")
}