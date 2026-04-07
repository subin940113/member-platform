package com.example.auth.security;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.example.member.application.GetMemberUseCase;
import com.example.common.exception.ErrorCode;
import com.example.member.domain.MemberRole;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import jakarta.servlet.FilterChain;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpHeaders;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.context.SecurityContextHolder;

@ExtendWith(MockitoExtension.class)
class JwtAuthenticationFilterTest {

    @Mock
    private JwtTokenProvider jwtTokenProvider;

    @Mock
    private GetMemberUseCase getMemberUseCase;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Mock
    private FilterChain filterChain;

    @Mock
    private Claims claims;

    private JwtAuthenticationFilter filter;

    @BeforeEach
    void 매_테스트_전에_필터와_시큐리티_컨텍스트를_초기화한다() {
        filter = new JwtAuthenticationFilter(jwtTokenProvider, getMemberUseCase, objectMapper);
        SecurityContextHolder.clearContext();
    }

    @Test
    void 활성_회원이면_인증을_세운다() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader(HttpHeaders.AUTHORIZATION, "Bearer valid.jwt.token");
        MockHttpServletResponse response = new MockHttpServletResponse();

        when(jwtTokenProvider.parseAccessTokenClaims("valid.jwt.token")).thenReturn(claims);
        when(jwtTokenProvider.extractMemberId(claims)).thenReturn(1L);
        when(getMemberUseCase.isActive(1L)).thenReturn(true);
        when(jwtTokenProvider.extractRoles(claims)).thenReturn(List.of(MemberRole.USER));
        when(jwtTokenProvider.extractEmail(claims)).thenReturn("u@test.com");

        filter.doFilterInternal(request, response, filterChain);

        verify(filterChain).doFilter(request, response);
        var auth = SecurityContextHolder.getContext().getAuthentication();
        org.assertj.core.api.Assertions.assertThat(auth).isNotNull();
        org.assertj.core.api.Assertions.assertThat(auth.getPrincipal()).isInstanceOf(MemberPrincipal.class);
        org.assertj.core.api.Assertions.assertThat(((MemberPrincipal) auth.getPrincipal()).getId()).isEqualTo(1L);
    }

    @Test
    void 활성_회원이_아니면_미인증과_유효하지_않은_토큰_코드로_응답한다() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader(HttpHeaders.AUTHORIZATION, "Bearer valid.jwt.token");
        MockHttpServletResponse response = new MockHttpServletResponse();

        when(jwtTokenProvider.parseAccessTokenClaims("valid.jwt.token")).thenReturn(claims);
        when(jwtTokenProvider.extractMemberId(claims)).thenReturn(1L);
        when(getMemberUseCase.isActive(1L)).thenReturn(false);

        filter.doFilterInternal(request, response, filterChain);

        verify(filterChain, never()).doFilter(any(), any());
        org.assertj.core.api.Assertions.assertThat(response.getStatus()).isEqualTo(401);
        org.assertj.core.api.Assertions.assertThat(response.getContentAsString()).contains(ErrorCode.AUTH_INVALID_TOKEN.name());
    }

    @Test
    void 인증_헤더가_없으면_인증_없이_필터_체인만_진행한다() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilterInternal(request, response, filterChain);

        verify(filterChain).doFilter(request, response);
        org.assertj.core.api.Assertions.assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
    }

    @Test
    void 잘못된_액세스_토큰이면_미인증과_유효하지_않은_토큰_코드로_응답한다() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader(HttpHeaders.AUTHORIZATION, "Bearer bad.token");
        MockHttpServletResponse response = new MockHttpServletResponse();

        when(jwtTokenProvider.parseAccessTokenClaims("bad.token")).thenThrow(new JwtException("invalid"));

        filter.doFilterInternal(request, response, filterChain);

        verify(filterChain, never()).doFilter(any(), any());
        org.assertj.core.api.Assertions.assertThat(response.getStatus()).isEqualTo(401);
        org.assertj.core.api.Assertions.assertThat(response.getContentAsString()).contains(ErrorCode.AUTH_INVALID_TOKEN.name());
    }

    @Test
    void 만료된_액세스_토큰이면_로그아웃이_아닌_경로에서는_만료_코드로_응답한다() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setMethod("GET");
        request.setRequestURI("/api/v1/members/me");
        request.addHeader(HttpHeaders.AUTHORIZATION, "Bearer expired.jwt");
        MockHttpServletResponse response = new MockHttpServletResponse();

        ExpiredJwtException expired = org.mockito.Mockito.mock(ExpiredJwtException.class);
        when(jwtTokenProvider.parseAccessTokenClaims("expired.jwt")).thenThrow(expired);

        filter.doFilterInternal(request, response, filterChain);

        verify(filterChain, never()).doFilter(any(), any());
        org.assertj.core.api.Assertions.assertThat(response.getStatus()).isEqualTo(401);
        org.assertj.core.api.Assertions.assertThat(response.getContentAsString()).contains(ErrorCode.AUTH_EXPIRED_TOKEN.name());
    }

    @Test
    void 만료된_액세스_토큰이어도_로그아웃_경로에서는_회원을_인증해_요청을_진행한다() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setMethod("DELETE");
        request.setRequestURI("/api/v1/auth/token");
        request.addHeader(HttpHeaders.AUTHORIZATION, "Bearer expired.jwt");
        MockHttpServletResponse response = new MockHttpServletResponse();

        ExpiredJwtException expired = org.mockito.Mockito.mock(ExpiredJwtException.class);
        when(jwtTokenProvider.parseAccessTokenClaims("expired.jwt")).thenThrow(expired);
        when(jwtTokenProvider.accessClaimsFromExpiredToken(expired)).thenReturn(claims);
        when(jwtTokenProvider.extractMemberId(claims)).thenReturn(7L);
        when(getMemberUseCase.isActive(7L)).thenReturn(true);
        when(jwtTokenProvider.extractRoles(claims)).thenReturn(List.of(MemberRole.USER));
        when(jwtTokenProvider.extractEmail(claims)).thenReturn("logout@test.com");

        filter.doFilterInternal(request, response, filterChain);

        verify(filterChain).doFilter(request, response);
        org.assertj.core.api.Assertions.assertThat(response.getStatus()).isNotEqualTo(401);
        org.assertj.core.api.Assertions.assertThat(SecurityContextHolder.getContext().getAuthentication()).isNotNull();
    }
}
