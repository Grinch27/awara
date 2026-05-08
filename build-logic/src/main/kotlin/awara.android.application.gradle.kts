// TODO(user): Decide whether release signing or benchmark-specific settings should eventually live in separate application conventions.
// TODO(agent): If a second Android app module appears, split packaging defaults from app-specific runtime config instead of growing this plugin indiscriminately.

import com.android.build.api.dsl.ApplicationExtension
import me.rerere.awara.buildlogic.configureAwaraAndroidApplication
import me.rerere.awara.buildlogic.configureAwaraKotlinJvm
import org.gradle.kotlin.dsl.configure

plugins {
    id("com.android.application")
}

extensions.configure<ApplicationExtension> {
    configureAwaraAndroidApplication()
}

configureAwaraKotlinJvm()