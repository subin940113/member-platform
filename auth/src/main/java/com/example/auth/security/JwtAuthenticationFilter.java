package com.example.auth.security;

import com.example.common.Constants;
import com.example.common.exception.ErrorCode;
import com.example.common.response.ApiResponse;
import com.example.member.application.GetMemberUseCase;
import com.example.member.domain.MemberRole;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.lang.NonNull;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private static final String BEARER_PREFIX = "Bearer ";

    public static final String API_V1_PREFIX = "/api/v1";

    private static final String AUTH_TOKEN = API_V1_PREFIX + "/auth/token";
    private static final String POST_MEMBERS = API_V1_PREFIX + "/members";

    private final JwtTokenProvider jwtTokenProvider;
    private final GetMemberUseCase getMemberUseCase;
    private final ObjectMapper objectMapper;

    public static boolean isDeleteAuthToken(HttpServletRequest request) {
        return HttpMethod.DELETE.matches(request.getMethod()) && request.getRequestURI().equals(AUTH_TOKEN);
    }

    public static boolean isJwtFilterSkippedRequest(HttpServletRequest request) {
        String path = request.getRequestURI();
        String method = request.getMethod();
        return (HttpMethod.POST.matches(method) && (path.equals(AUTH_TOKEN) || path.equals(POST_MEMBERS)))
                || (HttpMethod.PUT.matches(method) && path.equals(AUTH_TOKEN));
    }

    @Override
    protected boolean shouldNotFilter(@NonNull HttpServletRequest request) {
        return isJwtFilterSkippedRequest(request);
    }

    @Override
    protected void doFilterInternal(
            @NonNull HttpServletRequest request,
            @NonNull HttpServletResponse response,
            @NonNull FilterChain filterChain)
            throws ServletException, IOException {

        Authentication existing = SecurityContextHolder.getContext().getAuthentication();
        if (existing != null
                && existing.isAuthenticated()
                && existing.getPrincipal() instanceof MemberPrincipal) {
            filterChain.doFilter(request, response);
            return;
        }

        String header = request.getHeader(HttpHeaders.AUTHORIZATION);
        Optional<String> tokenOpt = tryReadBearerAccessToken(header);
        if (tokenOpt.isEmpty()) {
            filterChain.doFilter(request, response);
            return;
        }

        String token = tokenOpt.get();
        try {
            Claims claims = jwtTokenProvider.parseAccessTokenClaims(token);
            if (!authenticateFromAccessClaims(request, response, claims)) {
                return;
            }
        } catch (ExpiredJwtException ex) {
            if (isDeleteAuthToken(request)) {
                try {
                    Claims claims = jwtTokenProvider.accessClaimsFromExpiredToken(ex);
                    if (!authenticateFromAccessClaims(request, response, claims)) {
                        return;
                    }
                } catch (JwtException | IllegalArgumentException e) {
                    rejectInvalidToken(response);
                    return;
                }
            } else {
                SecurityContextHolder.clearContext();
                writeJsonFailure(response, ErrorCode.AUTH_EXPIRED_TOKEN);
                return;
            }
        } catch (JwtException | IllegalArgumentException ex) {
            rejectInvalidToken(response);
            return;
        }
        filterChain.doFilter(request, response);
    }

    private static Optional<String> tryReadBearerAccessToken(String authorizationHeader) {
        if (!StringUtils.hasText(authorizationHeader) || !authorizationHeader.startsWith(BEARER_PREFIX)) {
            return Optional.empty();
        }
        String raw = authorizationHeader.substring(BEARER_PREFIX.length()).trim();
        return StringUtils.hasText(raw) ? Optional.of(raw) : Optional.empty();
    }

    private boolean authenticateFromAccessClaims(HttpServletRequest request, HttpServletResponse response, Claims claims)
            throws IOException {
        Long memberId = jwtTokenProvider.extractMemberId(claims);
        if (memberId == null) {
            rejectInvalidToken(response);
            return false;
        }
        if (!getMemberUseCase.isActive(memberId)) {
            rejectInvalidToken(response);
            return false;
        }
        List<MemberRole> roles = jwtTokenProvider.extractRoles(claims);
        if (roles.isEmpty()) {
            rejectInvalidToken(response);
            return false;
        }
        String email = jwtTokenProvider.extractEmail(claims);
        if (!StringUtils.hasText(email)) {
            rejectInvalidToken(response);
            return false;
        }
        MemberPrincipal principal = new MemberPrincipal(memberId, email, roles);
        UsernamePasswordAuthenticationToken authentication =
                new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities());
        authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
        SecurityContextHolder.getContext().setAuthentication(authentication);
        request.setAttribute(Constants.MEMBER_ID, memberId);
        return true;
    }

    private void rejectInvalidToken(HttpServletResponse response) throws IOException {
        SecurityContextHolder.clearContext();
        writeJsonFailure(response, ErrorCode.AUTH_INVALID_TOKEN);
    }

    private void writeJsonFailure(HttpServletResponse response, ErrorCode errorCode) throws IOException {
        response.setStatus(errorCode.getHttpStatus().value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        response.getWriter()
                .write(objectMapper.writeValueAsString(ApiResponse.fail(errorCode, ApiResponse.newTraceId())));
    }
}
