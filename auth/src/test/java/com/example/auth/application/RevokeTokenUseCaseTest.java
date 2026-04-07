package com.example.auth.application;

import static org.mockito.Mockito.verify;

import com.example.auth.infrastructure.persistence.RefreshTokenRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class RevokeTokenUseCaseTest {

    @Mock
    private RefreshTokenRepository refreshTokenRepository;

    @InjectMocks
    private RevokeTokenUseCase revokeTokenUseCase;

    @Test
    void 토큰_폐기_시_해당_회원의_리프레시_토큰을_전부_무효화한다() {
        revokeTokenUseCase.revoke(9L);

        verify(refreshTokenRepository).revokeAllForMember(9L);
    }
}
