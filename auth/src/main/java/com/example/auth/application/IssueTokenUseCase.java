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
import java.util.Locale;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class IssueTokenUseCase {

    private final MemberRepository memberRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider jwtTokenProvider;
    private final RefreshTokenRepository refreshTokenRepository;
    private final LoginAttemptRecorder loginAttemptRecorder;

    @Transactional
    public AuthTokenResult issue(String email, String rawPassword) {
        Member member =
                memberRepository
                        .findByEmailAndStatus(email.toLowerCase(Locale.ROOT), MemberStatus.ACTIVE)
                        .orElseThrow(() -> new BusinessException(ErrorCode.AUTH_INVALID_CREDENTIALS));

        if (member.isLoginLocked()) {
            throw new BusinessException(ErrorCode.AUTH_ACCOUNT_LOCKED);
        }

        if (!passwordEncoder.matches(rawPassword, member.getPasswordHash())) {
            loginAttemptRecorder.recordFailure(member.getId());
            throw new BusinessException(ErrorCode.AUTH_INVALID_CREDENTIALS);
        }

        refreshTokenRepository.revokeAllForMember(member.getId());

        String accessToken = jwtTokenProvider.createAccessToken(member);
        String refreshToken = jwtTokenProvider.createRefreshToken(member);
        Claims refreshClaims = jwtTokenProvider.parseRefreshTokenClaims(refreshToken);
        refreshTokenRepository.save(
                RefreshToken.create(member, refreshClaims.getId(), refreshClaims.getExpiration().toInstant()));

        loginAttemptRecorder.clearFailure(member.getId());

        return new AuthTokenResult(accessToken, refreshToken);
    }
}
