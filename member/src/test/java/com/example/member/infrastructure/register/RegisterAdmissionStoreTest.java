package com.example.member.infrastructure.register;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.example.common.exception.BusinessException;
import com.example.common.exception.ErrorCode;
import java.time.Duration;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class RegisterAdmissionStoreTest {

    @Mock
    private StringRedisTemplate stringRedisTemplate;

    @Mock
    private ValueOperations<String, String> valueOperations;

    @Mock
    private RegisterProperties registerProperties;

    @InjectMocks
    private RegisterAdmissionStore registerAdmissionStore;

    private final Duration ttl = Duration.ofSeconds(60);

    @BeforeEach
    void 각_테스트_전에_레디스_모의와_설정을_준비한다() {
        when(stringRedisTemplate.opsForValue()).thenReturn(valueOperations);
        when(registerProperties.admission())
                .thenReturn(new RegisterProperties.Admission(2, ttl));
    }

    @Test
    void 슬롯이_남아_있으면_입장_토큰을_발급한다() {
        when(valueOperations.setIfAbsent(eq(RegisterRedisKeySpec.admissionSlotKey(0)), anyString(), eq(ttl)))
                .thenReturn(true);
        when(valueOperations.setIfAbsent(anyString(), eq("0"), eq(ttl))).thenReturn(true);

        var result = registerAdmissionStore.issue();

        assertThat(result.admissionToken()).isNotBlank();
        assertThat(result.expiresInSeconds()).isEqualTo(60L);
    }

    @Test
    void 앞_슬롯이_막혀_있으면_다음_슬롯에서_입장_토큰을_발급한다() {
        when(valueOperations.setIfAbsent(eq(RegisterRedisKeySpec.admissionSlotKey(0)), anyString(), eq(ttl)))
                .thenReturn(false);
        when(valueOperations.setIfAbsent(eq(RegisterRedisKeySpec.admissionSlotKey(1)), anyString(), eq(ttl)))
                .thenReturn(true);
        when(valueOperations.setIfAbsent(anyString(), eq("1"), eq(ttl))).thenReturn(true);

        var result = registerAdmissionStore.issue();

        assertThat(result.admissionToken()).isNotBlank();
        assertThat(result.expiresInSeconds()).isEqualTo(60L);
    }

    @Test
    void 슬롯은_잡았으나_토큰_키_저장에_실패하면_슬롯을_돌려주고_다음_슬롯을_시도한다() {
        when(valueOperations.setIfAbsent(eq(RegisterRedisKeySpec.admissionSlotKey(0)), anyString(), eq(ttl)))
                .thenReturn(true);
        when(valueOperations.setIfAbsent(anyString(), eq("0"), eq(ttl))).thenReturn(false);
        when(valueOperations.setIfAbsent(eq(RegisterRedisKeySpec.admissionSlotKey(1)), anyString(), eq(ttl)))
                .thenReturn(true);
        when(valueOperations.setIfAbsent(anyString(), eq("1"), eq(ttl))).thenReturn(true);

        var result = registerAdmissionStore.issue();

        assertThat(result.admissionToken()).isNotBlank();
        verify(stringRedisTemplate).delete(RegisterRedisKeySpec.admissionSlotKey(0));
    }

    @Test
    void 슬롯이_꽉_차면_요청_과다_오류이다() {
        when(valueOperations.setIfAbsent(eq(RegisterRedisKeySpec.admissionSlotKey(0)), anyString(), eq(ttl)))
                .thenReturn(false);
        when(valueOperations.setIfAbsent(eq(RegisterRedisKeySpec.admissionSlotKey(1)), anyString(), eq(ttl)))
                .thenReturn(false);

        assertThatThrownBy(() -> registerAdmissionStore.issue())
                .isInstanceOf(BusinessException.class)
                .satisfies(ex -> assertThat(((BusinessException) ex).getErrorCode()).isEqualTo(ErrorCode.TOO_MANY_REQUESTS));
    }

    @Test
    void 유효한_토큰은_검증에_성공한다() {
        String admissionToken = "tok";
        when(valueOperations.get(RegisterRedisKeySpec.admissionTokenKey(admissionToken))).thenReturn("0");
        when(valueOperations.get(RegisterRedisKeySpec.admissionSlotKey(0))).thenReturn(admissionToken);

        registerAdmissionStore.validate(admissionToken);

        verify(valueOperations).get(RegisterRedisKeySpec.admissionTokenKey(admissionToken));
    }

    @Test
    void 토큰이_없으면_입장_토큰이_유효하지_않다는_오류이다() {
        when(valueOperations.get(RegisterRedisKeySpec.admissionTokenKey("missing"))).thenReturn(null);

        assertThatThrownBy(() -> registerAdmissionStore.validate("missing"))
                .isInstanceOf(BusinessException.class)
                .satisfies(
                        ex ->
                                assertThat(((BusinessException) ex).getErrorCode())
                                        .isEqualTo(ErrorCode.MEMBER_ADMISSION_INVALID));
    }

    @Test
    void 검증_시_슬롯에_다른_토큰이_걸려_있으면_입장_토큰이_유효하지_않다는_오류이다() {
        String admissionToken = "mine";
        when(valueOperations.get(RegisterRedisKeySpec.admissionTokenKey(admissionToken))).thenReturn("0");
        when(valueOperations.get(RegisterRedisKeySpec.admissionSlotKey(0))).thenReturn("other-token");

        assertThatThrownBy(() -> registerAdmissionStore.validate(admissionToken))
                .isInstanceOf(BusinessException.class)
                .satisfies(
                        ex ->
                                assertThat(((BusinessException) ex).getErrorCode())
                                        .isEqualTo(ErrorCode.MEMBER_ADMISSION_INVALID));
    }

    @Test
    void 검증_시_토큰에_연결된_슬롯_번호가_숫자가_아니면_유효하지_않다는_오류이다() {
        String admissionToken = "bad-slot";
        when(valueOperations.get(RegisterRedisKeySpec.admissionTokenKey(admissionToken))).thenReturn("not-a-number");

        assertThatThrownBy(() -> registerAdmissionStore.validate(admissionToken))
                .isInstanceOf(BusinessException.class)
                .satisfies(
                        ex ->
                                assertThat(((BusinessException) ex).getErrorCode())
                                        .isEqualTo(ErrorCode.MEMBER_ADMISSION_INVALID));
    }

    @Test
    void 입장_해제_시_슬롯과_토큰_키를_삭제한다() {
        String admissionToken = "release-me";
        when(valueOperations.get(RegisterRedisKeySpec.admissionTokenKey(admissionToken))).thenReturn("1");
        when(valueOperations.get(RegisterRedisKeySpec.admissionSlotKey(1))).thenReturn(admissionToken);

        registerAdmissionStore.release(admissionToken);

        verify(stringRedisTemplate).delete(RegisterRedisKeySpec.admissionSlotKey(1));
        verify(stringRedisTemplate).delete(RegisterRedisKeySpec.admissionTokenKey(admissionToken));
    }

    @Test
    void 빈_문자열_토큰_검증은_유효하지_않다는_오류이다() {
        assertThatThrownBy(() -> registerAdmissionStore.validate("  "))
                .isInstanceOf(BusinessException.class)
                .satisfies(
                        ex ->
                                assertThat(((BusinessException) ex).getErrorCode())
                                        .isEqualTo(ErrorCode.MEMBER_ADMISSION_INVALID));
    }

    @Test
    void 입장_해제는_빈_토큰이면_아무것도_하지_않는다() {
        registerAdmissionStore.release(null);
        verify(stringRedisTemplate, never()).delete(anyString());
    }

    @Test
    void 입장_해제_시_슬롯에_다른_토큰이_있으면_슬롯은_두고_토큰_키만_삭제한다() {
        String admissionToken = "me";
        when(valueOperations.get(RegisterRedisKeySpec.admissionTokenKey(admissionToken))).thenReturn("0");
        when(valueOperations.get(RegisterRedisKeySpec.admissionSlotKey(0))).thenReturn("other");

        registerAdmissionStore.release(admissionToken);

        verify(stringRedisTemplate, never()).delete(RegisterRedisKeySpec.admissionSlotKey(0));
        verify(stringRedisTemplate).delete(RegisterRedisKeySpec.admissionTokenKey(admissionToken));
    }

    @Test
    void 입장_해제_중_레디스_오류가_나면_예외를_던져_상위에_전파한다() {
        when(valueOperations.get(RegisterRedisKeySpec.admissionTokenKey("broken")))
                .thenThrow(new DataAccessResourceFailureException("redis down"));

        assertThatThrownBy(() -> registerAdmissionStore.release("broken"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("회원가입 입장 해제에 실패했습니다");
    }
}
