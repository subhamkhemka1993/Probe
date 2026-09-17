# Probe Migration Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Port the existing, working `zdebug-api`/`zdevtools` module pair out of `zebpay_multiplatform` into this project (Probe) as `probe-api`/`probe-runtime`, renamed and version-aligned, with a working Android+iOS sample app that exercises the port end-to-end.

**Architecture:** Mechanical migration, not a rewrite. Copy each source module verbatim, then apply a deterministic rename script (package `com.zebpay.devtools` → `com.dev.probe`, symbol prefixes `ZDebug`/`ZTool` → `Probe`). Wire the ported modules into Probe's existing wizard scaffold (`shared`/`androidApp`/`iosApp`), repurposed as the integration sample.

**Tech Stack:** Kotlin Multiplatform 2.4.0, Compose Multiplatform 1.11.1, AGP 9.2.1, Ktor 3.5.0 (client+server), Koin 4.2.2, Room 2.8.4 + KSP 2.3.3, AndroidX DataStore 1.2.1, AndroidX Startup 1.2.0, moko-permissions 0.20.1.

**Spec:** `docs/superpowers/specs/2026-09-17-probe-migration-design.md`

## Global Constraints

- Source of truth for the pre-migration code: `/Users/dianapps/StudioProjects/zebpay_multiplatform`, branch `feat/zdevtools-inapp-debug-framework`, commit `4cbc38806` (PR #807, merged). Do not port anything from any other branch.
- Package namespace root is `com.dev.probe` (not `dev.probe`) — confirmed choice, matches the wizard's existing default.
- Every shared Gradle version aligns to `zebpay_multiplatform`'s pinned values (see Task 1's table) — this library is being built for that specific consumer.
- The archived DB/DataStore/Log capture spike (`feat/zdebug-runtime-state-capture` in `zebpay_multiplatform`) is explicitly **out of scope**. Do not port anything from that branch.
- `zdevtools/docs/runtime-state-capture-spec.md` and any `docs/superpowers/plans/*` from `zebpay_multiplatform` do not travel with the port — they document planning history for out-of-scope work.
- No AI attribution in any commit message.

---

### Task 1: Root Gradle Configuration

**Files:**
- Modify: `gradle/libs.versions.toml`
- Modify: `build.gradle.kts`

**Interfaces:**
- Produces: every version-catalog alias (`libs.plugins.*`, `libs.*`) that Tasks 2–6 reference by name. Getting an alias name wrong here is a compile failure two tasks later, so alias names below are copied verbatim from `zebpay_multiplatform`'s `gradle/libs.versions.toml` wherever ported source code will reference them, specifically so the copied `build.gradle.kts` files in Tasks 2–3 need no further hand-editing.

- [ ] **Step 1: Replace `gradle/libs.versions.toml` with the aligned + extended catalog**

