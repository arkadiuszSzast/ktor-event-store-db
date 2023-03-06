val event_store_client: String by project
val ktor_version: String by project
val grpc_api: String by project
val kotest_version: String by project
val strikt_version: String by project
val jackson_version: String by project
val coroutines_version: String by project

plugins {
    kotlin("jvm") version "1.8.0"
    `maven-publish`
    `java-test-fixtures`
}

group = "com.szastarek"
version = "0.0.9"

repositories {
    mavenCentral()
    maven("https://maven.pkg.github.com/arkadiuszSzast") {
        credentials {
            username = System.getenv("GITHUB_ACTOR")
            password = System.getenv("GITHUB_TOKEN")
        }
    }
}

publishing {
    publications {
        create<MavenPublication>("ktor-event-store-db") {
            from(components["java"])
        }
    }

    repositories {
        maven {
            name = "GitHubPackages"
            url = uri("https://maven.pkg.github.com/arkadiuszSzast/ktor-event-store-db")
            credentials {
                username = System.getenv("GITHUB_ACTOR")
                password = System.getenv("GITHUB_TOKEN")
            }
        }
    }
}

dependencies {
    implementation(kotlin("stdlib"))
    api("com.eventstore:db-client-java:$event_store_client")
    implementation("io.ktor:ktor-server-core:$ktor_version")
    implementation("io.grpc:grpc-api:$grpc_api")

    testImplementation("io.kotest:kotest-runner-junit5:$kotest_version")
    testImplementation("io.strikt:strikt-core:$strikt_version")
    testImplementation("com.fasterxml.jackson.core:jackson-core:$jackson_version")
    testImplementation("com.fasterxml.jackson.core:jackson-databind:$jackson_version")
    testImplementation("com.fasterxml.jackson.module:jackson-module-kotlin:$jackson_version")
    testImplementation("com.fasterxml.jackson.datatype:jackson-datatype-jsr310:$jackson_version")

    testFixturesImplementation("org.jetbrains.kotlin:kotlin-reflect:1.8.0")
    testFixturesImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:$coroutines_version")
    testFixturesImplementation("com.fasterxml.jackson.core:jackson-core:$jackson_version")
    testFixturesImplementation("com.fasterxml.jackson.core:jackson-databind:$jackson_version")
    testFixturesImplementation("com.fasterxml.jackson.module:jackson-module-kotlin:$jackson_version")
    testFixturesImplementation("com.fasterxml.jackson.datatype:jackson-datatype-jsr310:$jackson_version")
}
