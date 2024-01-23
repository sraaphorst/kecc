plugins {
    kotlin("jvm") version "1.9.21"
}

group = "vorpal"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
}

dependencies {
    testImplementation("org.jetbrains.kotlin:kotlin-test")
    implementation("it.unich.jgmp:jgmp:1.0.1")
}

tasks.test {
    useJUnitPlatform()
}

kotlin {
    jvmToolchain(19)
}