```toml
[versions]
agp = "9.2.1"
android-compileSdk = "37"
android-minSdk = "28"
android-targetSdk = "37"
androidx-activity = "1.13.0"
androidx-appcompat = "1.8.0"
androidx-core = "1.19.0"
androidx-espresso = "3.7.0"
androidx-lifecycle = "2.10.0"
androidx-room = "2.8.4"
androidx-startup = "1.2.0"
androidx-testExt = "1.3.0"
composeMultiplatform = "1.11.1"
core = "1.7.0"
datastore = "1.2.1"
junit = "4.13.2"
koin = "4.2.2"
kotlin = "2.4.0"
kotlinStdlib = "2.4.0"
kotlinx-coroutines = "1.11.0"
kotlinx-datetime = "0.8.0"
kotlinx-serialization-json = "1.11.0"
ksp = "2.3.3"
ktor = "3.5.0"
material3 = "1.9.0"
materialIconsExtended = "1.7.3"
mokoPermission = "0.20.1"
robolectric = "4.16.1"
sqlite = "2.6.2"
uiTooling = "1.11.3"
uiToolingPreview = "1.11.1"

[libraries]
# --- wizard defaults (unchanged aliases, versions realigned above) ---
kotlin-test = { module = "org.jetbrains.kotlin:kotlin-test", version.ref = "kotlin" }
kotlin-testJunit = { module = "org.jetbrains.kotlin:kotlin-test-junit", version.ref = "kotlin" }
junit = { module = "junit:junit", version.ref = "junit" }
androidx-core-ktx = { module = "androidx.core:core-ktx", version.ref = "androidx-core" }
androidx-testExt-junit = { module = "androidx.test.ext:junit", version.ref = "androidx-testExt" }
androidx-espresso-core = { module = "androidx.test.espresso:espresso-core", version.ref = "androidx-espresso" }
androidx-appcompat = { module = "androidx.appcompat:appcompat", version.ref = "androidx-appcompat" }
androidx-activity-compose = { module = "androidx.activity:activity-compose", version.ref = "androidx-activity" }
compose-uiTooling = { module = "org.jetbrains.compose.ui:ui-tooling", version.ref = "composeMultiplatform" }
androidx-lifecycle-viewmodelCompose = { module = "org.jetbrains.androidx.lifecycle:lifecycle-viewmodel-compose", version.ref = "androidx-lifecycle" }
androidx-lifecycle-runtimeCompose = { module = "org.jetbrains.androidx.lifecycle:lifecycle-runtime-compose", version.ref = "androidx-lifecycle" }
compose-runtime = { module = "org.jetbrains.compose.runtime:runtime", version.ref = "composeMultiplatform" }
compose-foundation = { module = "org.jetbrains.compose.foundation:foundation", version.ref = "composeMultiplatform" }
compose-material3 = { module = "org.jetbrains.compose.material3:material3", version.ref = "material3" }
compose-ui = { module = "org.jetbrains.compose.ui:ui", version.ref = "composeMultiplatform" }
compose-components-resources = { module = "org.jetbrains.compose.components:components-resources", version.ref = "composeMultiplatform" }
compose-uiToolingPreview = { module = "org.jetbrains.compose.ui:ui-tooling-preview", version.ref = "composeMultiplatform" }

# --- new: bare aliases matching zebpay_multiplatform's naming, used verbatim by ported probe-api/probe-runtime source ---
kotlin-stdlib = { group = "org.jetbrains.kotlin", name = "kotlin-stdlib", version.ref = "kotlinStdlib" }
kotlinx-coroutines-core = { group = "org.jetbrains.kotlinx", name = "kotlinx-coroutines-core", version.ref = "kotlinx-coroutines" }
kotlinx-coroutines-test = { group = "org.jetbrains.kotlinx", name = "kotlinx-coroutines-test", version.ref = "kotlinx-coroutines" }
kotlinx-datetime = { group = "org.jetbrains.kotlinx", name = "kotlinx-datetime", version.ref = "kotlinx-datetime" }
kotlinx-serialization-json = { group = "org.jetbrains.kotlinx", name = "kotlinx-serialization-json", version.ref = "kotlinx-serialization-json" }
robolectric = { module = "org.robolectric:robolectric", version.ref = "robolectric" }
androidx-core = { group = "androidx.test", name = "core", version.ref = "core" }
androidx-ui-tooling = { group = "androidx.compose.ui", name = "ui-tooling", version.ref = "uiTooling" }
ui = { module = "org.jetbrains.compose.ui:ui", version.ref = "composeMultiplatform" }
ui-tooling-preview = { module = "org.jetbrains.compose.ui:ui-tooling-preview", version.ref = "uiToolingPreview" }
ui-backhandler = { module = "org.jetbrains.compose.ui:ui-backhandler", version.ref = "composeMultiplatform" }
runtime = { module = "org.jetbrains.compose.runtime:runtime", version.ref = "composeMultiplatform" }
foundation = { module = "org.jetbrains.compose.foundation:foundation", version.ref = "composeMultiplatform" }
material3 = { module = "org.jetbrains.compose.material3:material3", version.ref = "material3" }
material-icons-extended = { module = "org.jetbrains.compose.material:material-icons-extended", version.ref = "materialIconsExtended" }
components-resources = { module = "org.jetbrains.compose.components:components-resources", version.ref = "composeMultiplatform" }
koin-core = { group = "io.insert-koin", name = "koin-core", version.ref = "koin" }
ktor-client-core = { group = "io.ktor", name = "ktor-client-core", version.ref = "ktor" }
ktor-client-kotlinx-json = { group = "io.ktor", name = "ktor-serialization-kotlinx-json", version.ref = "ktor" }
ktor-client-engine-cio = { group = "io.ktor", name = "ktor-client-cio", version.ref = "ktor" }
ktor-client-mock = { group = "io.ktor", name = "ktor-client-mock", version.ref = "ktor" }
ktor-server-core = { group = "io.ktor", name = "ktor-server-core", version.ref = "ktor" }
ktor-server-cio = { group = "io.ktor", name = "ktor-server-cio", version.ref = "ktor" }
ktor-server-websockets = { group = "io.ktor", name = "ktor-server-websockets", version.ref = "ktor" }
ktor-server-cors = { group = "io.ktor", name = "ktor-server-cors", version.ref = "ktor" }
ktor-server-content-negotiation = { group = "io.ktor", name = "ktor-server-content-negotiation", version.ref = "ktor" }
androidx-startup-runtime = { module = "androidx.startup:startup-runtime", version.ref = "androidx-startup" }
androidx-room-runtime = { group = "androidx.room", name = "room-runtime", version.ref = "androidx-room" }
androidx-room-compiler = { group = "androidx.room", name = "room-compiler", version.ref = "androidx-room" }
sqlite-bundled = { module = "androidx.sqlite:sqlite-bundled", version.ref = "sqlite" }
androidx-datastore-preferences = { module = "androidx.datastore:datastore-preferences", version.ref = "datastore" }
moko-permission = { module = "dev.icerock.moko:permissions-compose", version.ref = "mokoPermission" }
moko-permission-camera = { module = "dev.icerock.moko:permissions-camera", version.ref = "mokoPermission" }
moko-permission-location = { module = "dev.icerock.moko:permissions-location", version.ref = "mokoPermission" }
moko-permission-notifications = { module = "dev.icerock.moko:permissions-notifications", version.ref = "mokoPermission" }

[plugins]
androidApplication = { id = "com.android.application", version.ref = "agp" }
androidKotlinMultiplatformLibrary = { id = "com.android.kotlin.multiplatform.library", version.ref = "agp" }
androidLint = { id = "com.android.lint", version.ref = "agp" }
composeMultiplatform = { id = "org.jetbrains.compose", version.ref = "composeMultiplatform" }
composeCompiler = { id = "org.jetbrains.kotlin.plugin.compose", version.ref = "kotlin" }
kotlinMultiplatform = { id = "org.jetbrains.kotlin.multiplatform", version.ref = "kotlin" }
kotlinSerialization = { id = "org.jetbrains.kotlin.plugin.serialization", version.ref = "kotlin" }
ksp = { id = "com.google.devtools.ksp", version.ref = "ksp" }
room = { id = "androidx.room", version.ref = "androidx-room" }
```

