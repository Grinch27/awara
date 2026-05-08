package me.rerere.awara.buildlogic

// TODO(user): Decide whether targetSdk should stay in the application convention or be promoted to a shared constant once multiple apps exist.
// TODO(agent): If library modules start needing resource prefixes or publish settings, extend the convention helpers here instead of duplicating per-module config.

import com.android.build.api.dsl.ApplicationExtension
import com.android.build.api.dsl.CommonExtension
import com.android.build.api.dsl.LibraryExtension
import org.gradle.api.JavaVersion
import org.gradle.api.Project
import org.jetbrains.kotlin.gradle.dsl.KotlinTopLevelExtension

private const val COMPILE_SDK = 34
private const val MIN_SDK = 26
private const val TARGET_SDK = 33
private const val JVM_VERSION = 17

internal fun CommonExtension<*, *, *, *, *, *>.configureCommonAndroid() {
    compileSdk = COMPILE_SDK

    defaultConfig {
        minSdk = MIN_SDK
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
}

internal fun ApplicationExtension.configureAwaraAndroidApplication() {
    configureCommonAndroid()

    defaultConfig {
        targetSdk = TARGET_SDK

        vectorDrawables {
            useSupportLibrary = true
        }
    }

    buildFeatures {
        buildConfig = true
    }

    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
}

internal fun LibraryExtension.configureAwaraAndroidLibrary() {
    configureCommonAndroid()
}

internal fun Project.configureAwaraKotlinJvm() {
    extensions.findByType(KotlinTopLevelExtension::class.java)?.jvmToolchain(JVM_VERSION)
}

internal fun Project.configureAwaraComposeCompiler() {
    extensions.findByType(KotlinTopLevelExtension::class.java)?.compilerOptions?.optIn?.addAll(
        "androidx.compose.material3.ExperimentalMaterial3Api",
        "androidx.compose.material.ExperimentalMaterialApi",
        "androidx.compose.animation.ExperimentalAnimationApi",
        "androidx.compose.foundation.ExperimentalFoundationApi",
        "com.google.accompanist.pager.ExperimentalPagerApi",
        "coil.annotation.ExperimentalCoilApi",
        "androidx.compose.material3.windowsizeclass.ExperimentalMaterial3WindowSizeClassApi",
    )
}