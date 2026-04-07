package com.example.auth.security;

import com.example.auth.infrastructure.jwt.JwtProperties;
import com.example.member.domain.Member;
import com.example.member.domain.MemberRole;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtBuilder;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.UUID;
import javax.crypto.SecretKey;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.stereotype.Component;

@Component
public class JwtTokenProvider {

    public static final String CLAIM_ROLE = "role";
    public static final String CLAIM_ROLES = "roles";

    private static final String CLAIM_TYP = "typ";
    private static final String TYPE_ACCESS = "access";
    private static final String TYPE_REFRESH = "refresh";

    private final JwtProperties properties;
    private final SecretKey key;

    public JwtTokenProvider(JwtProperties properties) {
        this.properties = properties;
        this.key = Keys.hmacShaKeyFor(properties.secret().getBytes(StandardCharsets.UTF_8));
    }

    public String createAccessToken(Member member) {
        return buildToken(member, TYPE_ACCESS, properties.accessTokenValiditySeconds());
    }

    public String createRefreshToken(Member member) {
        return buildToken(member, TYPE_REFRESH, properties.refreshTokenValiditySeconds());
    }

    private String buildToken(Member member, String typ, long validitySeconds) {
        Instant now = Instant.now();
        Instant exp = now.plusSeconds(validitySeconds);
        var builder =
                Jwts.builder()
                        .id(UUID.randomUUID().toString())
                        .subject(member.getEmail())
                        .claim("uid", member.getId())
                        .claim(CLAIM_TYP, typ)
                        .issuedAt(Date.from(now))
                        .expiration(Date.from(exp));
        applyRoleClaims(builder, member);
        return builder.signWith(key).compact();
    }

    public static void applyRoleClaims(JwtBuilder builder, Member member) {
        String name = member.getRole().name();
        builder.claim(CLAIM_ROLE, name).claim(CLAIM_ROLES, List.of(name));
    }

    public static List<MemberRole> readMemberRoles(Claims claims) {
        Object rolesClaim = claims.get(CLAIM_ROLES);
        if (rolesClaim instanceof Collection<?> c) {
            return c.stream().map(String::valueOf).map(MemberRole::valueOf).distinct().toList();
        }
        String single = claims.get(CLAIM_ROLE, String.class);
        return single == null ? List.of() : List.of(MemberRole.valueOf(single));
    }

    public static List<GrantedAuthority> toGrantedAuthorities(List<MemberRole> roles) {
        return roles.stream()
                .distinct()
                .<GrantedAuthority>map(r -> new SimpleGrantedAuthority("ROLE_" + r.name()))
                .toList();
    }

    public Claims parseAccessTokenClaims(String token) {
        Claims claims = parseClaims(token);
        requireTokenType(claims, TYPE_ACCESS);
        return claims;
    }

    public Claims accessClaimsFromExpiredToken(ExpiredJwtException ex) {
        Claims claims = ex.getClaims();
        requireTokenType(claims, TYPE_ACCESS);
        return claims;
    }

    public Claims parseRefreshTokenClaims(String token) {
        Claims claims = parseClaims(token);
        requireTokenType(claims, TYPE_REFRESH);
        return claims;
    }

    private static void requireTokenType(Claims claims, String expected) {
        String typ = claims.get(CLAIM_TYP, String.class);
        if (!expected.equals(typ)) {
            throw new JwtException("unexpected token type: " + typ);
        }
    }

    public Claims parseClaims(String token) {
        return Jwts.parser().verifyWith(key).build().parseSignedClaims(token).getPayload();
    }

    public String extractJti(Claims claims) {
        return claims.getId();
    }

    public Instant extractExpiresAt(Claims claims) {
        return claims.getExpiration().toInstant();
    }

    public List<MemberRole> extractRoles(Claims claims) {
        return readMemberRoles(claims);
    }

    public MemberRole extractRole(Claims claims) {
        return extractRoles(claims).stream().findFirst().orElseThrow(() -> new JwtException("missing role claim"));
    }

    public Long extractMemberId(Claims claims) {
        return claims.get("uid", Long.class);
    }

    public String extractEmail(Claims claims) {
        return claims.getSubject();
    }
}
