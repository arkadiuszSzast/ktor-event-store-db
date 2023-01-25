val kotest_version: String by project
val strikt_version: String by project
val kotlin_logging_version: String by project

plugins {
    kotlin("jvm") version "1.8.0"
    kotlin("plugin.serialization") version "1.8.0"
}

group = "com.szastarek"
version = "0.0.1"

repositories {
    mavenCentral()
}

dependencies {
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.4.1")
    implementation("org.jetbrains.kotlin:kotlin-reflect:1.8.0")
    implementation("io.github.microutils:kotlin-logging-jvm:$kotlin_logging_version")

    testImplementation("io.kotest:kotest-runner-junit5:$kotest_version")
    testImplementation("io.strikt:strikt-core:$strikt_version")
}