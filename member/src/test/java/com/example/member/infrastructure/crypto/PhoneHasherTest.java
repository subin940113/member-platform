package com.example.member.infrastructure.crypto;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class PhoneHasherTest {

    private PhoneHasher phoneHasher;

    @BeforeEach
    void 각_테스트_전에_휴대폰_번호_해시기를_준비한다() {
        CryptoProperties properties = new CryptoProperties(
                "test-secret-key-that-is-at-least-32-chars!!", "test-pepper");
        phoneHasher = new PhoneHasher(properties);
    }

    @Test
    void 하이픈이_포함된_휴대폰_번호와_숫자만_있는_휴대폰_번호의_해시가_동일하다() {
        String withHyphens = phoneHasher.hash("010-1234-5678");
        String digitsOnly = phoneHasher.hash("01012345678");

        assertThat(withHyphens).isEqualTo(digitsOnly);
    }

    @Test
    void 같은_휴대폰_번호를_반복_해시하면_항상_동일한_값이_나온다() {
        String first = phoneHasher.hash("01012345678");
        String second = phoneHasher.hash("01012345678");

        assertThat(first).isEqualTo(second);
    }

    @Test
    void 서로_다른_휴대폰_번호는_서로_다른_해시를_반환한다() {
        String hash1 = phoneHasher.hash("01012345678");
        String hash2 = phoneHasher.hash("01087654321");

        assertThat(hash1).isNotEqualTo(hash2);
    }

    @Test
    void 해시_결과는_64자리_소문자_16진수_문자열이다() {
        String hash = phoneHasher.hash("01012345678");

        assertThat(hash).hasSize(64);
        assertThat(hash).matches("[0-9a-f]{64}");
    }

    @Test
    void 입력이_비어_있어도_예외를_던지지_않고_해시를_반환한다() {
        String hash = phoneHasher.hash(null);

        assertThat(hash).isNotNull();
        assertThat(hash).hasSize(64);
    }

    @Test
    void 추가_비밀값을_쓰면_같은_입력이라도_추가_비밀값이_없을_때와_다른_해시가_나온다() {
        CryptoProperties noPepperProps = new CryptoProperties(
                "test-secret-key-that-is-at-least-32-chars!!", null);
        PhoneHasher noPepperHasher = new PhoneHasher(noPepperProps);

        String withPepper = phoneHasher.hash("01012345678");
        String withoutPepper = noPepperHasher.hash("01012345678");

        assertThat(withPepper).isNotEqualTo(withoutPepper);
    }
}
