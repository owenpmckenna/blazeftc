// Top-level build file where you can add configuration options common to all sub-projects/modules.
plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.android.library) apply false
    alias(libs.plugins.kotlin.android) apply false
    id("io.github.gradle-nexus.publish-plugin") version "1.3.0"
}

nexusPublishing {
    repositories {
        sonatype {
            username = findProperty("sonatypeUsername") as String
            password = findProperty("sonatypePassword") as String
            //nexusUrl.set(uri("https://ossrh-staging-api.central.sonatype.com/service/local/staging/deploy/maven2/"))
            //snapshotRepositoryUrl.set(uri("https://central.sonatype.com/repository/maven-snapshots/"))
        }
    }
}