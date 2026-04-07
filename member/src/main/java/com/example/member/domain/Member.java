package com.example.member.domain;

import com.example.common.domain.BaseEntity;
import com.example.member.infrastructure.crypto.PiiCryptoConverter;
import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.io.Serializable;
import java.time.Instant;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "members")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Member extends BaseEntity implements Serializable {

    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String email;

    @Convert(converter = PiiCryptoConverter.class)
    @Column(name = "name_encrypted", nullable = false, length = 2048)
    private String name;

    @Convert(converter = PiiCryptoConverter.class)
    @Column(name = "phone_encrypted", nullable = false, length = 2048)
    private String phone;

    @Column(name = "phone_hash", length = 64, unique = true)
    private String phoneHash;

    @Column(name = "password_hash", nullable = false)
    private String passwordHash;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private MemberRole role;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private MemberStatus status;

    @Column(name = "login_fail_count", nullable = false)
    private int loginFailCount;

    @Column(name = "login_locked_until")
    private Instant loginLockedUntil;

    @Column(name = "withdrawn_at")
    private Instant withdrawnAt;

    private Member(
            Long id,
            String email,
            String name,
            String phone,
            String phoneHash,
            String passwordHash,
            MemberRole role,
            MemberStatus status,
            Instant withdrawnAt) {
        this.id = id;
        this.email = email;
        this.name = name;
        this.phone = phone;
        this.phoneHash = phoneHash;
        this.passwordHash = passwordHash;
        this.role = role;
        this.status = status;
        this.loginFailCount = 0;
        this.loginLockedUntil = null;
        this.withdrawnAt = withdrawnAt;
    }

    public static Member create(
            String email,
            String name,
            String phone,
            String phoneHash,
            String passwordHash,
            MemberRole role,
            MemberStatus status) {
        return new Member(null, email, name, phone, phoneHash, passwordHash, role, status, null);
    }

    public boolean isActive() {
        return status == MemberStatus.ACTIVE;
    }

    public boolean isLoginLocked() {
        return loginLockedUntil != null && Instant.now().isBefore(loginLockedUntil);
    }

    public void updateProfile(String name, String phone, String phoneHash) {
        this.name = name;
        this.phone = phone;
        this.phoneHash = phoneHash;
    }

    public void withdraw(String encodedRandomPassword) {
        String suffix = UUID.randomUUID().toString().replace("-", "");
        long phoneDigit = Math.floorMod(this.id, 1_000_000_000L);
        this.email = "deleted-" + this.id + "-" + suffix + "@invalid.local";
        this.name = "탈퇴회원";
        this.phone = String.format("00%09d", phoneDigit);
        this.phoneHash = null;
        this.passwordHash = encodedRandomPassword;
        this.status = MemberStatus.WITHDRAWN;
        this.withdrawnAt = Instant.now();
    }
}
