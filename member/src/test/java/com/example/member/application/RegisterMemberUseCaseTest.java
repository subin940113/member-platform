package com.example.member.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.example.common.exception.BusinessException;
import com.example.common.exception.ErrorCode;
import com.example.member.domain.Member;
import com.example.member.infrastructure.crypto.PhoneHasher;
import com.example.member.infrastructure.persistence.MemberRepository;
import com.example.member.infrastructure.register.RegisterAdmissionStore;
import com.example.member.infrastructure.register.RegisterIdempotencyStore;
import com.example.member.presentation.dto.request.RegisterMemberRequest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;

@ExtendWith(MockitoExtension.class)
class RegisterMemberUseCaseTest {

    private static final String ADMISSION_TOKEN = "tok";
    private static final String IDEMPOTENCY_KEY = "idem";

    @Mock
    private RegisterAdmissionStore admissionStore;

    @Mock
    private RegisterIdempotencyStore idempotencyStore;

    @Mock
    private MemberRepository memberRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private PhoneHasher phoneHasher;

    @InjectMocks
    private RegisterMemberUseCase registerMemberUseCase;

    private final RegisterMemberRequest request =
            new RegisterMemberRequest("a@b.com", "이름", "Aa1!aaaa", "01012345678");

    @Test
    void 입장_토큰이_없으면_입장_토큰이_필요하다는_오류이다() {
        assertThatThrownBy(() -> registerMemberUseCase.register(null, IDEMPOTENCY_KEY, request))
                .isInstanceOf(BusinessException.class)
                .satisfies(
                        ex ->
                                assertThat(((BusinessException) ex).getErrorCode())
                                        .isEqualTo(ErrorCode.MEMBER_ADMISSION_REQUIRED));

        verify(admissionStore, never()).validate(anyString());
    }

    @Test
    void 입장_토큰이_공백만_이면_입장_토큰이_필요하다는_오류이다() {
        assertThatThrownBy(() -> registerMemberUseCase.register("   ", IDEMPOTENCY_KEY, request))
                .isInstanceOf(BusinessException.class)
                .satisfies(
                        ex ->
                                assertThat(((BusinessException) ex).getErrorCode())
                                        .isEqualTo(ErrorCode.MEMBER_ADMISSION_REQUIRED));

        verify(admissionStore, never()).validate(anyString());
    }

    @Test
    void 멱등성_키가_없으면_멱등성_키가_필요하다는_오류이다() {
        assertThatThrownBy(() -> registerMemberUseCase.register(ADMISSION_TOKEN, null, request))
                .isInstanceOf(BusinessException.class)
                .satisfies(
                        ex ->
                                assertThat(((BusinessException) ex).getErrorCode())
                                        .isEqualTo(ErrorCode.MEMBER_IDEMPOTENCY_KEY_REQUIRED));

        verify(admissionStore, never()).validate(anyString());
    }

    @Test
    void 멱등성_키가_공백만_이면_멱등성_키가_필요하다는_오류이다() {
        assertThatThrownBy(() -> registerMemberUseCase.register(ADMISSION_TOKEN, "  \t  ", request))
                .isInstanceOf(BusinessException.class)
                .satisfies(
                        ex ->
                                assertThat(((BusinessException) ex).getErrorCode())
                                        .isEqualTo(ErrorCode.MEMBER_IDEMPOTENCY_KEY_REQUIRED));

        verify(admissionStore, never()).validate(anyString());
    }

    @Test
    void 멱등성_선점에_성공하면_회원_저장_후_멱등_키를_비우고_입장을_해제한다() {
        when(idempotencyStore.acquire(IDEMPOTENCY_KEY)).thenReturn(true);
        Member saved = org.mockito.Mockito.mock(Member.class);
        when(phoneHasher.normalize("01012345678")).thenReturn("01012345678");
        when(phoneHasher.hash("01012345678")).thenReturn("h");
        when(passwordEncoder.encode("Aa1!aaaa")).thenReturn("enc");
        when(memberRepository.save(any(Member.class))).thenReturn(saved);

        Member result = registerMemberUseCase.register(ADMISSION_TOKEN, IDEMPOTENCY_KEY, request);

        assertThat(result).isSameAs(saved);
        verify(admissionStore).validate(ADMISSION_TOKEN);
        verify(idempotencyStore).clear(IDEMPOTENCY_KEY);
        verify(admissionStore).release(ADMISSION_TOKEN);
    }

    @Test
    void 입장_토큰과_멱등성_키의_앞뒤_공백은_제거된_값으로_저장소에_전달된다() {
        when(idempotencyStore.acquire(IDEMPOTENCY_KEY)).thenReturn(true);
        Member saved = org.mockito.Mockito.mock(Member.class);
        when(phoneHasher.normalize("01012345678")).thenReturn("01012345678");
        when(phoneHasher.hash("01012345678")).thenReturn("h");
        when(passwordEncoder.encode("Aa1!aaaa")).thenReturn("enc");
        when(memberRepository.save(any(Member.class))).thenReturn(saved);

        registerMemberUseCase.register("  tok  ", "  idem  ", request);

        verify(admissionStore).validate(ADMISSION_TOKEN);
        verify(idempotencyStore).acquire(IDEMPOTENCY_KEY);
        verify(idempotencyStore).clear(IDEMPOTENCY_KEY);
        verify(admissionStore).release(ADMISSION_TOKEN);
    }

