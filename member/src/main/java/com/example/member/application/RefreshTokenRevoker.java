package com.example.member.application;

public interface RefreshTokenRevoker {
    void revokeAllForMember(Long memberId);
}

