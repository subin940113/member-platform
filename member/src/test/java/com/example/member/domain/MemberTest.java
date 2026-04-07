package com.example.member.domain;

import static org.assertj.core.api.Assertions.assertThat;

import java.lang.reflect.Field;
import java.time.Instant;
import org.junit.jupiter.api.Test;

class MemberTest {

    private static Member activeUser() {
        return Member.create(
                "test@example.com", "홍길동", "01012345678", "phoneHash", "passwordHash",
                MemberRole.USER, MemberStatus.ACTIVE);
    }

    private static void setId(Member member, Long id) throws Exception {
        Field idField = Member.class.getDeclaredField("id");
        idField.setAccessible(true);
        idField.set(member, id);
    }

    private static void setLoginLockedUntil(Member member, Instant lockedUntil) throws Exception {
        Field f = Member.class.getDeclaredField("loginLockedUntil");
        f.setAccessible(true);
        f.set(member, lockedUntil);
    }

    @Test
    void 회원을_생성하면_핵심_필드가_올바르게_설정된다() {
        Member member = Member.create(
                "a@b.com", "테스터", "01012345678", "h", "pw", MemberRole.USER, MemberStatus.ACTIVE);

        assertThat(member.getId()).isNull();
        assertThat(member.getEmail()).isEqualTo("a@b.com");
        assertThat(member.getName()).isEqualTo("테스터");
        assertThat(member.getPhone()).isEqualTo("01012345678");
        assertThat(member.getPhoneHash()).isEqualTo("h");
        assertThat(member.getPasswordHash()).isEqualTo("pw");
        assertThat(member.getRole()).isEqualTo(MemberRole.USER);
        assertThat(member.getStatus()).isEqualTo(MemberStatus.ACTIVE);
        assertThat(member.getWithdrawnAt()).isNull();
    }

    @Test
    void 활성_상태_회원은_계정이_활성으로_판별된다() {
        assertThat(activeUser().isActive()).isTrue();
    }

    @Test
    void 탈퇴_상태_회원은_계정이_활성이_아니라고_판별된다() {
        Member member = Member.create(
                "a@b.com", "테스터", "01012345678", "h", "pw",
                MemberRole.USER, MemberStatus.WITHDRAWN);

        assertThat(member.isActive()).isFalse();
    }

    @Test
    void 프로필을_갱신하면_이름과_휴대폰_번호와_휴대폰_번호_해시가_변경된다() {
        Member member = activeUser();

        member.updateProfile("새이름", "01099999999", "newHash");

        assertThat(member.getName()).isEqualTo("새이름");
        assertThat(member.getPhone()).isEqualTo("01099999999");
        assertThat(member.getPhoneHash()).isEqualTo("newHash");
    }

    @Test
    void 프로필을_갱신해도_이메일과_상태는_변경되지_않는다() {
        Member member = activeUser();
        String originalEmail = member.getEmail();

        member.updateProfile("새이름", "01099999999", "newHash");

        assertThat(member.getEmail()).isEqualTo(originalEmail);
        assertThat(member.getStatus()).isEqualTo(MemberStatus.ACTIVE);
    }

    @Test
    void 탈퇴를_처리하면_상태가_탈퇴로_바뀌고_휴대폰_번호_해시는_비어_있다() throws Exception {
        Member member = activeUser();
        setId(member, 42L);

        member.withdraw("encodedRandomPassword");

        assertThat(member.getStatus()).isEqualTo(MemberStatus.WITHDRAWN);
        assertThat(member.getPhoneHash()).isNull();
        assertThat(member.getWithdrawnAt()).isNotNull();
    }

    @Test
    void 탈퇴를_처리하면_이메일은_회원_식별자를_담은_익명화_형식으로_바뀐다() throws Exception {
        Member member = activeUser();
        setId(member, 7L);

        member.withdraw("encodedRandomPassword");

        assertThat(member.getEmail()).startsWith("deleted-7-");
        assertThat(member.getEmail()).endsWith("@invalid.local");
    }

    @Test
    void 탈퇴를_처리하면_이름이_탈퇴회원으로_바뀐다() throws Exception {
        Member member = activeUser();
        setId(member, 1L);

        member.withdraw("encodedRandomPassword");

        assertThat(member.getName()).isEqualTo("탈퇴회원");
    }

    @Test
    void 탈퇴를_처리하면_비밀번호_해시가_전달한_인코딩값으로_교체된다() throws Exception {
        Member member = activeUser();
        setId(member, 1L);

        member.withdraw("$2a$10$randomEncodedHash");

        assertThat(member.getPasswordHash()).isEqualTo("$2a$10$randomEncodedHash");
    }

    @Test
    void 탈퇴를_처리하면_탈퇴_시각이_현재_시각_이후로_설정된다() throws Exception {
        Member member = activeUser();
        setId(member, 1L);
        Instant before = Instant.now();

        member.withdraw("encodedRandomPassword");

        assertThat(member.getWithdrawnAt()).isAfterOrEqualTo(before);
    }

    @Test
    void 잠금_종료_시각이_아직_지나지_않았으면_로그인_잠금_상태이다() throws Exception {
        Member member = activeUser();
        setLoginLockedUntil(member, Instant.now().plusSeconds(3600));

        assertThat(member.isLoginLocked()).isTrue();
    }

    @Test
    void 잠금_종료_시각이_이미_지났으면_로그인_잠금_상태가_아니다() throws Exception {
        Member member = activeUser();
        setLoginLockedUntil(member, Instant.now().minusSeconds(60));

        assertThat(member.isLoginLocked()).isFalse();
    }

    @Test
    void 잠금_종료_시각이_없으면_로그인_잠금_상태가_아니다() throws Exception {
        Member member = activeUser();
        setLoginLockedUntil(member, null);

        assertThat(member.isLoginLocked()).isFalse();
    }
}
