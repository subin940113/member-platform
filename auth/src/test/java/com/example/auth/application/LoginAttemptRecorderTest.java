package com.example.auth.application;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.verify;

import com.example.auth.infrastructure.login.LoginLockProperties;
import com.example.member.infrastructure.persistence.MemberRepository;
import java.time.Duration;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class LoginAttemptRecorderTest {

    @Mock
    private MemberRepository memberRepository;

    @Mock
    private LoginLockProperties loginLockProperties;

    @InjectMocks
    private LoginAttemptRecorder loginAttemptRecorder;

    @BeforeEach
    void 각_테스트_전에_로그인_잠금_기본값을_스텁한다() {
        lenient().when(loginLockProperties.maxFailuresBeforeLock()).thenReturn(5);
        lenient().when(loginLockProperties.lockDuration()).thenReturn(Duration.ofMinutes(30));
    }

    @Test
    void 로그인_실패를_기록하면_실패_횟수_증가와_잠금_조건_갱신을_호출한다() {
        loginAttemptRecorder.recordFailure(7L);

        verify(memberRepository).incrementLoginFailCount(7L);
        verify(memberRepository)
                .lockAccountIfThresholdReached(eq(7L), any(), eq(5), any());
    }

    @Test
    void 로그인_성공_초기화를_호출하면_저장소에서_실패_카운트와_잠금을_지운다() {
        loginAttemptRecorder.clearFailure(3L);

        verify(memberRepository).clearLoginFailure(3L);
    }
}