- [ ] **Step 2: Add the new plugin aliases to the root `build.gradle.kts` `apply false` block**

```kotlin
plugins {
    // this is necessary to avoid the plugins to be loaded multiple times
    // in each subproject's classloader
    alias(libs.plugins.androidApplication) apply false
    alias(libs.plugins.androidKotlinMultiplatformLibrary) apply false
    alias(libs.plugins.androidLint) apply false
    alias(libs.plugins.composeMultiplatform) apply false
    alias(libs.plugins.composeCompiler) apply false
    alias(libs.plugins.kotlinMultiplatform) apply false
    alias(libs.plugins.kotlinSerialization) apply false
    alias(libs.plugins.ksp) apply false
    alias(libs.plugins.room) apply false
}
```

- [ ] **Step 3: Verify the version bump alone doesn't break the existing wizard scaffold**

Run: `./gradlew :androidApp:assembleDebug`
Expected: `BUILD SUCCESSFUL` (confirms the Kotlin/Compose/AGP/material3/lifecycle downgrades are all mutually compatible before any new module is added).

- [ ] **Step 4: Commit**

```bash
git add gradle/libs.versions.toml build.gradle.kts
git commit -m "chore: align versions and add catalog entries for probe-api/probe-runtime"
```

---

### Task 2: Port `probe-api`

