package com.example.member.infrastructure.register;

import static org.assertj.core.api.Assertions.assertThat;

import com.example.common.Constants;
import org.junit.jupiter.api.Test;

class RegisterRedisKeySpecTest {

    @Test
    void 입장_토큰_키는_서비스명과_토큰_값을_포함한다() {
        String key = RegisterRedisKeySpec.admissionTokenKey("abc-uuid");
        assertThat(key).startsWith(Constants.SERVICE_NAME + "::");
        assertThat(key).endsWith("::abc-uuid");
    }

    @Test
    void 입장_슬롯_키는_슬롯_번호를_포함한다() {
        assertThat(RegisterRedisKeySpec.admissionSlotKey(3)).contains("::SLOT::3");
    }

    @Test
    void 멱등성_키는_클라이언트가_준_문자열을_접미로_쓴다() {
        assertThat(RegisterRedisKeySpec.idempotencyKey("client-key-1")).endsWith("::client-key-1");
    }
}
