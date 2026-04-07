package com.example.auth.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.example.member.domain.MemberRole;
import io.jsonwebtoken.Claims;
import java.util.List;
import org.junit.jupiter.api.Test;

class JwtTokenProviderRoleClaimsTest {

    @Test
    void 역할_목록이_클레임에_있으면_그_값들로_회원_역할을_만든다() {
        Claims claims = mock(Claims.class);
        when(claims.get(JwtTokenProvider.CLAIM_ROLES)).thenReturn(List.of("USER", "ADMIN"));
        when(claims.get(JwtTokenProvider.CLAIM_ROLE, String.class)).thenReturn("USER");

        assertThat(JwtTokenProvider.readMemberRoles(claims)).containsExactly(MemberRole.USER, MemberRole.ADMIN);
    }

    @Test
    void 역할_목록_클레임이_없으면_단일_역할_클레임에서_회원_역할을_만든다() {
        Claims claims = mock(Claims.class);
        when(claims.get(JwtTokenProvider.CLAIM_ROLES)).thenReturn(null);
        when(claims.get(JwtTokenProvider.CLAIM_ROLE, String.class)).thenReturn("USER");

        assertThat(JwtTokenProvider.readMemberRoles(claims)).containsExactly(MemberRole.USER);
    }

    @Test
    void 스프링_권한으로_변환할_때_역할_접두어를_쓰고_중복_역할은_한_번만_포함한다() {
        assertThat(JwtTokenProvider.toGrantedAuthorities(List.of(MemberRole.USER, MemberRole.USER)))
                .hasSize(1)
                .extracting(a -> a.getAuthority())
                .containsExactly("ROLE_USER");
    }

    @Test
    void 역할_목록_클레임에_같은_역할이_반복되면_한_번만_포함한다() {
        Claims claims = mock(Claims.class);
        when(claims.get(JwtTokenProvider.CLAIM_ROLES)).thenReturn(List.of("USER", "USER"));

        assertThat(JwtTokenProvider.readMemberRoles(claims)).containsExactly(MemberRole.USER);
    }
}