    @Test
    void 멱등성_선점에_실패하면_요청_진행_중_오류이고_입장만_해제한다() {
        when(idempotencyStore.acquire(IDEMPOTENCY_KEY)).thenReturn(false);

        assertThatThrownBy(() -> registerMemberUseCase.register(ADMISSION_TOKEN, IDEMPOTENCY_KEY, request))
                .isInstanceOf(BusinessException.class)
                .satisfies(
                        ex ->
                                assertThat(((BusinessException) ex).getErrorCode())
                                        .isEqualTo(ErrorCode.MEMBER_REQUEST_IN_PROGRESS));

        verify(memberRepository, never()).save(any());
        verify(idempotencyStore, never()).clear(anyString());
        verify(admissionStore).release(ADMISSION_TOKEN);
    }

    @Test
    void 회원_저장이_실패해도_멱등_키를_비우고_입장을_해제한다() {
        when(idempotencyStore.acquire(IDEMPOTENCY_KEY)).thenReturn(true);
        when(phoneHasher.normalize("01012345678")).thenReturn("01012345678");
        when(phoneHasher.hash("01012345678")).thenReturn("h");
        when(passwordEncoder.encode("Aa1!aaaa")).thenReturn("enc");
        when(memberRepository.save(any(Member.class)))
                .thenThrow(new BusinessException(ErrorCode.MEMBER_DUPLICATE_EMAIL));

        assertThatThrownBy(() -> registerMemberUseCase.register(ADMISSION_TOKEN, IDEMPOTENCY_KEY, request))
                .isInstanceOf(BusinessException.class);

        verify(idempotencyStore).clear(IDEMPOTENCY_KEY);
        verify(admissionStore).release(ADMISSION_TOKEN);
    }

    @Test
    void 입장_검증에_실패하면_멱등_선점_전이므로_멱등_비우기와_입장_해제가_없다() {
        doThrow(new BusinessException(ErrorCode.MEMBER_ADMISSION_INVALID))
                .when(admissionStore)
                .validate("bad");

        assertThatThrownBy(() -> registerMemberUseCase.register("bad", IDEMPOTENCY_KEY, request))
                .isInstanceOf(BusinessException.class)
                .satisfies(
                        ex ->
                                assertThat(((BusinessException) ex).getErrorCode())
                                        .isEqualTo(ErrorCode.MEMBER_ADMISSION_INVALID));

        verify(idempotencyStore, never()).acquire(anyString());
        verify(admissionStore, never()).release(anyString());
    }

    @Test
    void 회원_저장_성공_시_비밀번호는_해시로_저장되고_이름과_휴대폰_번호는_평문으로_엔티티에_설정된다() {
        when(idempotencyStore.acquire(IDEMPOTENCY_KEY)).thenReturn(true);
        when(phoneHasher.normalize("01012345678")).thenReturn("01012345678");
        when(phoneHasher.hash("01012345678")).thenReturn("h1");
        when(passwordEncoder.encode("Aa1!aaaa")).thenReturn("$2a$10$hashedvalue");
        when(memberRepository.save(any(Member.class))).thenAnswer(inv -> inv.getArgument(0));

        Member result =
                registerMemberUseCase.register(
                        ADMISSION_TOKEN,
                        IDEMPOTENCY_KEY,
                        new RegisterMemberRequest("new@b.com", "테스터", "Aa1!aaaa", "01012345678"));

        verify(passwordEncoder).encode("Aa1!aaaa");
        verify(memberRepository).save(any(Member.class));
        assertThat(result.getName()).isEqualTo("테스터");
        assertThat(result.getPhone()).isEqualTo("01012345678");
        assertThat(result.getPasswordHash()).isEqualTo("$2a$10$hashedvalue");
    }

    @Test
    void 저장_시_이메일_고유_제약을_위반하면_중복_이메일_예외로_변환된다() {
        when(idempotencyStore.acquire(IDEMPOTENCY_KEY)).thenReturn(true);
        when(phoneHasher.normalize("01012345678")).thenReturn("01012345678");
        when(phoneHasher.hash("01012345678")).thenReturn("ph");
        when(passwordEncoder.encode("Aa1!aaaa")).thenReturn("enc");
        when(memberRepository.save(any(Member.class)))
                .thenThrow(
                        new DataIntegrityViolationException(
                                "save", new RuntimeException("violates uk_members_email")));

        assertThatThrownBy(
                        () ->
                                registerMemberUseCase.register(
                                        ADMISSION_TOKEN,
                                        IDEMPOTENCY_KEY,
                                        new RegisterMemberRequest("dup@b.com", "이름", "Aa1!aaaa", "01012345678")))
                .isInstanceOf(BusinessException.class)
                .satisfies(
                        ex ->
                                assertThat(((BusinessException) ex).getErrorCode())
                                        .isEqualTo(ErrorCode.MEMBER_DUPLICATE_EMAIL));
    }

