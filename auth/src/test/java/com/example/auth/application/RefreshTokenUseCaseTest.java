package com.example.auth.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.example.auth.application.result.AuthTokenResult;
import com.example.auth.domain.RefreshToken;
import com.example.auth.infrastructure.persistence.RefreshTokenRepository;
import com.example.auth.security.JwtTokenProvider;
import com.example.common.exception.BusinessException;
import com.example.common.exception.ErrorCode;
import com.example.member.domain.Member;
import com.example.member.domain.MemberStatus;
import com.example.member.infrastructure.persistence.MemberRepository;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import java.time.Instant;
import java.util.Date;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class RefreshTokenUseCaseTest {

    @Mock
    private JwtTokenProvider jwtTokenProvider;

    @Mock
    private RefreshTokenRepository refreshTokenRepository;

    @Mock
    private MemberRepository memberRepository;

    @InjectMocks
    private RefreshTokenUseCase refreshTokenUseCase;

    @Test
    void 활성_회원이_아니면_유효하지_않은_토큰_예외를_던지고_저장소를_조회하지_않는다() {
        Claims claims = org.mockito.Mockito.mock(Claims.class);
        when(jwtTokenProvider.parseRefreshTokenClaims("rt")).thenReturn(claims);
        when(jwtTokenProvider.extractMemberId(claims)).thenReturn(99L);
        when(memberRepository.findByIdAndStatus(99L, MemberStatus.ACTIVE)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> refreshTokenUseCase.refresh("rt"))
                .isInstanceOf(BusinessException.class)
                .extracting(ex -> ((BusinessException) ex).getErrorCode())
                .isEqualTo(ErrorCode.AUTH_INVALID_TOKEN);

        verify(refreshTokenRepository, never()).findByJtiAndRevokedFalse(any());
        verify(refreshTokenRepository, never()).save(any());
    }

    @Test
    void 저장된_토큰의_소유자와_클레임_회원이_다르면_유효하지_않은_토큰_예외를_던진다() {
        Claims claims = org.mockito.Mockito.mock(Claims.class);
        when(jwtTokenProvider.parseRefreshTokenClaims("rt")).thenReturn(claims);
        when(jwtTokenProvider.extractMemberId(claims)).thenReturn(1L);

        Member active = org.mockito.Mockito.mock(Member.class);
        when(active.getId()).thenReturn(1L);
        when(memberRepository.findByIdAndStatus(1L, MemberStatus.ACTIVE)).thenReturn(Optional.of(active));

        when(jwtTokenProvider.extractJti(claims)).thenReturn("jti-1");

        RefreshToken stored = org.mockito.Mockito.mock(RefreshToken.class);
        Member other = org.mockito.Mockito.mock(Member.class);
        when(other.getId()).thenReturn(2L);
        when(stored.getMember()).thenReturn(other);
        when(refreshTokenRepository.findByJtiAndRevokedFalse("jti-1")).thenReturn(Optional.of(stored));

        assertThatThrownBy(() -> refreshTokenUseCase.refresh("rt"))
                .isInstanceOf(BusinessException.class)
                .extracting(ex -> ((BusinessException) ex).getErrorCode())
                .isEqualTo(ErrorCode.AUTH_INVALID_TOKEN);

        verify(refreshTokenRepository, never()).save(any());
    }

    @Test
    void 저장된_토큰이_만료되었으면_만료된_토큰_예외를_던진다() {
        Claims claims = org.mockito.Mockito.mock(Claims.class);
        when(jwtTokenProvider.parseRefreshTokenClaims("rt")).thenReturn(claims);
        when(jwtTokenProvider.extractMemberId(claims)).thenReturn(1L);

        Member active = org.mockito.Mockito.mock(Member.class);
        when(active.getId()).thenReturn(1L);
        when(memberRepository.findByIdAndStatus(1L, MemberStatus.ACTIVE)).thenReturn(Optional.of(active));

        when(jwtTokenProvider.extractJti(claims)).thenReturn("jti-1");

        RefreshToken stored = org.mockito.Mockito.mock(RefreshToken.class);
        when(stored.getMember()).thenReturn(active);
        when(stored.getExpiresAt()).thenReturn(Instant.now().minusSeconds(1));
        when(refreshTokenRepository.findByJtiAndRevokedFalse("jti-1")).thenReturn(Optional.of(stored));

        assertThatThrownBy(() -> refreshTokenUseCase.refresh("rt"))
                .isInstanceOf(BusinessException.class)
                .extracting(ex -> ((BusinessException) ex).getErrorCode())
                .isEqualTo(ErrorCode.AUTH_EXPIRED_TOKEN);

        verify(refreshTokenRepository, never()).save(any());
    }

    @Test
    void 유효한_리프레시_토큰이면_새_토큰_쌍을_발급하고_기존_토큰은_폐기한다() {
        Claims claims = org.mockito.Mockito.mock(Claims.class);
        when(jwtTokenProvider.parseRefreshTokenClaims("rt-good")).thenReturn(claims);
        when(jwtTokenProvider.extractMemberId(claims)).thenReturn(1L);

        Member active = org.mockito.Mockito.mock(Member.class);
        when(active.getId()).thenReturn(1L);
        when(memberRepository.findByIdAndStatus(1L, MemberStatus.ACTIVE)).thenReturn(Optional.of(active));

        when(jwtTokenProvider.extractJti(claims)).thenReturn("jti-old");

        RefreshToken stored = org.mockito.Mockito.mock(RefreshToken.class);
        when(stored.getMember()).thenReturn(active);
        when(stored.getExpiresAt()).thenReturn(Instant.now().plusSeconds(10_000));
        when(refreshTokenRepository.findByJtiAndRevokedFalse("jti-old")).thenReturn(Optional.of(stored));

        when(jwtTokenProvider.createAccessToken(active)).thenReturn("new-access");
        when(jwtTokenProvider.createRefreshToken(active)).thenReturn("new-refresh");

        Claims newClaims = org.mockito.Mockito.mock(Claims.class);
        when(newClaims.getId()).thenReturn("jti-new");
        when(newClaims.getExpiration()).thenReturn(Date.from(Instant.now().plusSeconds(604_800)));
        when(jwtTokenProvider.parseRefreshTokenClaims("new-refresh")).thenReturn(newClaims);

        AuthTokenResult result = refreshTokenUseCase.refresh("rt-good");

        assertThat(result.accessToken()).isEqualTo("new-access");
        assertThat(result.refreshToken()).isEqualTo("new-refresh");
        verify(stored).revoke();
        verify(refreshTokenRepository).save(any(RefreshToken.class));
    }

    @Test
    void 저장소에_리프레시_발급_식별자가_없으면_유효하지_않은_토큰_예외를_던진다() {
        Claims claims = org.mockito.Mockito.mock(Claims.class);
        when(jwtTokenProvider.parseRefreshTokenClaims("rt")).thenReturn(claims);
        when(jwtTokenProvider.extractMemberId(claims)).thenReturn(1L);

        Member active = org.mockito.Mockito.mock(Member.class);
        when(memberRepository.findByIdAndStatus(1L, MemberStatus.ACTIVE)).thenReturn(Optional.of(active));

        when(jwtTokenProvider.extractJti(claims)).thenReturn("missing-jti");
        when(refreshTokenRepository.findByJtiAndRevokedFalse("missing-jti")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> refreshTokenUseCase.refresh("rt"))
                .isInstanceOf(BusinessException.class)
                .extracting(ex -> ((BusinessException) ex).getErrorCode())
                .isEqualTo(ErrorCode.AUTH_INVALID_TOKEN);
    }

    @Test
    void 리프레시_토큰_파싱_단계에서_만료_예외가_나면_만료된_토큰_예외로_변환된다() {
        ExpiredJwtException expired = org.mockito.Mockito.mock(ExpiredJwtException.class);
        when(jwtTokenProvider.parseRefreshTokenClaims("expired-rt")).thenThrow(expired);

        assertThatThrownBy(() -> refreshTokenUseCase.refresh("expired-rt"))
                .isInstanceOf(BusinessException.class)
                .extracting(ex -> ((BusinessException) ex).getErrorCode())
                .isEqualTo(ErrorCode.AUTH_EXPIRED_TOKEN);

        verify(memberRepository, never()).findByIdAndStatus(any(), any());
    }
}
