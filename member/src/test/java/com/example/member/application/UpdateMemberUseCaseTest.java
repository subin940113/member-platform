package com.example.member.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.example.common.exception.BusinessException;
import com.example.common.exception.ErrorCode;
import com.example.member.domain.Member;
import com.example.member.domain.MemberRole;
import com.example.member.domain.MemberStatus;
import com.example.member.infrastructure.crypto.PhoneHasher;
import com.example.member.infrastructure.persistence.MemberRepository;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class UpdateMemberUseCaseTest {

    @Mock
    private MemberRepository memberRepository;

    @Mock
    private PhoneHasher phoneHasher;

    @InjectMocks
    private UpdateMemberUseCase updateMemberUseCase;

    private static Member activeMemberWithPhone(String phone, String phoneHash) {
        return Member.create(
                "a@b.com", "홍길동", phone, phoneHash, "pw",
                MemberRole.USER, MemberStatus.ACTIVE);
    }

    @Test
    void 활성_회원이_없으면_회원을_찾을_수_없다_예외를_던진다() {
        when(memberRepository.findByIdAndStatus(99L, MemberStatus.ACTIVE)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> updateMemberUseCase.update(99L, "01012345678", "새이름"))
                .isInstanceOf(BusinessException.class)
                .satisfies(ex -> assertThat(((BusinessException) ex).getErrorCode())
                        .isEqualTo(ErrorCode.MEMBER_NOT_FOUND));
    }

    @Test
    void 새_휴대폰_번호가_이미_사용_중이면_휴대폰_번호_중복_예외를_던진다() {
        Member member = activeMemberWithPhone("01011111111", "oldHash");
        when(memberRepository.findByIdAndStatus(1L, MemberStatus.ACTIVE)).thenReturn(Optional.of(member));
        when(phoneHasher.normalize("01099999999")).thenReturn("01099999999");
        when(phoneHasher.hash("01099999999")).thenReturn("newHash");
        when(memberRepository.existsByPhoneHash("newHash")).thenReturn(true);

        assertThatThrownBy(() -> updateMemberUseCase.update(1L, "01099999999", "새이름"))
                .isInstanceOf(BusinessException.class)
                .satisfies(ex -> assertThat(((BusinessException) ex).getErrorCode())
                        .isEqualTo(ErrorCode.MEMBER_DUPLICATE_PHONE));
    }

    @Test
    void 휴대폰_번호가_변경되고_중복이_없으면_수정에_성공한다() {
        Member member = activeMemberWithPhone("01011111111", "oldHash");
        when(memberRepository.findByIdAndStatus(1L, MemberStatus.ACTIVE)).thenReturn(Optional.of(member));
        when(phoneHasher.normalize("010-9999-9999")).thenReturn("01099999999");
        when(phoneHasher.hash("01099999999")).thenReturn("newHash");
        when(memberRepository.existsByPhoneHash("newHash")).thenReturn(false);

        Member result = updateMemberUseCase.update(1L, "010-9999-9999", "새이름");

        assertThat(result.getPhone()).isEqualTo("01099999999");
        assertThat(result.getName()).isEqualTo("새이름");
    }

    @Test
    void 동일한_휴대폰_번호면_중복_검사를_수행하지_않는다() {
        Member member = activeMemberWithPhone("01012345678", "sameHash");
        when(memberRepository.findByIdAndStatus(1L, MemberStatus.ACTIVE)).thenReturn(Optional.of(member));
        when(phoneHasher.normalize("01012345678")).thenReturn("01012345678");
        when(phoneHasher.hash("01012345678")).thenReturn("sameHash");

        updateMemberUseCase.update(1L, "01012345678", "새이름");

        verify(memberRepository, never()).existsByPhoneHash("sameHash");
    }

    @Test
    void 이름이_비어_있으면_기존_이름을_유지한다() {
        Member member = activeMemberWithPhone("01012345678", "hash");
        when(memberRepository.findByIdAndStatus(1L, MemberStatus.ACTIVE)).thenReturn(Optional.of(member));
        when(phoneHasher.normalize("01012345678")).thenReturn("01012345678");
        when(phoneHasher.hash("01012345678")).thenReturn("hash");

        Member result = updateMemberUseCase.update(1L, "01012345678", null);

        assertThat(result.getName()).isEqualTo("홍길동");
    }

    @Test
    void 이름이_공백이면_기존_이름을_유지한다() {
        Member member = activeMemberWithPhone("01012345678", "hash");
        when(memberRepository.findByIdAndStatus(1L, MemberStatus.ACTIVE)).thenReturn(Optional.of(member));
        when(phoneHasher.normalize("01012345678")).thenReturn("01012345678");
        when(phoneHasher.hash("01012345678")).thenReturn("hash");

        Member result = updateMemberUseCase.update(1L, "01012345678", "   ");

        assertThat(result.getName()).isEqualTo("홍길동");
    }

    @Test
    void 이름_앞뒤_공백은_제거하고_저장된다() {
        Member member = activeMemberWithPhone("01012345678", "hash");
        when(memberRepository.findByIdAndStatus(1L, MemberStatus.ACTIVE)).thenReturn(Optional.of(member));
        when(phoneHasher.normalize("01012345678")).thenReturn("01012345678");
        when(phoneHasher.hash("01012345678")).thenReturn("hash");

        Member result = updateMemberUseCase.update(1L, "01012345678", "  새이름  ");

        assertThat(result.getName()).isEqualTo("새이름");
    }
}
