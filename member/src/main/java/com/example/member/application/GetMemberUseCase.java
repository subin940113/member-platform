package com.example.member.application;

import com.example.common.exception.BusinessException;
import com.example.common.exception.ErrorCode;
import com.example.member.domain.Member;
import com.example.member.domain.MemberStatus;
import com.example.member.infrastructure.cache.MemberCacheNames;
import com.example.member.infrastructure.persistence.MemberRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class GetMemberUseCase {

    private final MemberRepository memberRepository;

    @Cacheable(cacheNames = MemberCacheNames.MEMBER, key = "#memberId")
    @Transactional(readOnly = true)
    public Member getActiveById(Long memberId) {
        return memberRepository
                .findByIdAndStatus(memberId, MemberStatus.ACTIVE)
                .orElseThrow(() -> new BusinessException(ErrorCode.MEMBER_NOT_FOUND));
    }

    @Cacheable(cacheNames = MemberCacheNames.MEMBER_STATUS, key = "#memberId")
    @Transactional(readOnly = true)
    public boolean isActive(long memberId) {
        return memberRepository
                .findById(memberId)
                .map(Member::getStatus)
                .map(s -> s == MemberStatus.ACTIVE)
                .orElse(false);
    }
}
