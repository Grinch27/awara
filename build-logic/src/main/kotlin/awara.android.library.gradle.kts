// TODO(user): Decide whether published or reusable libraries should gain consumer ProGuard or resource prefix conventions here later.
// TODO(agent): If non-Android Kotlin modules are added, keep this convention Android-specific and add separate JVM/KMP plugins instead of overloading it.

import com.android.build.api.dsl.LibraryExtension
import me.rerere.awara.buildlogic.configureAwaraAndroidLibrary
import me.rerere.awara.buildlogic.configureAwaraKotlinJvm
import org.gradle.kotlin.dsl.configure

plugins {
    id("com.android.library")
}

extensions.configure<LibraryExtension> {
    configureAwaraAndroidLibrary()
}

configureAwaraKotlinJvm()