**Files:**
- Create: `scripts/port-module.sh`
- Create: `probe-api/**` (ported from `zdebug-api/**` in `zebpay_multiplatform`)
- Modify: `settings.gradle.kts`

**Interfaces:**
- Consumes: catalog aliases from Task 1 (`libs.plugins.kotlinMultiplatform`, `libs.plugins.androidKotlinMultiplatformLibrary`, `libs.plugins.androidLint`, `libs.kotlin.stdlib`, `libs.kotlinx.coroutines.core`, `libs.ktor.client.core`, `libs.koin.core`, `libs.ui`, `libs.kotlin.test`, `libs.kotlin.testJunit`, `libs.junit`, `libs.robolectric`, `libs.androidx.core`).
- Produces: module `:probe-api` with package `com.dev.probe.api`, exposing `ProbeHub`, `ProbeInstaller`, `ProbeState`, `ProbeHttpCapture`, `ProbePlatformContext`, `ProbeConfig`, `PROBE_NOTIFICATION_ID`, `ProbeInspector`, `HttpClientDebugHook`, `ProbeThemeOverride` — consumed by Task 3 as `projects.probeApi` and by Tasks 5–6 directly.

- [ ] **Step 1: Create the port script**

```bash
#!/usr/bin/env bash
# Mechanically ports a KMP module from zebpay_multiplatform into Probe, renaming
# packages/symbols per docs/superpowers/specs/2026-09-17-probe-migration-design.md.
# Usage: port-module.sh <source-module-dir> <dest-module-dir>
set -euo pipefail

SRC="$1"
DEST="$2"

rm -rf "$DEST"
cp -R "$SRC" "$DEST"

# zebpay-internal planning docs never travel with the port.
rm -rf "$DEST/docs"

# Relocate every "kotlin/com/zebpay/devtools" package directory to "kotlin/com/dev/probe",
# preserving everything nested underneath (api/, browser/, db/, ui/, etc.) in one move.
find "$DEST" -type d -path '*/kotlin/com/zebpay' | while read -r zebpay_dir; do
  kotlin_root=$(dirname "$zebpay_dir")
  mkdir -p "$kotlin_root/com/dev"
  mv "$zebpay_dir/devtools" "$kotlin_root/com/dev/probe"
  rm -rf "$zebpay_dir"
done

# Relocate the Room schema export directory (named after the DB class's FQN), if present.
if [ -d "$DEST/schemas/com.zebpay.devtools.db.ZDebugDatabase" ]; then
  mv "$DEST/schemas/com.zebpay.devtools.db.ZDebugDatabase" "$DEST/schemas/com.dev.probe.db.ProbeDatabase"
fi

# Rename Z-prefixed / zdebug_-prefixed file names (contents are fixed by the sed pass below).
find "$DEST" -type f \( -name 'ZDebug*' -o -name 'ZTool*' -o -name 'zdebug_*' \) | while read -r f; do
  dir=$(dirname "$f")
  base=$(basename "$f")
  newbase=$(printf '%s' "$base" | sed -e 's/^ZDebug/Probe/' -e 's/^ZTool/Probe/' -e 's/^zdebug_/probe_/')
  if [ "$base" != "$newbase" ]; then
    mv "$f" "$dir/$newbase"
  fi
done

# Content substitution across every text file the port touches.
find "$DEST" -type f \( -name '*.kt' -o -name '*.kts' -o -name '*.xml' -o -name '*.md' -o -name '*.js' -o -name '*.html' -o -name '*.css' -o -name '*.json' \) -print0 \
  | xargs -0 sed -i '' \
    -e 's/com\.zebpay\.devtools/com.dev.probe/g' \
    -e 's/ZDEBUG_/PROBE_/g' \
    -e 's/ZDebug/Probe/g' \
    -e 's/ZTool/Probe/g' \
    -e 's/zdebug_/probe_/g' \
    -e 's/zdebugApi/probeApi/g'
```

