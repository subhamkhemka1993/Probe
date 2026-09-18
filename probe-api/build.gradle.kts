import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.androidKotlinMultiplatformLibrary)
    alias(libs.plugins.androidLint)
    alias(libs.plugins.vanniktechMavenPublish)
}

mavenPublishing {
    pom {
        description.set(
            "Always-present, dependency-light hooks (ProbeHttpCapture, ProbeState, ProbeConfig, " +
                "ProbeDatabaseCapture, ProbeDataStoreCapture, ProbeLogSink) for Probe's on-device debug tooling.",
        )
    }
}

kotlin {
    compilerOptions {
        freeCompilerArgs.add("-Xexpect-actual-classes")
    }

    android {
        namespace = "com.dev.probe.api"
        minSdk =
            libs.versions.android.minSdk
                .get()
                .toInt()
        compileSdk =
            libs.versions.android.compileSdk
                .get()
                .toInt()
        compilerOptions {
            jvmTarget.set(JvmTarget.JVM_17)
        }
        withHostTestBuilder {
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

    iosArm64()
    iosSimulatorArm64()

    sourceSets {
        commonMain.dependencies {
            implementation(libs.kotlin.stdlib)
            implementation(libs.kotlinx.coroutines.core)
            api(libs.ktor.client.core)
            implementation(libs.koin.core)
            api(libs.ui)
            api(libs.androidx.room.runtime)
        }
        commonTest.dependencies {
            implementation(libs.kotlin.test)
        }
        getByName("androidHostTest") {
            dependencies {
                implementation(libs.kotlin.testJunit)
                implementation(libs.junit)
                implementation(libs.robolectric)
                implementation(libs.androidx.core)
            }
        }
    }
}
