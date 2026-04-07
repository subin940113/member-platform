package com.example.auth.presentation;

import com.example.auth.application.IssueTokenUseCase;
import com.example.auth.application.RefreshTokenUseCase;
import com.example.auth.application.RevokeTokenUseCase;
import com.example.auth.application.result.AuthTokenResult;
import com.example.auth.presentation.dto.request.TokenRequest;
import com.example.auth.presentation.dto.request.RefreshTokenRequest;
import com.example.auth.presentation.dto.response.TokenResponse;
import com.example.common.annotation.MemberId;
import com.example.common.response.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class AuthController {

    private final IssueTokenUseCase issueTokenUseCase;
    private final RefreshTokenUseCase refreshTokenUseCase;
    private final RevokeTokenUseCase revokeTokenUseCase;

    @PostMapping("/token")
    public ApiResponse<TokenResponse> issue(@Valid @RequestBody TokenRequest request) {
        AuthTokenResult tokens = issueTokenUseCase.issue(request.email(), request.password());
        return ApiResponse.ok(new TokenResponse(tokens.accessToken(), tokens.refreshToken()));
    }

    @PutMapping("/token")
    public ApiResponse<TokenResponse> refresh(@Valid @RequestBody RefreshTokenRequest request) {
        AuthTokenResult tokens = refreshTokenUseCase.refresh(request.refreshToken());
        return ApiResponse.ok(new TokenResponse(tokens.accessToken(), tokens.refreshToken()));
    }

    @DeleteMapping("/token")
    public ApiResponse<Void> revoke(@MemberId Long memberId) {
        revokeTokenUseCase.revoke(memberId);
        return ApiResponse.ok();
    }
}
