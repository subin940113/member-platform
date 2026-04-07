package com.example.common.exception;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

class BusinessExceptionTest {

    @Test
    void 오류_코드만으로_생성하면_기본_메시지가_설정된다() {
        BusinessException ex = new BusinessException(ErrorCode.MEMBER_NOT_FOUND);

        assertThat(ex.getErrorCode()).isEqualTo(ErrorCode.MEMBER_NOT_FOUND);
        assertThat(ex.getMessage()).isEqualTo(ErrorCode.MEMBER_NOT_FOUND.getDefaultMessage());
        assertThat(ex.getStatus()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(ex.getCode()).isEqualTo("MEMBER_NOT_FOUND");
    }

    @Test
    void 커스텀_메시지로_생성하면_기본_메시지를_대체한다() {
        BusinessException ex = new BusinessException(ErrorCode.INVALID_INPUT, "커스텀 메시지");

        assertThat(ex.getErrorCode()).isEqualTo(ErrorCode.INVALID_INPUT);
        assertThat(ex.getMessage()).isEqualTo("커스텀 메시지");
    }

    @Test
    void 오류_코드별로_올바른_응답_상태를_반환한다() {
        assertThat(new BusinessException(ErrorCode.AUTH_UNAUTHORIZED).getStatus())
                .isEqualTo(HttpStatus.UNAUTHORIZED);
        assertThat(new BusinessException(ErrorCode.AUTH_FORBIDDEN).getStatus())
                .isEqualTo(HttpStatus.FORBIDDEN);
        assertThat(new BusinessException(ErrorCode.MEMBER_DUPLICATE_EMAIL).getStatus())
                .isEqualTo(HttpStatus.CONFLICT);
        assertThat(new BusinessException(ErrorCode.INTERNAL_SERVER_ERROR).getStatus())
                .isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
    }
}
