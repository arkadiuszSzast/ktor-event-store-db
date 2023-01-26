val kotest_version: String by project
val strikt_version: String by project
val kotlin_logging_version: String by project
val coroutines_version: String by project
val kotlin_reflect: String by project

plugins {
    kotlin("jvm") version "1.8.0"
    `java-test-fixtures`
    application
}

group = "com.szastarek"
version = "0.0.1"

repositories {
    mavenCentral()
    mavenLocal()
}

dependencies {
    implementation("org.jetbrains.kotlin:kotlin-reflect:$kotlin_reflect")
    implementation("io.github.microutils:kotlin-logging-jvm:$kotlin_logging_version")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:$coroutines_version")

    testImplementation("io.kotest:kotest-runner-junit5:$kotest_version")
    testImplementation("io.strikt:strikt-core:$strikt_version")
}
