package com.example.member.application;

import com.example.common.exception.BusinessException;
import com.example.common.exception.ErrorCode;
import com.example.member.domain.Member;
import com.example.member.infrastructure.cache.MemberCacheNames;
import com.example.member.infrastructure.persistence.MemberRepository;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class WithdrawMemberUseCase {

    private final MemberRepository memberRepository;
    private final RefreshTokenRevoker refreshTokenRevoker;
    private final PasswordEncoder passwordEncoder;

    @CacheEvict(
            cacheNames = {MemberCacheNames.MEMBER_STATUS, MemberCacheNames.MEMBER},
            key = "#memberId")
    @Transactional
    public void withdraw(Long memberId) {
        Member member =
                memberRepository
                        .findById(memberId)
                        .orElseThrow(() -> new BusinessException(ErrorCode.MEMBER_NOT_FOUND));
        if (!member.isActive()) {
            throw new BusinessException(ErrorCode.MEMBER_ALREADY_WITHDRAWN);
        }

        String anonymizedPassword = passwordEncoder.encode(UUID.randomUUID().toString());
        member.withdraw(anonymizedPassword);
        refreshTokenRevoker.revokeAllForMember(memberId);
    }
}
