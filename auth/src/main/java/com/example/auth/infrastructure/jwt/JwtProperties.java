package com.example.auth.infrastructure.jwt;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@ConfigurationProperties(prefix = "jwt")
@Validated
public record JwtProperties(
        @NotBlank @Size(min = 32) String secret,
        @Min(1) long accessTokenValiditySeconds,
        @Min(1) long refreshTokenValiditySeconds) {}
