package com.example.member.presentation.dto.response;

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
