package com.example.member.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

import com.example.common.exception.BusinessException;
import com.example.common.exception.ErrorCode;
import com.example.member.domain.Member;
import com.example.member.domain.MemberRole;
import com.example.member.domain.MemberStatus;
import com.example.member.infrastructure.persistence.MemberRepository;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class GetMemberUseCaseTest {

    @Mock
    private MemberRepository memberRepository;

    @InjectMocks
    private GetMemberUseCase getMemberUseCase;

    private static Member activeMember() {
        return Member.create(
                "a@b.com", "홍길동", "01012345678", "hash", "pw",
                MemberRole.USER, MemberStatus.ACTIVE);
    }

    @Test
    void 활성_회원이_있으면_식별자로_조회하면_해당_회원을_반환한다() {
        Member member = activeMember();
        when(memberRepository.findByIdAndStatus(1L, MemberStatus.ACTIVE)).thenReturn(Optional.of(member));

        assertThat(getMemberUseCase.getActiveById(1L)).isSameAs(member);
    }

    @Test
    void 활성_회원이_없으면_식별자로_조회하면_회원을_찾을_수_없다_예외를_던진다() {
        when(memberRepository.findByIdAndStatus(99L, MemberStatus.ACTIVE)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> getMemberUseCase.getActiveById(99L))
                .isInstanceOf(BusinessException.class)
                .satisfies(ex -> assertThat(((BusinessException) ex).getErrorCode())
                        .isEqualTo(ErrorCode.MEMBER_NOT_FOUND));
    }

    @Test
    void 활성_상태_회원이면_계정_활성_여부_조회가_참이다() {
        when(memberRepository.findById(1L)).thenReturn(Optional.of(activeMember()));

        assertThat(getMemberUseCase.isActive(1L)).isTrue();
    }

    @Test
    void 탈퇴_상태_회원이면_계정_활성_여부_조회가_거짓이다() {
        Member withdrawn = Member.create(
                "a@b.com", "홍길동", "01012345678", "hash", "pw",
                MemberRole.USER, MemberStatus.WITHDRAWN);
        when(memberRepository.findById(2L)).thenReturn(Optional.of(withdrawn));

        assertThat(getMemberUseCase.isActive(2L)).isFalse();
    }

    @Test
    void 회원이_존재하지_않으면_계정_활성_여부_조회가_거짓이다() {
        when(memberRepository.findById(99L)).thenReturn(Optional.empty());

        assertThat(getMemberUseCase.isActive(99L)).isFalse();
    }
}
