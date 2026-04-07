plugins {
    java
}

dependencies {
    implementation(project(":common"))
    implementation(project(":member"))

    implementation("org.springframework.boot:spring-boot-starter-data-jpa")
    implementation("org.springframework.boot:spring-boot-starter-security")
    implementation("org.springframework.boot:spring-boot-starter-validation")
    implementation("org.springframework.boot:spring-boot-starter-web")

    implementation("io.jsonwebtoken:jjwt-api:0.12.6")
    runtimeOnly("io.jsonwebtoken:jjwt-impl:0.12.6")
    runtimeOnly("io.jsonwebtoken:jjwt-jackson:0.12.6")

    testImplementation("org.springframework.boot:spring-boot-starter-test")
    testImplementation("org.springframework.security:spring-security-test")
    testImplementation(project(":app"))
    testImplementation(project(":admin"))
    testImplementation(testFixtures(project(":app")))
    testImplementation(testFixtures(project(":member")))
    testImplementation(platform("org.testcontainers:testcontainers-bom:1.20.4"))
    testImplementation("org.testcontainers:junit-jupiter")
    testImplementation("org.testcontainers:postgresql")
}

tasks.named<Test>("test") {
    filter {
        isFailOnNoMatchingTests = false
        excludeTestsMatching("*IntegrationTest")
    }
}

tasks.register<Test>("integrationTest") {
    group = "verification"
    description = "Testcontainers 기반 인증 및 보안 통합 테스트(Docker 필요)"
    testClassesDirs = sourceSets["test"].output.classesDirs
    classpath = sourceSets["test"].runtimeClasspath
    filter {
        includeTestsMatching("*IntegrationTest")
    }
}
