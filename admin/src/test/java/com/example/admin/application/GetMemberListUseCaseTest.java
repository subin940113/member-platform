package com.example.admin.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import com.example.member.domain.Member;
import com.example.member.domain.MemberRole;
import com.example.member.domain.MemberStatus;
import com.example.member.infrastructure.persistence.MemberRepository;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

@ExtendWith(MockitoExtension.class)
class GetMemberListUseCaseTest {

    @Mock
    private MemberRepository memberRepository;

    @InjectMocks
    private GetMemberListUseCase getMemberListUseCase;

    @Test
    void 조회하면_전체_회원_페이지를_반환한다() {
        Member active = Member.create(
                "a@b.com", "홍길동", "01012345678", "hash1", "pw",
                MemberRole.USER, MemberStatus.ACTIVE);
        Member withdrawn = Member.create(
                "b@b.com", "탈퇴자", "01099999999", "hash2", "pw",
                MemberRole.USER, MemberStatus.WITHDRAWN);

        when(memberRepository.findAll(any(PageRequest.class)))
                .thenReturn(new PageImpl<>(List.of(active, withdrawn)));

        Page<Member> result = getMemberListUseCase.getList(PageRequest.of(0, 10));

        assertThat(result.getContent()).hasSize(2);
    }

    @Test
    void 회원이_없으면_빈_페이지를_반환한다() {
        when(memberRepository.findAll(any(PageRequest.class)))
                .thenReturn(new PageImpl<>(List.of()));

        Page<Member> result = getMemberListUseCase.getList(PageRequest.of(0, 10));

        assertThat(result.getContent()).isEmpty();
    }
}
