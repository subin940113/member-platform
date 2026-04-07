package com.example.app;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

@SpringBootApplication(scanBasePackages = "com.example")
@EnableJpaAuditing
@EntityScan(basePackages = {
        "com.example.member.domain",
        "com.example.auth.domain",
        "com.example.admin.domain"
})
@EnableJpaRepositories(basePackages = {
        "com.example.member.infrastructure.persistence",
        "com.example.auth.infrastructure.persistence",
        "com.example.admin.infrastructure.persistence"
})
public class MemberPlatformApplication {

    public static void main(String[] args) {
        SpringApplication.run(MemberPlatformApplication.class, args);
    }
}
