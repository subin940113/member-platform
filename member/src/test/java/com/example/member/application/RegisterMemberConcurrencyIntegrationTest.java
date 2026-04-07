package com.example.member.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.lenient;

import com.example.app.MemberPlatformApplication;
import com.example.app.testsupport.AbstractPostgresIntegrationTest;
import com.example.common.exception.BusinessException;
import com.example.member.infrastructure.register.RegisterAdmissionStore;
import com.example.member.infrastructure.register.RegisterIdempotencyStore;
import com.example.member.presentation.dto.request.RegisterMemberRequest;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.HttpStatus;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest(classes = MemberPlatformApplication.class)
@ActiveProfiles("test")
class RegisterMemberConcurrencyIntegrationTest extends AbstractPostgresIntegrationTest {

    private static final String ADMISSION_TOKEN = "concurrent-admission-token";

    @Autowired
    private RegisterMemberUseCase registerMemberUseCase;

    @MockBean
    private RegisterAdmissionStore admissionStore;

    @MockBean
    private RegisterIdempotencyStore idempotencyStore;

    @BeforeEach
    void 입장과_멱등_저장소를_성공_경로로_스텁한다() {
        lenient().doNothing().when(admissionStore).validate(anyString());
        lenient().when(idempotencyStore.acquire(anyString())).thenReturn(true);
        lenient().doNothing().when(admissionStore).release(anyString());
        lenient().doNothing().when(idempotencyStore).clear(anyString());
    }

    @Test
    void 같은_이메일로_동시에_회원가입하면_하나만_성공한다() throws Exception {
        String email = "concurrent-" + System.nanoTime() + "@test.com";
        String password = "Aa1!aaaa";
        String phone = "01012345678";

        int threads = 32;
        ExecutorService executor = Executors.newFixedThreadPool(threads);
        CountDownLatch latch = new CountDownLatch(threads);
        AtomicInteger success = new AtomicInteger();
        AtomicInteger conflict = new AtomicInteger();
        AtomicInteger other = new AtomicInteger();

        for (int i = 0; i < threads; i++) {
            executor.submit(
                    () -> {
                        try {
                            registerMemberUseCase.register(
                                    ADMISSION_TOKEN,
                                    UUID.randomUUID().toString(),
                                    new RegisterMemberRequest(email, "동시가입", password, phone));
                            success.incrementAndGet();
                        } catch (BusinessException ex) {
                            if (ex.getStatus() == HttpStatus.CONFLICT) {
                                conflict.incrementAndGet();
                            } else {
                                other.incrementAndGet();
                            }
                        } catch (Exception ex) {
                            other.incrementAndGet();
                        } finally {
                            latch.countDown();
                        }
                    });
        }

        assertThat(latch.await(30, TimeUnit.SECONDS)).isTrue();
        executor.shutdown();

        assertThat(success).hasValue(1);
        assertThat(conflict.get()).isEqualTo(threads - 1);
        assertThat(other).hasValue(0);
    }

    @Test
    void 같은_휴대폰_번호로_동시에_회원가입하면_하나만_성공한다() throws Exception {
        String phone = String.format("010%08d", Math.abs(System.nanoTime()) % 100_000_000);
        String password = "Aa1!aaaa";

        int threads = 32;
        ExecutorService executor = Executors.newFixedThreadPool(threads);
        CountDownLatch latch = new CountDownLatch(threads);
        AtomicInteger success = new AtomicInteger();
        AtomicInteger conflict = new AtomicInteger();
        AtomicInteger other = new AtomicInteger();

        for (int i = 0; i < threads; i++) {
            final int index = i;
            executor.submit(
                    () -> {
                        try {
                            registerMemberUseCase.register(
                                    ADMISSION_TOKEN,
                                    UUID.randomUUID().toString(),
                                    new RegisterMemberRequest(
                                            "phone-dup-" + index + "-" + System.nanoTime() + "@test.com",
                                            "동시가입",
                                            password,
                                            phone));
                            success.incrementAndGet();
                        } catch (BusinessException ex) {
                            if (ex.getStatus() == HttpStatus.CONFLICT) {
                                conflict.incrementAndGet();
                            } else {
                                other.incrementAndGet();
                            }
                        } catch (Exception ex) {
                            other.incrementAndGet();
                        } finally {
                            latch.countDown();
                        }
                    });
        }

        assertThat(latch.await(30, TimeUnit.SECONDS)).isTrue();
        executor.shutdown();

        assertThat(success).hasValue(1);
        assertThat(conflict.get()).isEqualTo(threads - 1);
        assertThat(other).hasValue(0);
    }
}
