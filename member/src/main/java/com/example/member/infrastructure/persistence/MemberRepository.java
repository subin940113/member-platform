package com.example.member.infrastructure.persistence;

import com.example.member.domain.Member;
import com.example.member.domain.MemberStatus;
import java.time.Instant;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface MemberRepository extends JpaRepository<Member, Long> {

    Optional<Member> findByEmailAndStatus(String email, MemberStatus status);

    boolean existsByEmail(String email);

    boolean existsByPhoneHash(String phoneHash);

    Optional<Member> findByIdAndStatus(Long id, MemberStatus status);

    @Modifying
    @Query("UPDATE Member m SET m.loginFailCount = m.loginFailCount + 1 WHERE m.id = :id")
    void incrementLoginFailCount(@Param("id") Long id);

    @Modifying
    @Query("UPDATE Member m SET m.loginLockedUntil = :lockedUntil " +
            "WHERE m.id = :id AND m.loginFailCount >= :maxFailures " +
            "AND (m.loginLockedUntil IS NULL OR m.loginLockedUntil < :now)")
    void lockAccountIfThresholdReached(@Param("id") Long id,
                                       @Param("lockedUntil") Instant lockedUntil,
                                       @Param("maxFailures") int maxFailures,
                                       @Param("now") Instant now);

    @Modifying
    @Query("UPDATE Member m SET m.loginFailCount = 0, m.loginLockedUntil = null WHERE m.id = :id")
    void clearLoginFailure(@Param("id") Long id);
}
