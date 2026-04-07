package com.example.auth.infrastructure.persistence;

import com.example.member.application.RefreshTokenRevoker;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
public class JpaRefreshTokenRevoker implements RefreshTokenRevoker {

    private final RefreshTokenRepository refreshTokenRepository;

    @Override
    @Transactional
    public void revokeAllForMember(Long memberId) {
        refreshTokenRepository.revokeAllForMember(memberId);
    }
}

