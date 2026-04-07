package com.example.auth.application;

import com.example.auth.infrastructure.persistence.RefreshTokenRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class RevokeTokenUseCase {

    private final RefreshTokenRepository refreshTokenRepository;

    @Transactional
    public void revoke(Long memberId) {
        refreshTokenRepository.revokeAllForMember(memberId);
    }
}
