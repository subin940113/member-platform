package com.example.member.infrastructure.register;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@ConfigurationProperties(prefix = "app.register")
@Validated
public record RegisterProperties(@NotNull @Valid Admission admission, @NotNull @Valid Idempotency idempotency) {

    public record Admission(@Min(1) @Max(100_000) int maxConcurrent, @NotNull Duration tokenTtl) {}

    public record Idempotency(@NotNull Duration ttl) {}
}
