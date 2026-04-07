package com.example.admin.application;

import com.example.member.domain.Member;
import com.example.member.infrastructure.persistence.MemberRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class GetMemberListUseCase {

    private final MemberRepository memberRepository;

    @Transactional(readOnly = true)
    public Page<Member> getList(Pageable pageable) {
        return memberRepository.findAll(pageable);
    }
}
