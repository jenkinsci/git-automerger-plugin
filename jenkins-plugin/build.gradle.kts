import org.jetbrains.kotlin.gradle.internal.KaptGenerateStubsTask
import org.jetbrains.kotlin.gradle.internal.KaptTask
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    kotlin("jvm")
    kotlin("kapt")
    id("org.jenkins-ci.jpi") version "0.32.0"
}

group = "com.vinted.automerger"
version = "0.1"

tasks {
    "test"(Test::class) {
        useJUnitPlatform()
        reports {
            html.isEnabled = true
        }
    }
}

jenkinsPlugin {
    coreVersion = "2.164.3"
    displayName = "Git branches automerger plugin"
    url = "https://github.com/vinted/git-automerger"
    gitHubUrl = "https://github.com/vinted/git-automerger"
    shortName = "git-automerger"
}

dependencies {
    implementation(project(":lib"))
    implementation(kotlin("stdlib-jdk8"))
    implementation("com.vinted:slf4j-streamadapter:1.0.0")

    testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine:${Versions.junit5Version}")
    testImplementation("org.junit.jupiter:junit-jupiter-api:${Versions.junit5Version}")
    testImplementation("org.junit.jupiter:junit-jupiter-params:${Versions.junit5Version}")
    testImplementation("lt.neworld:kupiter:${Versions.kupiter}")

    // sezpoz is used to process extension annotations
    kapt("net.java.sezpoz:sezpoz:1.13")
}

java {
    sourceCompatibility = JavaVersion.VERSION_1_8
}

// Config was ported from https://github.com/jenkinsci/doktor-plugin/blob/master/build.gradle.kts
kapt {
    correctErrorTypes = true
}

tasks.withType(KotlinCompile::class.java).all {
    dependsOn("localizer")

    kotlinOptions {
        jvmTarget = "1.8"
    }
}

tasks.withType(KaptTask::class.java).all {
    outputs.upToDateWhen { false }
}

tasks.withType(KaptGenerateStubsTask::class.java).all {
    outputs.upToDateWhen { false }
}
