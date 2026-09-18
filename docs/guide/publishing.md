← [Guide index](README.md)

# Publishing to Maven Central

`:probe-api` and `:probe-runtime` both apply
[`com.vanniktech.maven.publish`](https://github.com/vanniktech/gradle-maven-publish-plugin),
configured for Maven Central. This page is the one-time setup a maintainer does *once per
Sonatype account*, plus the steps to cut an actual release. Nothing here has been run yet — no
version of Probe has been published — so treat every external step below as still to do.

## How the configuration is wired

- **Coordinates.** `group`/`version` come from `GROUP`/`VERSION_NAME` in the root
  [`gradle.properties`](../../gradle.properties), applied to every subproject *before* each
  module's own `build.gradle.kts` runs (see the `subprojects {}` block in the root
  [`build.gradle.kts`](../../build.gradle.kts)) — this lets the plugin's own defaults
  (`project.group` / `project.name` / `project.version`) produce the right artifact IDs
  (`probe-api`, `probe-runtime`) without either module calling `coordinates(...)` itself.
- **POM metadata** (license, developer, SCM URLs) is centralized in that same root
  `subprojects {}` block, under `plugins.withId("com.vanniktech.maven.publish") { ... }`, so both
  modules stay consistent automatically; only the per-module `description` differs, set locally in
  each module's `build.gradle.kts`.
- **Signing and Central credentials** are never hardcoded — the plugin reads them from Gradle
  properties/environment variables at publish time (see below). Not configuring them is safe:
  `./gradlew build`/`test`/`assemble` are entirely unaffected.

## One-time account setup (a human, not CI, does this)

1. Create an account at the [Sonatype Central Portal](https://central.sonatype.com) and verify
   ownership of the `com.dev.probe` namespace (Central now verifies namespace ownership instead
   of the legacy OSSRH ticket process — for a `com.dev.*` group this typically means proving
   control of a matching reverse-DNS domain, or publishing under a namespace tied to your GitHub
   account instead; see Central's own namespace docs for the current options).
2. Generate a User Token from the Central Portal (Account → Generate User Token). This gives a
   token username/password pair — **not** your login password — used by the plugin as
   `mavenCentralUsername` / `mavenCentralPassword`.
3. Generate a dedicated GPG key pair for signing releases (`gpg --full-generate-key`), then export
   the private key in the in-memory form the plugin expects:
   ```
   gpg --export-secret-keys --armor <key-id> | base64 | tr -d '\n'
   ```
4. Store these four values as **GitHub Actions repository secrets** (Settings → Secrets and
   variables → Actions), named exactly:
   - `ORG_GRADLE_PROJECT_mavenCentralUsername`
   - `ORG_GRADLE_PROJECT_mavenCentralPassword`
   - `ORG_GRADLE_PROJECT_signingInMemoryKey` (the base64 string from step 3)
   - `ORG_GRADLE_PROJECT_signingInMemoryKeyPassword` (the key's passphrase)

   The `ORG_GRADLE_PROJECT_` prefix is Gradle's own convention for turning an environment
   variable into a project property — this is how [`.github/workflows/release.yml`](../../.github/workflows/release.yml)
   hands the plugin its credentials without a line of Gradle config referencing secrets directly.

## Cutting a release

1. Bump `VERSION_NAME` in [`gradle.properties`](../../gradle.properties), commit it.
2. Push a tag matching `v*` (e.g. `v0.1.0`) — this triggers
   [`.github/workflows/release.yml`](../../.github/workflows/release.yml), which runs
   `./gradlew publishAllPublicationsToMavenCentralRepository` for both modules using the secrets
   above, then closes/releases the staging repository automatically
   (`publishToMavenCentral()` in the plugin config already implies auto-release; there's no
   separate manual "close the staging repo" step to perform on the Central Portal UI).
3. Sync propagation to Maven Central's public index typically takes anywhere from a few minutes
   to a couple of hours after the workflow succeeds.

## Verifying locally before a real release

`./gradlew :probe-api:publishToMavenLocal :probe-runtime:publishToMavenLocal` publishes both
modules to `~/.m2/repository` without touching Maven Central or requiring any of the secrets
above — useful for checking the generated POM/coordinates, or for a consumer project to depend on
a local build while iterating.

---
← [Guide index](README.md) · Previous: [Development](development.md)
