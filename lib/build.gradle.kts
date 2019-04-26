plugins {
    kotlin("jvm")
    kotlin("kapt")
}

group = "com.vinted.automerger"
version = "0.1"

tasks {
    "test"(Test::class) {
        useJUnitPlatform()
        reports {
            html.isEnabled = true
        }
        jvmArgs("-Dorg.slf4j.simpleLogger.defaultLogLevel=debug")
    }
}

dependencies {
    implementation(kotlin("stdlib-jdk8"))
    implementation("org.eclipse.jgit:org.eclipse.jgit:${Versions.jgit}")
    implementation("org.apache.maven:maven-artifact:3.6.1")
    implementation("org.slf4j:slf4j-api:${Versions.slf4j}")
    runtimeOnly("org.slf4j:slf4j-simple:${Versions.slf4j}")

    testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine:${Versions.junit5Version}")
    testImplementation("org.junit.jupiter:junit-jupiter-api:${Versions.junit5Version}")
    testImplementation("org.junit.jupiter:junit-jupiter-params:${Versions.junit5Version}")
    testImplementation("lt.neworld:kupiter:${Versions.kupiter}")
    testImplementation("com.google.auto.service:auto-service:1.0-rc5")
    kaptTest("com.google.auto.service:auto-service:1.0-rc5")
}
