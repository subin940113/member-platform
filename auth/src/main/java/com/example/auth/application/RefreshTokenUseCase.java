package com.example.auth.application;

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
import io.jsonwebtoken.JwtException;
import java.time.Instant;
import java.util.Objects;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class RefreshTokenUseCase {

    private final JwtTokenProvider jwtTokenProvider;
    private final RefreshTokenRepository refreshTokenRepository;
    private final MemberRepository memberRepository;

    @Transactional
    public AuthTokenResult refresh(String refreshToken) {
        try {
            Claims claims = jwtTokenProvider.parseRefreshTokenClaims(refreshToken);
            Long memberId = jwtTokenProvider.extractMemberId(claims);

            Member member =
                    memberRepository
                            .findByIdAndStatus(memberId, MemberStatus.ACTIVE)
                            .orElseThrow(() -> new BusinessException(ErrorCode.AUTH_INVALID_TOKEN));

            String jti = jwtTokenProvider.extractJti(claims);
            RefreshToken stored =
                    refreshTokenRepository
                            .findByJtiAndRevokedFalse(jti)
                            .orElseThrow(() -> new BusinessException(ErrorCode.AUTH_INVALID_TOKEN));

            if (!Objects.equals(stored.getMember().getId(), member.getId())) {
                throw new BusinessException(ErrorCode.AUTH_INVALID_TOKEN);
            }

            if (stored.getExpiresAt().isBefore(Instant.now())) {
                throw new BusinessException(ErrorCode.AUTH_EXPIRED_TOKEN);
            }

            stored.revoke();

            String accessToken = jwtTokenProvider.createAccessToken(member);
            String newRefreshToken = jwtTokenProvider.createRefreshToken(member);
            Claims newRefreshClaims = jwtTokenProvider.parseRefreshTokenClaims(newRefreshToken);
            refreshTokenRepository.save(
                    RefreshToken.create(member, newRefreshClaims.getId(), newRefreshClaims.getExpiration().toInstant()));

            return new AuthTokenResult(accessToken, newRefreshToken);
        } catch (ExpiredJwtException ex) {
            throw new BusinessException(ErrorCode.AUTH_EXPIRED_TOKEN);
        } catch (JwtException | IllegalArgumentException ex) {
            throw new BusinessException(ErrorCode.AUTH_INVALID_TOKEN);
        }
    }
}
