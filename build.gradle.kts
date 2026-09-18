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
    alias(libs.plugins.spotless) apply false
    alias(libs.plugins.vanniktechMavenPublish) apply false
}

subprojects {
    // Set before any subproject's own build.gradle.kts (and therefore before
    // com.vanniktech.maven.publish's plugin-apply-time defaults) runs, so the artifact
    // coordinates it derives from project.group/project.name/project.version are correct
    // without :probe-api/:probe-runtime needing to call coordinates() themselves.
    group = providers.gradleProperty("GROUP").get()
    version = providers.gradleProperty("VERSION_NAME").get()

    apply(plugin = "com.diffplug.spotless")

    extensions.configure<com.diffplug.gradle.spotless.SpotlessExtension> {
        kotlin {
            target("src/**/*.kt")
            targetExclude("${layout.buildDirectory.get()}/**/*.kt")
            ktlint(libs.versions.ktlint.get())
        }
        kotlinGradle {
            target("*.gradle.kts")
            ktlint(libs.versions.ktlint.get())
        }
    }

    // Shared POM metadata for every module that opts into publishing (:probe-api,
    // :probe-runtime) by applying com.vanniktech.maven.publish — see docs/guide/publishing.md
    // for the one-time Sonatype/signing setup this depends on.
    plugins.withId("com.vanniktech.maven.publish") {
        extensions.configure<com.vanniktech.maven.publish.MavenPublishBaseExtension> {
            publishToMavenCentral()
            signAllPublications()
            pom {
                name.set(project.name)
                url.set("https://github.com/subhamkhemka1993/Probe")
                licenses {
                    license {
                        name.set("Apache-2.0")
                        url.set("https://www.apache.org/licenses/LICENSE-2.0")
                    }
                }
                developers {
                    developer {
                        id.set("subhamkhemka1993")
                        name.set("Subham Khemka")
                        url.set("https://github.com/subhamkhemka1993")
                    }
                }
                scm {
                    url.set("https://github.com/subhamkhemka1993/Probe")
                    connection.set("scm:git:git://github.com/subhamkhemka1993/Probe.git")
                    developerConnection.set("scm:git:ssh://git@github.com/subhamkhemka1993/Probe.git")
                }
            }
        }
    }
}
