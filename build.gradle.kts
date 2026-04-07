plugins {
    java
    id("org.springframework.boot") version "3.3.5" apply false
}

allprojects {
    group = "com.example"
    version = "0.0.1-SNAPSHOT"

    repositories {
        mavenCentral()
    }
}

subprojects {
    apply(plugin = "java")

    dependencies {
        implementation(platform("org.springframework.boot:spring-boot-dependencies:3.3.5"))
        compileOnly("org.projectlombok:lombok:1.18.36")
        annotationProcessor("org.projectlombok:lombok:1.18.36")

        testCompileOnly("org.projectlombok:lombok:1.18.36")
        testAnnotationProcessor("org.projectlombok:lombok:1.18.36")
    }

    java {
        toolchain {
            languageVersion.set(JavaLanguageVersion.of(21))
        }
    }

    tasks.withType<JavaCompile>().configureEach {
        options.encoding = "UTF-8"
        options.compilerArgs.add("-parameters")
    }

    tasks.withType<Test>().configureEach {
        useJUnitPlatform()
    }
}

tasks.register("assertModuleDependencyRules") {
    group = "verification"
    description = "Verifies Gradle project(:*) dependencies match layered monolith rules"
    doLast {
        val allowed =
            mapOf(
                    "common" to emptySet(),
                    "member" to setOf("common"),
                    "auth" to setOf("common", "member"),
                    "admin" to setOf("common", "member"),
                    "app" to setOf("common", "member", "auth", "admin"))
        subprojects.forEach { sp ->
            val allowedNames = allowed[sp.name] ?: return@forEach
            sp.configurations
                    .getByName("implementation")
                    .dependencies
                    .filterIsInstance<ProjectDependency>()
                    .forEach { dep ->
                        val depName = dep.name
                        require(depName in allowedNames) {
                            "Module '${sp.name}' must not depend on project ':$depName'. Allowed project dependencies: $allowedNames"
                        }
                    }
        }
    }
}

tasks.named("check") { dependsOn("assertModuleDependencyRules") }

subprojects {
    tasks.named("check") { dependsOn(rootProject.tasks.named("assertModuleDependencyRules")) }
}
