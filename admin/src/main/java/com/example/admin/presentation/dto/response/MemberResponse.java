package com.example.admin.presentation.dto.response;

import com.example.admin.presentation.masking.PiiMasking;
import com.example.member.domain.Member;
import com.example.member.domain.MemberRole;
import com.example.member.domain.MemberStatus;
import com.fasterxml.jackson.annotation.JsonInclude;
import java.time.Instant;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record MemberResponse(
        Long id,
        String email,
        String name,
        String phone,
        MemberRole role,
        MemberStatus status,
        Instant createdAt,
        Instant updatedAt,
        Instant withdrawnAt) {

    public static MemberResponse from(Member member) {
        return new MemberResponse(
                member.getId(),
                PiiMasking.maskEmail(member.getEmail()),
                PiiMasking.maskName(member.getName()),
                PiiMasking.maskPhone(member.getPhone()),
                member.getRole(),
                member.getStatus(),
                member.getCreatedAt(),
                member.getUpdatedAt(),
                member.getWithdrawnAt());
    }
}
