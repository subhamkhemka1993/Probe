import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.androidKotlinMultiplatformLibrary)
    alias(libs.plugins.androidLint)
    alias(libs.plugins.composeMultiplatform)
    alias(libs.plugins.composeCompiler)
}

kotlin {
    listOf(
        iosArm64(),
        iosSimulatorArm64(),
    ).forEach { iosTarget ->
        iosTarget.binaries.framework {
            baseName = "Shared"
            isStatic = true
        }
    }

    android {
        namespace = "com.dev.probe.shared"
        compileSdk =
            libs.versions.android.compileSdk
                .get()
                .toInt()
        minSdk =
            libs.versions.android.minSdk
                .get()
                .toInt()

        compilerOptions {
            jvmTarget = JvmTarget.JVM_11
        }
        androidResources {
            enable = true
        }
        withHostTest {
            isIncludeAndroidResources = true
        }
        withDeviceTestBuilder {
            sourceSetTreeName = "test"
        }.configure {
            instrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        }
        lint {
            xmlReport = true
            sarifReport = true
            checkDependencies = true
            disable += "GradleDependency"
            // Same reasoning as GradleDependency above: an advisory that a newer
            // Gradle/AGP is available, not a defect — bumping is a deliberate,
            // separately-verified decision, not something to nag about here.
            disable += "AndroidGradlePluginVersion"
            // Room's own generated Dao_Impl classes call Room-internal @RestrictTo
            // APIs; lint flags that cross-module even though it's Room's own code.
            disable += "RestrictedApi"
        }
    }

    sourceSets {
        androidMain.dependencies {
            implementation(libs.ui.tooling.preview)
            implementation(libs.compose.uiTooling)
        }
        commonMain.dependencies {
            implementation(libs.runtime)
            implementation(libs.foundation)
            implementation(libs.material3)
            implementation(libs.ui)
            implementation(libs.components.resources)
            implementation(libs.ui.tooling.preview)
            implementation(libs.androidx.lifecycle.viewmodelCompose)
            implementation(libs.androidx.lifecycle.runtimeCompose)
            api(projects.probeApi)
            api(projects.probeRuntime)
        }
        commonTest.dependencies {
            implementation(libs.kotlin.test)
        }
    }
}

dependencies {
    androidRuntimeClasspath(libs.compose.uiTooling)
}
