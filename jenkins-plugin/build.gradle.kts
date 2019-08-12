import org.jenkinsci.gradle.plugins.jpi.JpiDeveloper
import org.jenkinsci.gradle.plugins.jpi.JpiExtension
import org.jenkinsci.gradle.plugins.jpi.JpiLicense
import org.jetbrains.kotlin.gradle.internal.KaptGenerateStubsTask
import org.jetbrains.kotlin.gradle.internal.KaptTask
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    kotlin("jvm")
    kotlin("kapt")
    id("org.jenkins-ci.jpi") version "0.33.0"
}

group = "com.vinted.automerger"
version = "0.5.1"

tasks {
    "test"(Test::class) {
        reports {
            html.isEnabled = true
        }
    }
}

jenkinsPlugin {
    coreVersion = "2.150.3"
    displayName = "Git Automerger Plugin"
    url = "https://github.com/jenkinsci/git-automerger-plugin"
    gitHubUrl = "https://github.com/jenkinsci/git-automerger-plugin"
    shortName = "git-automerger"
    description = "Tool for merging release branches into master."

    developers(closureOf<JpiExtension.Developers> {
        developer(closureOf<JpiDeveloper> {
            setProperty("id", "neworldlt")
            setProperty("name", "Andrius Semionovas")
            setProperty("email", "aneworld@gmail.com")
            setProperty("url", "https://github.com/neworld/")
            setProperty("organization", "Vinted UAB")
            setProperty("organizationUrl", "https://engineering.vinted.com/")
            setProperty("timezone", "Vilnius GMT+2")
        })
    })

    licenses(closureOf<JpiExtension.Licenses> {
        license(closureOf<JpiLicense> {
            setProperty("name", "MIT")
            setProperty("url", "https://raw.githubusercontent.com/jenkinsci/git-automerger/master/LICENSE")
            setProperty("distribution", "repo")
        })
    })
}

dependencies {
    implementation(project(":lib"))
    implementation(kotlin("stdlib-jdk8"))
    implementation("com.vinted:slf4j-streamadapter:1.0.0")

    // sezpoz is used to process extension annotations
    kapt("net.java.sezpoz:sezpoz:1.13")

    jenkinsTest("org.jenkins-ci.plugins:pipeline-utility-steps:2.3.0")
    jenkinsTest("org.jenkins-ci.plugins.workflow:workflow-job:2.33")
    jenkinsTest("org.jenkins-ci.plugins.workflow:workflow-cps:2.72")
    jenkinsTest("org.jenkins-ci.plugins.workflow:workflow-basic-steps:2.18")
    testImplementation("org.eclipse.jgit:org.eclipse.jgit:${Versions.jgit}")
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
