plugins {
    kotlin("jvm") version "2.2.21"
    kotlin("plugin.serialization") version "2.2.21"
    id("org.graalvm.buildtools.native") version "0.10.2"
    application
}

group = "io.github.alelk.apps.challengetgbot"
version = "1.0"

repositories { mavenCentral() }

val exposedVersion = "1.0.0-rc-4"

dependencies {
    // Telegram Bot API
    implementation("dev.inmo:tgbotapi:18.2.1") // https://central.sonatype.com/artifact/dev.inmo/tgbotapi

    // Configuration
    implementation("com.sksamuel.hoplite:hoplite-core:2.8.0.RC3")
    implementation("com.sksamuel.hoplite:hoplite-yaml:2.8.0.RC3")

    // Coroutines
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.8.0")


    // Database - Exposed ORM with H2
    implementation("org.jetbrains.exposed:exposed-core:$exposedVersion")
    implementation("org.jetbrains.exposed:exposed-dao:$exposedVersion")
    implementation("org.jetbrains.exposed:exposed-jdbc:$exposedVersion")
    implementation("org.jetbrains.exposed:exposed-kotlin-datetime:$exposedVersion")
    implementation("com.h2database:h2:2.2.224")

    // Logging
    implementation("io.github.oshai:kotlin-logging-jvm:6.0.3")
    implementation("org.slf4j:slf4j-simple:2.0.11")

    // Testing
    testImplementation("io.kotest:kotest-runner-junit5:5.8.0")
    testImplementation("io.kotest:kotest-assertions-core:5.8.0")
    testImplementation("io.kotest:kotest-property:5.8.0")
    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.8.0")
    testImplementation("io.mockk:mockk:1.13.9")
}

application {
    mainClass.set("io.github.alelk.apps.challengetgbot.AppKt")
}

tasks.test {
    useJUnitPlatform()
}

graalvmNative {
    binaries {
        named("main") {
            imageName.set("challenge-bot")
            buildArgs.addAll(
                "--no-fallback",
                "--enable-http",
                "--enable-https"
            )
        }
    }
}