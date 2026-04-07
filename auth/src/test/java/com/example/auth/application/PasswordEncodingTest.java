package com.example.auth.application;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

class PasswordEncodingTest {

    private final PasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    @Test
    void 비밀번호_암호화_결과는_평문과_다르다() {
        String raw = "Aa1!aaaa";

        String encoded = passwordEncoder.encode(raw);

        assertThat(encoded).isNotEqualTo(raw);
    }

    @Test
    void 같은_비밀번호를_두_번_암호화하면_다른_해시가_나온다() {
        String raw = "Aa1!aaaa";

        String first = passwordEncoder.encode(raw);
        String second = passwordEncoder.encode(raw);

        assertThat(first).isNotEqualTo(second);
    }

    @Test
    void 올바른_비밀번호는_검증에_성공한다() {
        String raw = "Aa1!aaaa";
        String encoded = passwordEncoder.encode(raw);

        assertThat(passwordEncoder.matches(raw, encoded)).isTrue();
    }

    @Test
    void 틀린_비밀번호는_검증에_실패한다() {
        String encoded = passwordEncoder.encode("Aa1!aaaa");

        assertThat(passwordEncoder.matches("wrongPassword1!", encoded)).isFalse();
    }

    @Test
    void 암호화된_해시에_평문이_노출되지_않는다() {
        String raw = "Aa1!aaaa";
        String encoded = passwordEncoder.encode(raw);

        assertThat(encoded).doesNotContain(raw);
    }
}
