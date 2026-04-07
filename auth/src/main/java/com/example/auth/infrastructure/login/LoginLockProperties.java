package com.example.auth.infrastructure.login;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@ConfigurationProperties(prefix = "app.auth.login")
@Validated
public record LoginLockProperties(
        @Min(1) @Max(999) int maxFailuresBeforeLock, @NotNull Duration lockDuration) {}
