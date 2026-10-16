buildscript {
    repositories {
        gradlePluginPortal()
    }
    dependencies {
        classpath(platform("com.fasterxml.jackson:jackson-bom:2.22.3"))
        constraints {
            classpath("org.apache.httpcomponents.client5:httpclient5:5.6.5")
            classpath("org.apache.httpcomponents.core5:httpcore5:5.4.4")
            classpath("org.apache.httpcomponents.core5:httpcore5-h2:5.4.4")
            classpath("org.apache.commons:commons-lang3:3.21.0")
        }
    }
}

plugins {
    id("org.springframework.boot") version "3.5.16"
    id("io.spring.dependency-management") version "1.1.7"
    id("io.github.ben-manes.versions") version "0.66.0"
    id("org.jlleitschuh.gradle.ktlint") version "14.2.0"
    kotlin("jvm") version "2.4.10"
    kotlin("plugin.spring") version "2.4.10"
}

group = "no.novari"
version = "0.0.1-SNAPSHOT"

var fintResourceModelVersion = "4.1.0"

kotlin {
    jvmToolchain(25)
    compilerOptions {
        freeCompilerArgs.addAll(
            "-Xjsr305=strict",
        )
    }
}

repositories {
    mavenCentral()
    maven {
        url = uri("https://repo.fintlabs.no/releases")
    }
    mavenLocal()
}

extra["jackson-bom.version"] = "2.22.3"
extra["log4j2.version"] = "2.26.1"
extra["tomcat.version"] = "10.1.59"
extra["commons-lang3.version"] = "3.21.0"

dependencies {
    constraints {
        implementation("org.apache.commons:commons-lang3:3.21.0") {
            because("Fixes CVE-2025-48924 in the Spring Boot managed version")
        }
    }

    implementation("org.springframework.boot:spring-boot-starter-actuator")
    implementation("org.springframework.boot:spring-boot-starter-security")
    implementation("org.springframework.boot:spring-boot-starter-validation")
    implementation("org.springframework.boot:spring-boot-starter-web")
    implementation("org.springframework.boot:spring-boot-starter-thymeleaf")
    implementation("org.jetbrains.kotlin:kotlin-reflect")
    implementation("io.github.oshai:kotlin-logging-jvm:8.0.4")
    implementation("com.fasterxml.jackson.module:jackson-module-kotlin")

    implementation("org.wiremock:wiremock-standalone:3.13.2")

    implementation("no.novari:fint-arkiv-resource-model-java:$fintResourceModelVersion")
    implementation("no.novari:fint-administrasjon-resource-model-java:$fintResourceModelVersion")

    compileOnly("javax.validation:validation-api:2.0.1.Final")

    runtimeOnly("io.micrometer:micrometer-registry-prometheus")
    // Pinned to 8.x: 9.x pulls in Jackson 3 (tools.jackson), while Spring Boot 3.5 uses Jackson 2
    runtimeOnly("net.logstash.logback:logstash-logback-encoder:8.1")

    testImplementation("org.springframework.boot:spring-boot-starter-test")
}

tasks.test {
    useJUnitPlatform()
    jvmArgs("--sun-misc-unsafe-memory-access=allow")
}

tasks.bootRun {
    jvmArgs("--sun-misc-unsafe-memory-access=allow")
}

tasks.jar {
    isEnabled = false
}

ktlint {
    version.set("1.8.0")
}

tasks.named("check") {
    dependsOn("ktlintCheck")
}
