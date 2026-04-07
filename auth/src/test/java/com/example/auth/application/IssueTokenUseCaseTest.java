package com.example.auth.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import com.example.auth.application.result.AuthTokenResult;
import com.example.auth.infrastructure.persistence.RefreshTokenRepository;
import com.example.auth.security.JwtTokenProvider;
import com.example.common.exception.BusinessException;
import com.example.common.exception.ErrorCode;
import com.example.member.domain.Member;
import com.example.member.domain.MemberStatus;
import com.example.member.infrastructure.persistence.MemberRepository;
import io.jsonwebtoken.Claims;
import java.time.Instant;
import java.util.Date;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

@ExtendWith(MockitoExtension.class)
class IssueTokenUseCaseTest {

    @Mock
    private MemberRepository memberRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private JwtTokenProvider jwtTokenProvider;

    @Mock
    private RefreshTokenRepository refreshTokenRepository;

    @Mock
    private LoginAttemptRecorder loginAttemptRecorder;

    @InjectMocks
    private IssueTokenUseCase issueTokenUseCase;

    @Test
    void 존재하지_않는_이메일이면_로그인은_실패한다() {
        when(memberRepository.findByEmailAndStatus("none@b.com", MemberStatus.ACTIVE))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> issueTokenUseCase.issue("none@b.com", "Aa1!aaaa"))
                .isInstanceOf(BusinessException.class)
                .satisfies(ex -> assertThat(((BusinessException) ex).getErrorCode())
                        .isEqualTo(ErrorCode.AUTH_INVALID_CREDENTIALS));

        verify(passwordEncoder, never()).matches(any(), any());
    }

    @Test
    void 로그인_시_이메일은_소문자로_정규화되어_회원을_조회한다() {
        when(memberRepository.findByEmailAndStatus("user@test.com", MemberStatus.ACTIVE))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> issueTokenUseCase.issue("User@Test.Com", "Aa1!aaaa"))
                .isInstanceOf(BusinessException.class);

        verify(memberRepository).findByEmailAndStatus("user@test.com", MemberStatus.ACTIVE);
        verifyNoMoreInteractions(memberRepository);
    }

    @Test
    void 이메일_대소문자가_입력과_달라도_저장된_소문자와_일치하면_로그인에_성공한다() {
        Member member = Mockito.mock(Member.class);
        when(member.getId()).thenReturn(1L);
        when(member.getPasswordHash()).thenReturn("HASH");
        when(member.isLoginLocked()).thenReturn(false);
        when(memberRepository.findByEmailAndStatus("mix@b.com", MemberStatus.ACTIVE))
                .thenReturn(Optional.of(member));
        when(passwordEncoder.matches("Aa1!aaaa", "HASH")).thenReturn(true);
        when(jwtTokenProvider.createAccessToken(member)).thenReturn("access-token");
        when(jwtTokenProvider.createRefreshToken(member)).thenReturn("refresh-token");

        Claims claims = Mockito.mock(Claims.class);
        when(claims.getId()).thenReturn("jti-1");
        when(claims.getExpiration()).thenReturn(Date.from(Instant.now().plusSeconds(604800)));
        when(jwtTokenProvider.parseRefreshTokenClaims("refresh-token")).thenReturn(claims);

        AuthTokenResult result = issueTokenUseCase.issue("Mix@B.Com", "Aa1!aaaa");

        assertThat(result.accessToken()).isEqualTo("access-token");
        verify(memberRepository).findByEmailAndStatus("mix@b.com", MemberStatus.ACTIVE);
    }

    @Test
    void 계정이_잠금_상태이면_로그인은_실패한다() {
        Member member = Mockito.mock(Member.class);
        when(member.isLoginLocked()).thenReturn(true);
        when(memberRepository.findByEmailAndStatus("a@b.com", MemberStatus.ACTIVE))
                .thenReturn(Optional.of(member));

        assertThatThrownBy(() -> issueTokenUseCase.issue("a@b.com", "Aa1!aaaa"))
                .isInstanceOf(BusinessException.class)
                .satisfies(ex -> assertThat(((BusinessException) ex).getErrorCode())
                        .isEqualTo(ErrorCode.AUTH_ACCOUNT_LOCKED));

        verify(passwordEncoder, never()).matches(any(), any());
        verify(loginAttemptRecorder, never()).recordFailure(any());
    }

    @Test
    void 비밀번호가_틀리면_로그인은_실패하고_실패_횟수가_기록된다() {
        Member member = Mockito.mock(Member.class);
        when(member.getId()).thenReturn(1L);
        when(member.getPasswordHash()).thenReturn("HASH");
        when(member.isLoginLocked()).thenReturn(false);
        when(memberRepository.findByEmailAndStatus("a@b.com", MemberStatus.ACTIVE))
                .thenReturn(Optional.of(member));
        when(passwordEncoder.matches("wrongPassword", "HASH")).thenReturn(false);

        assertThatThrownBy(() -> issueTokenUseCase.issue("a@b.com", "wrongPassword"))
                .isInstanceOf(BusinessException.class)
                .satisfies(ex -> assertThat(((BusinessException) ex).getErrorCode())
                        .isEqualTo(ErrorCode.AUTH_INVALID_CREDENTIALS));

        verify(loginAttemptRecorder).recordFailure(1L);
        verify(jwtTokenProvider, never()).createAccessToken(any());
    }

    @Test
    void 올바른_이메일과_비밀번호로_로그인하면_토큰이_발급된다() {
        Member member = Mockito.mock(Member.class);
        when(member.getId()).thenReturn(1L);
        when(member.getPasswordHash()).thenReturn("HASH");
        when(member.isLoginLocked()).thenReturn(false);
        when(memberRepository.findByEmailAndStatus("a@b.com", MemberStatus.ACTIVE))
                .thenReturn(Optional.of(member));
        when(passwordEncoder.matches("Aa1!aaaa", "HASH")).thenReturn(true);
        when(jwtTokenProvider.createAccessToken(member)).thenReturn("access-token");
        when(jwtTokenProvider.createRefreshToken(member)).thenReturn("refresh-token");

        Claims claims = Mockito.mock(Claims.class);
        when(claims.getId()).thenReturn("jti-1");
        when(claims.getExpiration()).thenReturn(Date.from(Instant.now().plusSeconds(604800)));
        when(jwtTokenProvider.parseRefreshTokenClaims("refresh-token")).thenReturn(claims);

        AuthTokenResult result = issueTokenUseCase.issue("a@b.com", "Aa1!aaaa");

        assertThat(result.accessToken()).isEqualTo("access-token");
        assertThat(result.refreshToken()).isEqualTo("refresh-token");
        verify(refreshTokenRepository).save(any());
        verify(loginAttemptRecorder).clearFailure(1L);
    }
}
