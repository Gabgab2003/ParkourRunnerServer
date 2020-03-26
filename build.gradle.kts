plugins {
    kotlin("jvm") version "1.3.71"
    application
}

group = "net.downloadpizza"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
    jcenter()
}

dependencies {
    implementation(kotlin("stdlib-jdk8"))
    implementation("org.http4k", "http4k-core", "3.235.0")
    implementation("org.http4k", "http4k-server-jetty", "3.235.0")
    implementation("com.beust", "klaxon", "5.0.1")
    implementation("org.jetbrains.exposed", "exposed-core", "0.22.1")
    implementation("org.jetbrains.exposed", "exposed-dao", "0.22.1")
    implementation("org.jetbrains.exposed", "exposed-jdbc", "0.22.1")
    implementation("org.jetbrains.exposed", "exposed-java-time", "0.22.1")
    implementation("mysql", "mysql-connector-java", "8.0.19")
}

tasks {
    compileKotlin {
        kotlinOptions.jvmTarget = "1.8"
    }
    compileTestKotlin {
        kotlinOptions.jvmTarget = "1.8"
    }
}

application {
    mainClassName = "net.downloadpizza.prserver.ServerKt"
}
