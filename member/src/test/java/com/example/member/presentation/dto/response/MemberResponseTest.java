package com.example.member.presentation.dto.response;

import static org.assertj.core.api.Assertions.assertThat;

import com.example.member.domain.Member;
import com.example.member.domain.MemberRole;
import com.example.member.domain.MemberStatus;
import org.junit.jupiter.api.Test;

class MemberResponseTest {

    @Test
    void 응답을_만들면_회원의_핵심_필드가_그대로_매핑된다() {
        Member member = Member.create(
                "user@example.com", "홍길동", "01012345678", "hash", "pw",
                MemberRole.USER, MemberStatus.ACTIVE);

        MemberResponse response = MemberResponse.from(member);

        assertThat(response.email()).isEqualTo("user@example.com");
        assertThat(response.name()).isEqualTo("홍길동");
        assertThat(response.phone()).isEqualTo("01012345678");
        assertThat(response.role()).isEqualTo(MemberRole.USER);
        assertThat(response.status()).isEqualTo(MemberStatus.ACTIVE);
        assertThat(response.withdrawnAt()).isNull();
    }

    @Test
    void 본인_응답은_개인정보를_마스킹하지_않는다() {
        Member member = Member.create(
                "user@example.com", "홍길동", "01012345678", "hash", "pw",
                MemberRole.USER, MemberStatus.ACTIVE);

        MemberResponse response = MemberResponse.from(member);

        assertThat(response.email()).isEqualTo("user@example.com");
        assertThat(response.name()).isEqualTo("홍길동");
        assertThat(response.phone()).isEqualTo("01012345678");
    }
}
