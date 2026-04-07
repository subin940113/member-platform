package com.example.member.infrastructure.register;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Duration;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class RegisterIdempotencyStoreTest {

    @Mock
    private StringRedisTemplate stringRedisTemplate;

    @Mock
    private ValueOperations<String, String> valueOperations;

    @Mock
    private RegisterProperties registerProperties;

    @InjectMocks
    private RegisterIdempotencyStore registerIdempotencyStore;

    private final Duration ttl = Duration.ofMinutes(10);

    @BeforeEach
    void 각_테스트_전에_레디스_모의와_설정을_준비한다() {
        when(stringRedisTemplate.opsForValue()).thenReturn(valueOperations);
        when(registerProperties.idempotency()).thenReturn(new RegisterProperties.Idempotency(ttl));
    }

    @Test
    void 처음_멱등성_선점은_성공한다() {
        when(valueOperations.setIfAbsent(
                        eq(RegisterRedisKeySpec.idempotencyKey("k1")), eq("PROCESSING"), eq(ttl)))
                .thenReturn(true);

        assertThat(registerIdempotencyStore.acquire("k1")).isTrue();
    }

    @Test
    void 이미_키가_있으면_멱등성_선점은_실패한다() {
        when(valueOperations.setIfAbsent(any(), any(), any(Duration.class))).thenReturn(false);

        assertThat(registerIdempotencyStore.acquire("k1")).isFalse();
    }

    @Test
    void 멱등_키_비우기는_저장소에서_키를_삭제한다() {
        registerIdempotencyStore.clear(" k2 ");

        verify(stringRedisTemplate).delete(RegisterRedisKeySpec.idempotencyKey("k2"));
    }

    @Test
    void 멱등성_키_앞뒤_공백은_제거한_뒤_레디스_키를_만든다() {
        when(valueOperations.setIfAbsent(
                        eq(RegisterRedisKeySpec.idempotencyKey("abc")), eq("PROCESSING"), eq(ttl)))
                .thenReturn(true);

        assertThat(registerIdempotencyStore.acquire("  abc  ")).isTrue();
    }
}
