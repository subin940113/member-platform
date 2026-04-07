package com.example.auth.domain;

import static org.assertj.core.api.Assertions.assertThat;

import com.example.member.domain.Member;
import com.example.member.domain.MemberRole;
import com.example.member.domain.MemberStatus;
import java.time.Instant;
import org.junit.jupiter.api.Test;

class RefreshTokenTest {

    private static Member member() {
        return Member.create(
                "a@b.com", "홍길동", "01012345678", "hash", "pw",
                MemberRole.USER, MemberStatus.ACTIVE);
    }

    @Test
    void 새로_만든_리프레시_토큰은_아직_폐기되지_않았다() {
        Member member = member();
        Instant expiresAt = Instant.now().plusSeconds(604800);

        RefreshToken token = RefreshToken.create(member, "jti-1", expiresAt);

        assertThat(token.isRevoked()).isFalse();
        assertThat(token.getJti()).isEqualTo("jti-1");
        assertThat(token.getExpiresAt()).isEqualTo(expiresAt);
        assertThat(token.getMember()).isSameAs(member);
        assertThat(token.getId()).isNull();
    }

    @Test
    void 폐기하면_리프레시_토큰은_무효_상태가_된다() {
        RefreshToken token = RefreshToken.create(member(), "jti-1", Instant.now().plusSeconds(3600));

        token.revoke();

        assertThat(token.isRevoked()).isTrue();
    }

    @Test
    void 폐기를_여러_번_호출해도_무효_상태가_유지된다() {
        RefreshToken token = RefreshToken.create(member(), "jti-1", Instant.now().plusSeconds(3600));

        token.revoke();
        token.revoke();

        assertThat(token.isRevoked()).isTrue();
    }

    @Test
    void 생성된_토큰의_만료시각은_현재_시각_이후다() {
        RefreshToken token = RefreshToken.create(member(), "jti-1", Instant.now().plusSeconds(3600));

        assertThat(token.getExpiresAt()).isAfter(Instant.now());
    }
}