    @Test
    void 이메일은_소문자로_정규화되어_저장된다() {
        when(idempotencyStore.acquire(IDEMPOTENCY_KEY)).thenReturn(true);
        when(phoneHasher.normalize("01012345678")).thenReturn("01012345678");
        when(phoneHasher.hash("01012345678")).thenReturn("h1");
        when(passwordEncoder.encode("Aa1!aaaa")).thenReturn("enc");
        when(memberRepository.save(any(Member.class))).thenAnswer(inv -> inv.getArgument(0));

        registerMemberUseCase.register(
                ADMISSION_TOKEN,
                IDEMPOTENCY_KEY,
                new RegisterMemberRequest("MiXeD@Example.COM", "이름", "Aa1!aaaa", "01012345678"));

        ArgumentCaptor<Member> memberCaptor = ArgumentCaptor.forClass(Member.class);
        verify(memberRepository).save(memberCaptor.capture());
        assertThat(memberCaptor.getValue().getEmail()).isEqualTo("mixed@example.com");
    }

    @Test
    void 저장_시_휴대폰_번호_해시_고유_제약을_위반하면_중복_휴대폰_번호_예외로_변환된다() {
        when(idempotencyStore.acquire(IDEMPOTENCY_KEY)).thenReturn(true);
        when(phoneHasher.normalize("01099998888")).thenReturn("01099998888");
        when(phoneHasher.hash("01099998888")).thenReturn("ph2");
        when(passwordEncoder.encode("Aa1!aaaa")).thenReturn("enc");
        when(memberRepository.save(any(Member.class)))
                .thenThrow(
                        new DataIntegrityViolationException(
                                "save", new RuntimeException("violates uk_members_phone_hash")));

        assertThatThrownBy(
                        () ->
                                registerMemberUseCase.register(
                                        ADMISSION_TOKEN,
                                        IDEMPOTENCY_KEY,
                                        new RegisterMemberRequest("u@b.com", "이름", "Aa1!aaaa", "01099998888")))
                .isInstanceOf(BusinessException.class)
                .satisfies(
                        ex -> {
                            BusinessException be = (BusinessException) ex;
                            assertThat(be.getStatus()).isEqualTo(HttpStatus.CONFLICT);
                            assertThat(be.getErrorCode()).isEqualTo(ErrorCode.MEMBER_DUPLICATE_PHONE);
                        });
    }

    @Test
    void 성공_후_입장_해제가_실패하면_정리_실패를_즉시_전파한다() {
        when(idempotencyStore.acquire(IDEMPOTENCY_KEY)).thenReturn(true);
        Member saved = org.mockito.Mockito.mock(Member.class);
        when(phoneHasher.normalize("01012345678")).thenReturn("01012345678");
        when(phoneHasher.hash("01012345678")).thenReturn("h");
        when(passwordEncoder.encode("Aa1!aaaa")).thenReturn("enc");
        when(memberRepository.save(any(Member.class))).thenReturn(saved);
        doThrow(new IllegalStateException("cleanup failed")).when(admissionStore).release(ADMISSION_TOKEN);

        assertThatThrownBy(() -> registerMemberUseCase.register(ADMISSION_TOKEN, IDEMPOTENCY_KEY, request))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("cleanup failed");

        verify(idempotencyStore).clear(IDEMPOTENCY_KEY);
        verify(admissionStore).release(ADMISSION_TOKEN);
    }

    @Test
    void 회원_저장_실패와_입장_해제_실패가_함께_발생하면_원인_예외에_정리_실패를_보존한다() {
        when(idempotencyStore.acquire(IDEMPOTENCY_KEY)).thenReturn(true);
        when(phoneHasher.normalize("01012345678")).thenReturn("01012345678");
        when(phoneHasher.hash("01012345678")).thenReturn("h");
        when(passwordEncoder.encode("Aa1!aaaa")).thenReturn("enc");
        BusinessException duplicateEmail = new BusinessException(ErrorCode.MEMBER_DUPLICATE_EMAIL);
        when(memberRepository.save(any(Member.class))).thenThrow(duplicateEmail);
        doThrow(new IllegalStateException("cleanup failed")).when(admissionStore).release(ADMISSION_TOKEN);

        assertThatThrownBy(() -> registerMemberUseCase.register(ADMISSION_TOKEN, IDEMPOTENCY_KEY, request))
                .isInstanceOf(BusinessException.class)
                .satisfies(
                        ex -> {
                            BusinessException be = (BusinessException) ex;
                            assertThat(be.getErrorCode()).isEqualTo(ErrorCode.MEMBER_DUPLICATE_EMAIL);
                            assertThat(be.getSuppressed()).hasSize(1);
                            assertThat(be.getSuppressed()[0]).isInstanceOf(IllegalStateException.class);
                        });

        verify(idempotencyStore).clear(IDEMPOTENCY_KEY);
        verify(admissionStore).release(ADMISSION_TOKEN);
    }
}
