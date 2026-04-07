package com.example.common.exception;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.dao.DataIntegrityViolationException;

class DataIntegrityViolationsTest {

    private static DataIntegrityViolationException exWith(String message) {
        return new DataIntegrityViolationException(message, new RuntimeException(message));
    }

    @Test
    void 이메일_고유_제약_위반은_중복_이메일_오류로_변환된다() {
        DataIntegrityViolationException ex = exWith(
                "ERROR: duplicate key value violates unique constraint \"uk_members_email\"");

        BusinessException result = DataIntegrityViolations.toBusinessException(ex);

        assertThat(result.getErrorCode()).isEqualTo(ErrorCode.MEMBER_DUPLICATE_EMAIL);
    }

    @Test
    void 휴대폰_번호_해시_고유_제약_위반은_중복_휴대폰_번호_오류로_변환된다() {
        DataIntegrityViolationException ex = exWith(
                "ERROR: duplicate key value violates unique constraint \"uk_members_phone_hash\"");

        BusinessException result = DataIntegrityViolations.toBusinessException(ex);

        assertThat(result.getErrorCode()).isEqualTo(ErrorCode.MEMBER_DUPLICATE_PHONE);
    }

    @Test
    void 알_수_없는_제약_위반은_내부_서버_오류로_변환된다() {
        DataIntegrityViolationException ex = exWith("unknown constraint violation");

        BusinessException result = DataIntegrityViolations.toBusinessException(ex);

        assertThat(result.getErrorCode()).isEqualTo(ErrorCode.INTERNAL_SERVER_ERROR);
    }

    @Test
    void 중첩된_예외에서_무결성_위반_예외를_찾아낸다() {
        DataIntegrityViolationException inner = exWith("constraint");
        RuntimeException outer = new RuntimeException("wrapper", inner);

        DataIntegrityViolationException found = DataIntegrityViolations.findDataIntegrity(outer);

        assertThat(found).isSameAs(inner);
    }

    @Test
    void 무결성_위반_예외가_없으면_찾은_결과가_비어_있다() {
        RuntimeException ex = new RuntimeException("no constraint here");

        assertThat(DataIntegrityViolations.findDataIntegrity(ex)).isNull();
    }

    @Test
    void 직접_전달된_무결성_위반_예외를_그대로_반환한다() {
        DataIntegrityViolationException ex = exWith("direct");

        assertThat(DataIntegrityViolations.findDataIntegrity(ex)).isSameAs(ex);
    }
}
