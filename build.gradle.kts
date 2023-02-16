val event_store_client: String by project
val ktor_version: String by project
val grpc_api: String by project

plugins {
    kotlin("jvm") version "1.8.0"
    `maven-publish`
}

group = "com.szastarek"
version = "0.0.2"

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
        create<MavenPublication>("kotlin-event-store-db") {
            from(components["kotlin"])
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
}