```bash
chmod +x scripts/port-module.sh
```

- [ ] **Step 2: Run it against `zdebug-api`**

```bash
./scripts/port-module.sh /Users/dianapps/StudioProjects/zebpay_multiplatform/zdebug-api probe-api
```

- [ ] **Step 3: Verify no residue and inspect the result**

```bash
grep -rl 'ZDebug\|ZTool\|com\.zebpay' probe-api || echo "clean"
find probe-api -type f | sort
```

Expected: `clean` printed; file list shows `probe-api/src/commonMain/kotlin/com/dev/probe/api/ProbeHub.kt`, `ProbeInstaller.kt`, `ProbeState.kt`, `ProbeHttpCapture.kt`, `ProbePlatformContext.kt`, `ProbeConfig.kt`, `ProbeNotificationId.kt`, `ProbeInspector.kt`, `HttpClientDebugHook.kt`, `ProbeThemeOverride.kt`, plus the platform variants and `androidHostTest`/`commonTest` files, all under `com/dev/probe/api/`.

- [ ] **Step 4: Add the module to the build**

```kotlin
// settings.gradle.kts
include(":androidApp")
include(":shared")
include(":probe-api")
```

- [ ] **Step 5: Compile and test**

Run: `./gradlew :probe-api:compileAndroidMain`
Expected: `BUILD SUCCESSFUL`

Run: `./gradlew test`
Expected: `BUILD SUCCESSFUL` — includes the ported `ProbeHubTest` and `ProbeInstallerTest` (renamed from `ZDebugHubTest`/`ZDebugInstallerTest`), passing unmodified in behavior.

- [ ] **Step 6: Commit**

```bash
git add scripts probe-api settings.gradle.kts
git commit -m "feat: port zdebug-api into probe-api"
```

---

### Task 3: Port `probe-runtime`

**Files:**
- Create: `probe-runtime/**` (ported from `zdevtools/**` in `zebpay_multiplatform`)
- Modify: `settings.gradle.kts`

**Interfaces:**
- Consumes: `projects.probeApi` (Task 2), plus every catalog alias added in Task 1.
- Produces: module `:probe-runtime` with package `com.dev.probe` (+ `.startup`, `.browser`, `.db`, `.devactions`, `.export`, `.internal`, `.network`, `.platform`, `.plugin`, `.policy`, `.prefs`, `.preview`, `.session`, `.shell`, `.theme`, `.ui` sub-packages), exposing `installProbeTools(config, platform): CoroutineScope` and `ProbeStartupInitializer` (Android App Startup `Initializer`) — consumed by Tasks 4–6.

- [ ] **Step 1: Run the port script against `zdevtools`**

```bash
./scripts/port-module.sh /Users/dianapps/StudioProjects/zebpay_multiplatform/zdevtools probe-runtime
```

- [ ] **Step 2: Verify no residue and inspect the result**

```bash
grep -rl 'ZDebug\|ZTool\|com\.zebpay' probe-runtime || echo "clean"
find probe-runtime -maxdepth 6 -type d | sort
cat probe-runtime/src/androidMain/AndroidManifest.xml
```

Expected: `clean` printed. The manifest's App Startup `<meta-data>` entry now reads `android:name="com.dev.probe.startup.ProbeStartupInitializer"`; the launcher `activity-alias` now points at `.shell.ProbeActivity` with `android:icon="@drawable/probe_icon"` and `android:label="Probe"`; the file provider now reads `.platform.ProbeFileProvider` / `@xml/probe_file_paths` / authority `${applicationId}.probe.fileprovider`.

- [ ] **Step 3: Add the module to the build**

```kotlin
// settings.gradle.kts
include(":androidApp")
include(":shared")
include(":probe-api")
include(":probe-runtime")
```

- [ ] **Step 4: Compile and test**

Run: `./gradlew :probe-runtime:compileAndroidMain`
Expected: `BUILD SUCCESSFUL` — this exercises the full plugin stack (Ktor server/client, Room+KSP, moko-permission, Compose MP, App Startup) resolving against Task 1's catalog.

