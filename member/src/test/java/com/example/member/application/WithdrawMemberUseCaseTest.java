package com.example.member.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.example.common.exception.BusinessException;
import com.example.common.exception.ErrorCode;
import com.example.member.domain.Member;
import com.example.member.domain.MemberRole;
import com.example.member.domain.MemberStatus;
import com.example.member.infrastructure.persistence.MemberRepository;
import java.lang.reflect.Field;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

@ExtendWith(MockitoExtension.class)
class WithdrawMemberUseCaseTest {

    @Mock
    private MemberRepository memberRepository;

    @Mock
    private RefreshTokenRevoker refreshTokenRevoker;

    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private WithdrawMemberUseCase withdrawMemberUseCase;

    private static Member activeUser(Long id) throws Exception {
        Member member = Member.create(
                "a@b.com", "홍길동", "01012345678", "hash", "pw",
                MemberRole.USER, MemberStatus.ACTIVE);
        Field idField = Member.class.getDeclaredField("id");
        idField.setAccessible(true);
        idField.set(member, id);
        return member;
    }

    @Test
    void 존재하지_않는_회원이면_회원을_찾을_수_없다_예외를_던진다() {
        when(memberRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> withdrawMemberUseCase.withdraw(99L))
                .isInstanceOf(BusinessException.class)
                .satisfies(ex -> assertThat(((BusinessException) ex).getErrorCode())
                        .isEqualTo(ErrorCode.MEMBER_NOT_FOUND));
    }

    @Test
    void 이미_탈퇴한_회원이면_이미_탈퇴한_회원_예외를_던진다() throws Exception {
        Member member = Member.create(
                "a@b.com", "홍길동", "01012345678", "hash", "pw",
                MemberRole.USER, MemberStatus.WITHDRAWN);
        when(memberRepository.findById(1L)).thenReturn(Optional.of(member));

        assertThatThrownBy(() -> withdrawMemberUseCase.withdraw(1L))
                .isInstanceOf(BusinessException.class)
                .satisfies(ex -> assertThat(((BusinessException) ex).getErrorCode())
                        .isEqualTo(ErrorCode.MEMBER_ALREADY_WITHDRAWN));
    }

    @Test
    void 정상_탈퇴_시_회원이_익명화되고_토큰이_폐기된다() throws Exception {
        Member member = activeUser(1L);
        when(memberRepository.findById(1L)).thenReturn(Optional.of(member));
        when(passwordEncoder.encode(anyString())).thenReturn("$2a$10$anonymized");

        withdrawMemberUseCase.withdraw(1L);

        assertThat(member.getStatus()).isEqualTo(MemberStatus.WITHDRAWN);
        assertThat(member.getPhoneHash()).isNull();
        verify(refreshTokenRevoker).revokeAllForMember(1L);
    }

    @Test
    void 탈퇴_시_랜덤_비밀번호를_인코딩하여_저장한다() throws Exception {
        Member member = activeUser(1L);
        when(memberRepository.findById(1L)).thenReturn(Optional.of(member));
        when(passwordEncoder.encode(anyString())).thenReturn("$2a$10$anonymized");

        withdrawMemberUseCase.withdraw(1L);

        verify(passwordEncoder).encode(anyString());
        assertThat(member.getPasswordHash()).isEqualTo("$2a$10$anonymized");
    }
}
