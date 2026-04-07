package com.example.auth.infrastructure.persistence;

import com.example.auth.domain.RefreshToken;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface RefreshTokenRepository extends JpaRepository<RefreshToken, Long> {

    Optional<RefreshToken> findByJtiAndRevokedFalse(String jti);

    @Modifying
    @Query("update RefreshToken r set r.revoked = true where r.member.id = :memberId and r.revoked = false")
    int revokeAllForMember(@Param("memberId") Long memberId);
}

