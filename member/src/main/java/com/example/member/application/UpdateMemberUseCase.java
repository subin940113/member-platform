package com.example.member.application;

import com.example.common.exception.BusinessException;
import com.example.common.exception.ErrorCode;
import com.example.member.domain.Member;
import com.example.member.domain.MemberStatus;
import com.example.member.infrastructure.cache.MemberCacheNames;
import com.example.member.infrastructure.crypto.PhoneHasher;
import com.example.member.infrastructure.persistence.MemberRepository;
import java.util.Objects;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class UpdateMemberUseCase {

    private final MemberRepository memberRepository;
    private final PhoneHasher phoneHasher;

    @CacheEvict(cacheNames = MemberCacheNames.MEMBER, key = "#memberId")
    @Transactional
    public Member update(Long memberId, String phone, String name) {
        Member member =
                memberRepository
                        .findByIdAndStatus(memberId, MemberStatus.ACTIVE)
                        .orElseThrow(() -> new BusinessException(ErrorCode.MEMBER_NOT_FOUND));

        String normalizedPhone = phoneHasher.normalize(phone);
        String newPhoneHash = phoneHasher.hash(normalizedPhone);
        if (!Objects.equals(newPhoneHash, member.getPhoneHash()) && memberRepository.existsByPhoneHash(newPhoneHash)) {
            throw new BusinessException(ErrorCode.MEMBER_DUPLICATE_PHONE);
        }

        String newName = (name == null || name.isBlank()) ? member.getName() : name.strip();
        member.updateProfile(newName, normalizedPhone, newPhoneHash);
        return member;
    }
}
