package com.example.admin.presentation.dto.response;

import static org.assertj.core.api.Assertions.assertThat;

import com.example.member.domain.Member;
import com.example.member.domain.MemberRole;
import com.example.member.domain.MemberStatus;
import java.lang.reflect.Field;
import java.time.Instant;
import org.junit.jupiter.api.Test;

class MemberPiiResponseTest {

    private static Member sampleMember() {
        return Member.create(
                "user@example.com", "홍길동", "01012345678", "hash", "pw",
                MemberRole.USER, MemberStatus.ACTIVE);
    }

    private static void setId(Member member, Long id) throws Exception {
        Field idField = Member.class.getDeclaredField("id");
        idField.setAccessible(true);
        idField.set(member, id);
    }

    private static void setAuditingTimestamps(Member member, Instant createdAt, Instant updatedAt) throws Exception {
        Class<?> base = Member.class.getSuperclass();
        Field created = base.getDeclaredField("createdAt");
        created.setAccessible(true);
        created.set(member, createdAt);
        Field updated = base.getDeclaredField("updatedAt");
        updated.setAccessible(true);
        updated.set(member, updatedAt);
    }

    @Test
    void 응답을_만들면_이메일과_이름과_휴대폰_번호가_마스킹_없이_그대로_포함된다() {
        Member member = sampleMember();

        MemberPiiResponse response = MemberPiiResponse.from(member);

        assertThat(response.email()).isEqualTo("user@example.com");
        assertThat(response.name()).isEqualTo("홍길동");
        assertThat(response.phone()).isEqualTo("01012345678");
    }

    @Test
    void 응답을_만들면_역할과_상태가_그대로_포함된다() {
        Member member = sampleMember();

        MemberPiiResponse response = MemberPiiResponse.from(member);

        assertThat(response.role()).isEqualTo(MemberRole.USER);
        assertThat(response.status()).isEqualTo(MemberStatus.ACTIVE);
    }

    @Test
    void 식별자를_가진_회원이면_응답에_그_식별자가_담긴다() throws Exception {
        Member member = sampleMember();
        setId(member, 42L);

        MemberPiiResponse response = MemberPiiResponse.from(member);

        assertThat(response.id()).isEqualTo(42L);
    }

    @Test
    void 생성_수정_시각이_있으면_응답에_그대로_담긴다() throws Exception {
        Member member = sampleMember();
        Instant created = Instant.parse("2024-01-01T00:00:00Z");
        Instant updated = Instant.parse("2024-06-01T12:00:00Z");
        setAuditingTimestamps(member, created, updated);

        MemberPiiResponse response = MemberPiiResponse.from(member);

        assertThat(response.createdAt()).isEqualTo(created);
        assertThat(response.updatedAt()).isEqualTo(updated);
    }

    @Test
    void 탈퇴_처리된_회원이면_탈퇴_상태와_탈퇴_시각이_응답에_반영된다() throws Exception {
        Member member = sampleMember();
        setId(member, 7L);

        member.withdraw("encoded-random");

        MemberPiiResponse response = MemberPiiResponse.from(member);

        assertThat(response.status()).isEqualTo(MemberStatus.WITHDRAWN);
        assertThat(response.withdrawnAt()).isNotNull();
        assertThat(response.email()).startsWith("deleted-7-");
        assertThat(response.name()).isEqualTo("탈퇴회원");
    }
}
