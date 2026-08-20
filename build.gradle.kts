plugins {
    kotlin("jvm") version "2.3.21"
    application
}

group = "ca.cashmclean.tagalog"
version = "0.1.0"

repositories {
    mavenCentral()
}

dependencies {
    implementation("info.picocli:picocli:4.7.7")
    implementation("com.networknt:json-schema-validator:1.5.9")
    implementation("org.xerial:sqlite-jdbc:3.53.2.0")
    implementation("org.flywaydb:flyway-core:12.8.1")
    implementation("org.jetbrains.exposed:exposed-core:1.3.1")
    implementation("org.jetbrains.exposed:exposed-jdbc:1.3.1")
    implementation("ch.qos.logback:logback-classic:1.5.18")

    testImplementation("org.junit.jupiter:junit-jupiter:5.14.2")
    testImplementation("org.junit.jupiter:junit-jupiter-params:5.14.2")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher:1.14.2")
}

kotlin {
    jvmToolchain(21)
}

application {
    mainClass = "ca.cashmclean.tagalog.MainKt"
    applicationName = "tagalog"
}

tasks.test {
    useJUnitPlatform()
}

tasks.processResources {
    from("docs/lesson-package.schema.json")
}
