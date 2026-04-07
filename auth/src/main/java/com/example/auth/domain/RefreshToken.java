package com.example.auth.domain;

import com.example.common.domain.BaseEntity;
import com.example.member.domain.Member;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.Instant;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "refresh_tokens")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class RefreshToken extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "member_id", nullable = false)
    private Member member;

    @Column(nullable = false, unique = true, length = 64)
    private String jti;

    @Column(name = "expires_at", nullable = false)
    private Instant expiresAt;

    @Column(nullable = false)
    private boolean revoked;

    private RefreshToken(Long id, Member member, String jti, Instant expiresAt, boolean revoked) {
        this.id = id;
        this.member = member;
        this.jti = jti;
        this.expiresAt = expiresAt;
        this.revoked = revoked;
    }

    public static RefreshToken create(Member member, String jti, Instant expiresAt) {
        return new RefreshToken(null, member, jti, expiresAt, false);
    }

    public void revoke() {
        this.revoked = true;
    }
}
