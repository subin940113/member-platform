package com.example.common.response;

import static org.assertj.core.api.Assertions.assertThat;

import com.example.common.exception.ErrorCode;
import org.junit.jupiter.api.Test;

class ApiResponseTest {

    @Test
    void 데이터가_있는_성공_응답은_성공_여부가_참이고_정상_코드를_반환한다() {
        ApiResponse<String> response = ApiResponse.ok("hello");

        assertThat(response.success()).isTrue();
        assertThat(response.code()).isEqualTo("OK");
        assertThat(response.data()).isEqualTo("hello");
        assertThat(response.message()).isNull();
        assertThat(response.traceId()).isNull();
    }

    @Test
    void 데이터_없는_성공_응답은_본문_데이터가_비어_있다() {
        ApiResponse<Void> response = ApiResponse.ok();

        assertThat(response.success()).isTrue();
        assertThat(response.code()).isEqualTo("OK");
        assertThat(response.data()).isNull();
    }

    @Test
    void 오류_코드로_실패_응답을_생성하면_성공_여부가_거짓이다() {
        ApiResponse<Void> response = ApiResponse.fail(ErrorCode.MEMBER_NOT_FOUND, "trace-1");

        assertThat(response.success()).isFalse();
        assertThat(response.code()).isEqualTo("MEMBER_NOT_FOUND");
        assertThat(response.message()).isEqualTo(ErrorCode.MEMBER_NOT_FOUND.getDefaultMessage());
        assertThat(response.traceId()).isEqualTo("trace-1");
        assertThat(response.data()).isNull();
    }

    @Test
    void 커스텀_메시지로_실패_응답을_생성하면_기본_메시지를_대체한다() {
        ApiResponse<Void> response = ApiResponse.fail(ErrorCode.INVALID_INPUT, "필드 오류", "trace-2");

        assertThat(response.success()).isFalse();
        assertThat(response.code()).isEqualTo("INVALID_INPUT");
        assertThat(response.message()).isEqualTo("필드 오류");
        assertThat(response.traceId()).isEqualTo("trace-2");
    }

    @Test
    void 데이터가_포함된_실패_응답을_생성할_수_있다() {
        ApiResponse<String> response = ApiResponse.failWithData(ErrorCode.INVALID_INPUT, "오류", "detail", "trace-3");

        assertThat(response.success()).isFalse();
        assertThat(response.data()).isEqualTo("detail");
    }

    @Test
    void 커스텀_코드_문자열로_실패_응답을_생성할_수_있다() {
        ApiResponse<Void> response = ApiResponse.fail("CUSTOM_CODE", "커스텀 메시지", "trace-4");

        assertThat(response.success()).isFalse();
        assertThat(response.code()).isEqualTo("CUSTOM_CODE");
        assertThat(response.message()).isEqualTo("커스텀 메시지");
    }

    @Test
    void 추적_식별자는_표준_임의_식별_형식으로_생성된다() {
        String traceId = ApiResponse.newTraceId();

        assertThat(traceId).isNotBlank();
        assertThat(traceId).matches("[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}");
    }
}
