package com.example.member.presentation.validation;

import static org.assertj.core.api.Assertions.assertThat;

import jakarta.validation.ConstraintValidatorContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

class KoreanMobilePhoneValidatorTest {

    private KoreanMobilePhoneValidator validator;

    @BeforeEach
    void 각_테스트_전에_검증기를_준비한다() {
        validator = new KoreanMobilePhoneValidator();
    }

    @Test
    void 하이픈_포함_번호는_정규화_후_유효하면_통과한다() {
        assertThat(validator.isValid("010-1234-5678", Mockito.mock(ConstraintValidatorContext.class)))
                .isTrue();
    }

    @Test
    void 숫자만_입력도_통과한다() {
        assertThat(validator.isValid("01012345678", Mockito.mock(ConstraintValidatorContext.class)))
                .isTrue();
    }

    @Test
    void 잘못된_번호는_실패한다() {
        assertThat(validator.isValid("0212345678", Mockito.mock(ConstraintValidatorContext.class)))
                .isFalse();
    }

    @Test
    void 값이_비어_있으면_검증에_실패한다() {
        assertThat(validator.isValid(null, Mockito.mock(ConstraintValidatorContext.class))).isFalse();
    }
}
