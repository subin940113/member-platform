package com.example.auth.application;

import com.example.auth.infrastructure.login.LoginLockProperties;
import com.example.member.infrastructure.persistence.MemberRepository;
import java.time.Instant;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
public class LoginAttemptRecorder {

    private final MemberRepository memberRepository;
    private final LoginLockProperties loginLockProperties;

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void recordFailure(Long memberId) {
        Instant now = Instant.now();
        memberRepository.incrementLoginFailCount(memberId);
        memberRepository.lockAccountIfThresholdReached(
                memberId,
                now.plus(loginLockProperties.lockDuration()),
                loginLockProperties.maxFailuresBeforeLock(),
                now);
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void clearFailure(Long memberId) {
        memberRepository.clearLoginFailure(memberId);
    }
}
