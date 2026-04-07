plugins {
    java
    `java-test-fixtures`
    id("org.springframework.boot") version "3.3.5"
}

dependencies {
    implementation(project(":common"))
    implementation(project(":member"))
    implementation(project(":auth"))
    implementation(project(":admin"))

    implementation("org.springframework.boot:spring-boot-starter-web")
    implementation("org.springframework.boot:spring-boot-starter-data-jpa")
    implementation("org.springframework.boot:spring-boot-starter-validation")
    implementation("org.springframework.boot:spring-boot-starter-security")

    implementation("org.flywaydb:flyway-core")
    implementation("org.flywaydb:flyway-database-postgresql")

    runtimeOnly("org.postgresql:postgresql")

    testImplementation("org.springframework.boot:spring-boot-starter-test")
    testImplementation("org.springframework.security:spring-security-test")
    testImplementation(platform("org.testcontainers:testcontainers-bom:1.20.4"))
    testImplementation("org.testcontainers:junit-jupiter")
    testImplementation("org.testcontainers:postgresql")

    testFixturesImplementation(platform("org.springframework.boot:spring-boot-dependencies:3.3.5"))
    testFixturesImplementation("org.springframework.boot:spring-boot-starter-test")
    testFixturesImplementation(platform("org.testcontainers:testcontainers-bom:1.20.4"))
    testFixturesImplementation("org.testcontainers:junit-jupiter")
    testFixturesImplementation("org.testcontainers:postgresql")
}

springBoot {
    mainClass.set("com.example.app.MemberPlatformApplication")
}

tasks.named<org.springframework.boot.gradle.tasks.bundling.BootJar>("bootJar") {
    enabled = true
}

tasks.named<Jar>("jar") {
    enabled = true
}

tasks.named<Test>("test") {
    filter {
        isFailOnNoMatchingTests = false
        excludeTestsMatching("*IntegrationTest")
    }
}

tasks.register<Test>("integrationTest") {
    group = "verification"
    description = "app 테스트 소스의 *IntegrationTest(없으면 스킵). Docker 필요"
    testClassesDirs = sourceSets["test"].output.classesDirs
    classpath = sourceSets["test"].runtimeClasspath
    filter {
        isFailOnNoMatchingTests = false
        includeTestsMatching("*IntegrationTest")
    }
}
