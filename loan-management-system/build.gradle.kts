plugins {
    id("project-base")
    id("test-suites")
    id("org.springframework.boot") version "3.3.9"
    id("io.spring.dependency-management") version "1.1.4"
}

group = "io.credable.lms"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
}

dependencies {
    implementation(platform(springboot.bom))
    implementation("org.springframework.boot:spring-boot-starter")
    implementation("org.springframework.boot:spring-boot-starter-web")
    implementation("org.springframework.boot:spring-boot-starter-security")

    implementation("org.springframework.boot:spring-boot-starter-validation")
    implementation("org.springframework.boot:spring-boot-starter-aop")
    implementation("org.springframework.retry:spring-retry")

    implementation(fasterxml.jackson.databind)

}

tasks.bootJar {
    archiveFileName.set("lms.jar")
}
