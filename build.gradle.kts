plugins {
    id("org.springframework.boot") version "3.5.10"
    id("io.spring.dependency-management") version "1.1.7"
    id("org.jlleitschuh.gradle.ktlint") version "14.0.1"
    kotlin("jvm") version "2.3.10"
    kotlin("plugin.spring") version "2.3.10"
}

group = "no.novari"
version = "0.1.0-SNAPSHOT"

repositories {
    mavenLocal()
    maven(url = "https://repo.fintlabs.no/releases")
    mavenCentral()
}

tasks.withType<Test> {
    useJUnitPlatform()
}

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(25))
    }
}

kotlin {
    compilerOptions {
        freeCompilerArgs.addAll(
            "-Xjsr305=strict",
        )
    }
}

dependencies {
    implementation("org.springframework.boot:spring-boot-starter-actuator")
    implementation("org.springframework.boot:spring-boot-starter-security")
    implementation("org.springframework.boot:spring-boot-starter-validation")
    implementation("org.springframework.boot:spring-boot-starter-web")
    implementation("org.springframework.boot:spring-boot-starter-thymeleaf")

    implementation("org.wiremock:wiremock-standalone:3.13.2")

    implementation("com.fasterxml.jackson.module:jackson-module-kotlin")
    implementation("io.github.oshai:kotlin-logging-jvm:7.0.13")
    implementation("net.logstash.logback:logstash-logback-encoder:7.4")

    implementation("no.fintlabs:fint-model-resource:0.5.0")
    implementation("no.fint:fint-arkiv-resource-model-java:3.21.10")
    implementation("no.fint:fint-administrasjon-resource-model-java:3.21.10")

    runtimeOnly("io.micrometer:micrometer-registry-prometheus")

    compileOnly("javax.validation:validation-api:2.0.1.Final")

    testImplementation("org.springframework.boot:spring-boot-starter-test")
}

ktlint {
    version.set("1.8.0")
}

tasks.named("check") {
    dependsOn("ktlintCheck")
}
