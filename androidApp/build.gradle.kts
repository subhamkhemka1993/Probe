import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.androidApplication)
    alias(libs.plugins.composeCompiler)
}

kotlin {
    compilerOptions {
        jvmTarget = JvmTarget.JVM_11
    }
}
dependencies {
    implementation(project(":shared"))

    implementation(libs.androidx.activity.compose)

    implementation(libs.ui.tooling.preview)
    debugImplementation(libs.compose.uiTooling)
}

android {
    namespace = "com.dev.probe"
    compileSdk =
        libs.versions.android.compileSdk
            .get()
            .toInt()

    defaultConfig {
        applicationId = "com.dev.probe"
        minSdk =
            libs.versions.android.minSdk
                .get()
                .toInt()
        targetSdk =
            libs.versions.android.targetSdk
                .get()
                .toInt()
        versionCode = 1
        versionName = "1.0"
    }
    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro",
            )
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    buildFeatures {
        compose = true
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
