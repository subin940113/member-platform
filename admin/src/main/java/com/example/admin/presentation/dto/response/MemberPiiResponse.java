package com.example.admin.presentation.dto.response;

import com.example.member.domain.Member;
import com.example.member.domain.MemberRole;
import com.example.member.domain.MemberStatus;
import com.fasterxml.jackson.annotation.JsonInclude;
import java.time.Instant;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record MemberPiiResponse(
        Long id,
        String email,
        String name,
        String phone,
        MemberRole role,
        MemberStatus status,
        Instant createdAt,
        Instant updatedAt,
        Instant withdrawnAt) {

    public static MemberPiiResponse from(Member member) {
        return new MemberPiiResponse(
                member.getId(),
                member.getEmail(),
                member.getName(),
                member.getPhone(),
                member.getRole(),
                member.getStatus(),
                member.getCreatedAt(),
                member.getUpdatedAt(),
                member.getWithdrawnAt());
    }
}