Run: `./gradlew test`
Expected: `BUILD SUCCESSFUL` — the full ported `androidHostTest`/`commonTest` suite (network capture, browser server, session manager, theme, export, etc.) passes unmodified in behavior.

- [ ] **Step 5: Commit**

```bash
git add probe-runtime settings.gradle.kts
git commit -m "feat: port zdevtools into probe-runtime"
```

---

### Task 4: Shared Sample Glue

**Files:**
- Modify: `shared/build.gradle.kts`
- Modify: `shared/src/commonMain/kotlin/com/dev/probe/App.kt`
- Modify: `shared/src/iosMain/kotlin/com/dev/probe/MainViewController.kt`
- Create: `shared/src/iosMain/kotlin/com/dev/probe/ProbeBootstrap.kt`

**Interfaces:**
- Consumes: `com.dev.probe.api.ProbeHub.openHub(ProbePlatformContext)`, `com.dev.probe.api.ProbePlatformContext` (Task 2); `com.dev.probe.installProbeTools(config, platform)`, `com.dev.probe.api.ProbeConfig` (Task 3).
- Produces: `App(onOpenHub: () -> Unit)` composable (consumed by Task 5's `MainActivity`), `MainViewController(): UIViewController` (consumed by `ContentView.swift`, unchanged), `installProbeSample()` top-level function exposed to Swift as `ProbeBootstrapKt.installProbeSample()` (consumed by Task 6).

- [ ] **Step 1: Depend on the ported modules**

```kotlin
// shared/build.gradle.kts — inside sourceSets { commonMain.dependencies { ... } }, add:
        commonMain.dependencies {
            implementation(libs.compose.runtime)
            implementation(libs.compose.foundation)
            implementation(libs.compose.material3)
            implementation(libs.compose.ui)
            implementation(libs.compose.components.resources)
            implementation(libs.compose.uiToolingPreview)
            implementation(libs.androidx.lifecycle.viewmodelCompose)
            implementation(libs.androidx.lifecycle.runtimeCompose)
            api(projects.probeApi)
            api(projects.probeRuntime)
        }
```

- [ ] **Step 2: Replace the Greeting demo with an "open hub" button**

```kotlin
// shared/src/commonMain/kotlin/com/dev/probe/App.kt
package com.dev.probe

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.safeContentPadding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview

@Composable
@Preview
fun App(onOpenHub: () -> Unit = {}) {
    MaterialTheme {
        Column(
            modifier = Modifier.fillMaxSize().safeContentPadding(),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Button(onClick = onOpenHub) {
                Text("Open Probe Hub")
            }
        }
    }
}
```

- [ ] **Step 3: Wire the iOS view controller to open the hub**

```kotlin
// shared/src/iosMain/kotlin/com/dev/probe/MainViewController.kt
package com.dev.probe

import androidx.compose.ui.window.ComposeUIViewController
import com.dev.probe.api.ProbeHub
import com.dev.probe.api.ProbePlatformContext

fun MainViewController() = ComposeUIViewController {
    App(onOpenHub = { ProbeHub.openHub(ProbePlatformContext()) })
}
```

- [ ] **Step 4: Add the iOS install entry point**

```kotlin
// shared/src/iosMain/kotlin/com/dev/probe/ProbeBootstrap.kt
package com.dev.probe

import com.dev.probe.api.ProbeConfig
import com.dev.probe.api.ProbePlatformContext

fun installProbeSample() {
    installProbeTools(
        config = ProbeConfig(isEnabled = { true }),
        platform = ProbePlatformContext(),
    )
}
```

- [ ] **Step 5: Verify**

Run: `./gradlew :shared:compileKotlinIosSimulatorArm64 :shared:compileDebugKotlinAndroid`
Expected: `BUILD SUCCESSFUL`

- [ ] **Step 6: Commit**

```bash
git add shared
git commit -m "feat: wire shared sample UI to probe-api/probe-runtime"
```

---

### Task 5: Android Sample Wiring

**Files:**
- Modify: `androidApp/src/main/kotlin/com/dev/probe/MainActivity.kt`

**Interfaces:**
- Consumes: `App(onOpenHub: () -> Unit)` (Task 4), `com.dev.probe.api.ProbeInstaller.install(config, platform)`, `ProbeConfig`, `ProbeHub.openHub(ProbePlatformContext)`, `ProbePlatformContext(context: Context)` (Task 2). `androidApp` already sees these transitively through `:shared`'s `api(...)` dependency from Task 4 — no `androidApp/build.gradle.kts` change needed.

- [ ] **Step 1: Install on launch and open the hub from the button**

```kotlin
package com.dev.probe

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import com.dev.probe.api.ProbeConfig
import com.dev.probe.api.ProbeHub
import com.dev.probe.api.ProbeInstaller
import com.dev.probe.api.ProbePlatformContext

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)

        ProbeInstaller.install(
            config = ProbeConfig(isEnabled = { true }),
            platform = ProbePlatformContext(this),
        )

        setContent {
            App(onOpenHub = { ProbeHub.openHub(ProbePlatformContext(this)) })
        }
    }
}

@Preview
@Composable
fun AppAndroidPreview() {
    App(onOpenHub = {})
}
```

- [ ] **Step 2: Verify**

Run: `./gradlew :androidApp:assembleDebug`
Expected: `BUILD SUCCESSFUL`

Run: `adb install -r androidApp/build/outputs/apk/debug/androidApp-debug.apk && adb shell am start -n com.dev.probe/.MainActivity` (best-effort — requires a connected device/emulator)
Expected: app launches; tapping "Open Probe Hub" opens the Probe debug hub.

- [ ] **Step 3: Commit**

```bash
git add androidApp
git commit -m "feat: self-install probe-runtime from the android sample app"
```

---

### Task 6: iOS Sample Wiring

**Files:**
- Modify: `iosApp/iosApp/iOSApp.swift`

**Interfaces:**
- Consumes: `ProbeBootstrapKt.installProbeSample()` (Task 4, exposed through the `Shared` framework).

- [ ] **Step 1: Install at the same point in the app lifecycle production code uses**

```swift
import SwiftUI
import Shared

@main
struct iOSApp: App {
    init() {
        ProbeBootstrapKt.installProbeSample()
    }

    var body: some Scene {
        WindowGroup {
            ContentView()
        }
    }
}
```

- [ ] **Step 2: Verify (best-effort — no iOS CI in this project)**

Open `iosApp/iosApp.xcodeproj` in Xcode, select the `iosApp` scheme, run on a simulator.
Expected: app launches; tapping "Open Probe Hub" opens the Probe debug hub.

- [ ] **Step 3: Commit**

```bash
git add iosApp
git commit -m "feat: install probe-runtime from the ios sample app"
```

---

### Task 7: Final Verification

**Files:** none (verification only; fix inline if anything below fails, then re-run this task's steps)

- [ ] **Step 1: Full test suite**

Run: `./gradlew test`
Expected: `BUILD SUCCESSFUL`

- [ ] **Step 2: Full compile across both platforms' relevant targets**

Run: `./gradlew :probe-api:compileAndroidMain :probe-runtime:compileAndroidMain :shared:compileDebugKotlinAndroid :androidApp:assembleDebug`
Expected: `BUILD SUCCESSFUL`

- [ ] **Step 3: Residue check across the whole project**

```bash
grep -rl 'com\.zebpay\|ZDebug\|ZTool' probe-api probe-runtime shared androidApp iosApp || echo "clean: no zebpay/ZDebug/ZTool residue in source"
```

Expected: `clean: no zebpay/ZDebug/ZTool residue in source` (the design spec and this plan document itself, under `docs/`, legitimately reference `zebpay_multiplatform` as the migration source and are excluded from this check).

- [ ] **Step 4: iOS build (best-effort)**

Open `iosApp/iosApp.xcodeproj` in Xcode, build the `iosApp` scheme for a simulator.
Expected: build succeeds.

- [ ] **Step 5: Report**

No commit for this task unless Step 3 found residue that needed fixing — if it did, commit that fix, then report the fix separately from a "no changes" verification pass.
