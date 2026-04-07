package com.example.admin.application;

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
class GetMemberDetailUseCaseTest {

    @Mock
    private MemberRepository memberRepository;

    @InjectMocks
    private GetMemberDetailUseCase getMemberDetailUseCase;

    @Test
    void 활성_회원을_조회하면_해당_회원을_반환한다() {
        Member member = Member.create(
                "a@b.com", "홍길동", "01012345678", "hash", "pw",
                MemberRole.USER, MemberStatus.ACTIVE);
        when(memberRepository.findById(1L)).thenReturn(Optional.of(member));

        assertThat(getMemberDetailUseCase.getById(1L)).isSameAs(member);
    }

    @Test
    void 탈퇴한_회원도_관리자는_조회할_수_있다() {
        Member member = Member.create(
                "a@b.com", "홍길동", "01012345678", "hash", "pw",
                MemberRole.USER, MemberStatus.WITHDRAWN);
        when(memberRepository.findById(2L)).thenReturn(Optional.of(member));

        Member result = getMemberDetailUseCase.getById(2L);

        assertThat(result.getStatus()).isEqualTo(MemberStatus.WITHDRAWN);
    }

    @Test
    void 존재하지_않는_회원이면_회원을_찾을_수_없다_예외를_던진다() {
        when(memberRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> getMemberDetailUseCase.getById(99L))
                .isInstanceOf(BusinessException.class)
                .satisfies(ex -> assertThat(((BusinessException) ex).getErrorCode())
                        .isEqualTo(ErrorCode.MEMBER_NOT_FOUND));
    }
}
