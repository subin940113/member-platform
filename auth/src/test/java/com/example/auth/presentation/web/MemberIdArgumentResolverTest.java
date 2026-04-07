package com.example.auth.presentation.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.example.auth.security.MemberPrincipal;
import com.example.common.exception.BusinessException;
import com.example.common.exception.ErrorCode;
import com.example.member.domain.MemberRole;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.core.MethodParameter;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

class MemberIdArgumentResolverTest {

    private final MemberIdArgumentResolver resolver = new MemberIdArgumentResolver();

    @AfterEach
    void 각_테스트_후에_시큐리티_컨텍스트를_비운다() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void 로그인한_회원_주체가_있으면_회원_식별자를_반환한다() throws Exception {
        MemberPrincipal principal = new MemberPrincipal(42L, "u@test.com", MemberRole.USER);
        SecurityContextHolder.getContext()
                .setAuthentication(new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities()));

        MethodParameter param = MethodParameter.forExecutable(
                TestController.class.getDeclaredMethod("회원_식별자를_받는다", Long.class), 0);

        assertThat(resolver.resolveArgument(param, null, null, null)).isEqualTo(42L);
    }

    @Test
    void 인증이_없으면_인증_필요_예외를_던진다() throws Exception {
        MethodParameter param = MethodParameter.forExecutable(
                TestController.class.getDeclaredMethod("회원_식별자를_받는다", Long.class), 0);

        assertThatThrownBy(() -> resolver.resolveArgument(param, null, null, null))
                .isInstanceOf(BusinessException.class)
                .extracting(ex -> ((BusinessException) ex).getErrorCode())
                .isEqualTo(ErrorCode.AUTH_UNAUTHORIZED);
    }

    @Test
    void 인증_주체가_회원_주체가_아니면_인증_필요_예외를_던진다() throws Exception {
        SecurityContextHolder.getContext()
                .setAuthentication(new UsernamePasswordAuthenticationToken("unknown", null, null));

        MethodParameter param = MethodParameter.forExecutable(
                TestController.class.getDeclaredMethod("회원_식별자를_받는다", Long.class), 0);

        assertThatThrownBy(() -> resolver.resolveArgument(param, null, null, null))
                .isInstanceOf(BusinessException.class)
                .extracting(ex -> ((BusinessException) ex).getErrorCode())
                .isEqualTo(ErrorCode.AUTH_UNAUTHORIZED);
    }

    private static class TestController {
        @SuppressWarnings("unused")
        void 회원_식별자를_받는다(@com.example.common.annotation.MemberId Long memberId) {}
    }
}
