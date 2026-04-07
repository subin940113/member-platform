package com.example.member.infrastructure.crypto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@ConfigurationProperties(prefix = "app.crypto")
@Validated
public record CryptoProperties(
        @NotBlank @Size(min = 32) String secret,
        @NotBlank @Size(min = 32) String phonePepper) {}
