package com.example.member.infrastructure.crypto;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class PiiEncryptorTest {

    private PiiEncryptor encryptor;

    @BeforeEach
    void 각_테스트_전에_암호화기를_준비한다() {
        CryptoProperties properties = new CryptoProperties(
                "test-secret-key-that-is-at-least-32-chars!!", null);
        encryptor = new PiiEncryptor(properties);
    }

    @Test
    void 암호화_후_복호화하면_원본_평문이_복원된다() {
        String plainText = "홍길동";

        String decrypted = encryptor.decrypt(encryptor.encrypt(plainText));

        assertThat(decrypted).isEqualTo(plainText);
    }

    @Test
    void 암호화_결과에_평문이_노출되지_않는다() {
        String encrypted = encryptor.encrypt("테스트");

        assertThat(encrypted).isNotBlank();
        assertThat(encrypted).doesNotContain("테스트");
    }

    @Test
    void 같은_평문을_두_번_암호화하면_서로_다른_암호문이_생성된다() {
        String first = encryptor.encrypt("동일한값");
        String second = encryptor.encrypt("동일한값");

        assertThat(first).isNotEqualTo(second);
    }

    @Test
    void 비어_있는_값을_암호화하면_비어_있는_값을_반환한다() {
        assertThat(encryptor.encrypt(null)).isNull();
    }

    @Test
    void 비어_있는_값을_복호화하면_비어_있는_값을_반환한다() {
        assertThat(encryptor.decrypt(null)).isNull();
    }

    @Test
    void 공백_문자열을_복호화하면_비어_있는_값을_반환한다() {
        assertThat(encryptor.decrypt("   ")).isNull();
    }

    @Test
    void 휴대폰_번호를_암호화_복호화하면_원본이_복원된다() {
        String phone = "01012345678";

        assertThat(encryptor.decrypt(encryptor.encrypt(phone))).isEqualTo(phone);
    }

    @Test
    void 이메일을_암호화_복호화하면_원본이_복원된다() {
        String email = "user@example.com";

        assertThat(encryptor.decrypt(encryptor.encrypt(email))).isEqualTo(email);
    }
}
