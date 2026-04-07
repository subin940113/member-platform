package com.example.admin.presentation.dto.response;

import static org.assertj.core.api.Assertions.assertThat;

import com.example.member.domain.Member;
import com.example.member.domain.MemberRole;
import com.example.member.domain.MemberStatus;
import org.junit.jupiter.api.Test;

class MemberResponseTest {

    private static Member activeMember() {
        return Member.create(
                "user@example.com", "홍길동", "01012345678", "hash", "pw",
                MemberRole.USER, MemberStatus.ACTIVE);
    }

    @Test
    void 응답을_만들면_이메일_첫_글자만_보이고_나머지는_마스킹된다() {
        MemberResponse response = MemberResponse.from(activeMember());

        assertThat(response.email()).isEqualTo("u***@example.com");
    }

    @Test
    void 응답을_만들면_이름_첫_글자만_보이고_나머지는_마스킹된다() {
        MemberResponse response = MemberResponse.from(activeMember());

        assertThat(response.name()).isEqualTo("홍**");
    }

    @Test
    void 응답을_만들면_휴대폰_번호_앞_3자리와_뒤_4자리를_제외하고_마스킹된다() {
        MemberResponse response = MemberResponse.from(activeMember());

        assertThat(response.phone()).isEqualTo("010****5678");
    }

    @Test
    void 응답을_만들면_역할과_상태는_마스킹_없이_그대로_포함된다() {
        MemberResponse response = MemberResponse.from(activeMember());

        assertThat(response.role()).isEqualTo(MemberRole.USER);
        assertThat(response.status()).isEqualTo(MemberStatus.ACTIVE);
    }

    @Test
    void 응답을_만들면_원본_개인정보가_노출되지_않는다() {
        MemberResponse response = MemberResponse.from(activeMember());

        assertThat(response.email()).isNotEqualTo("user@example.com");
        assertThat(response.name()).isNotEqualTo("홍길동");
        assertThat(response.phone()).isNotEqualTo("01012345678");
    }
}
