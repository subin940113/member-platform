package com.example.member.integration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.example.app.MemberPlatformApplication;
import com.example.app.testsupport.AbstractPostgresIntegrationTest;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest(classes = MemberPlatformApplication.class)
@ActiveProfiles("test")
@AutoConfigureMockMvc
class RegisterMemberFlowIntegrationTest extends AbstractPostgresIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void 입장_발급_후_회원가입하면_응답_본문_없이_생성됨_상태와_위치_헤더만_반환된다() throws Exception {
        String email = "flow-" + System.nanoTime() + "@test.com";
        String phone = String.format("010%08d", Math.abs(System.nanoTime()) % 100_000_000);

        String admissionJson =
                mockMvc.perform(post("/api/v1/members/admission"))
                        .andExpect(status().isOk())
                        .andExpect(jsonPath("$.data.admissionToken").exists())
                        .andExpect(jsonPath("$.data.expiresInSeconds").value(60))
                        .andReturn()
                        .getResponse()
                        .getContentAsString();
        String admissionToken =
                objectMapper.readTree(admissionJson).path("data").path("admissionToken").asText();

        String idempotencyKey = UUID.randomUUID().toString();
        String location =
                mockMvc.perform(
                                post("/api/v1/members")
                                        .header("Admission-Token", admissionToken)
                                        .header("Idempotency-Key", idempotencyKey)
                                        .contentType(MediaType.APPLICATION_JSON)
                                        .content(
                                                objectMapper.writeValueAsString(
                                                        Map.of(
                                                                "email", email,
                                                                "name", "플로우",
                                                                "password", "Aa1!aaaa",
                                                                "phone", phone))))
                        .andExpect(status().isCreated())
                        .andExpect(content().string(""))
                        .andReturn()
                        .getResponse()
                        .getHeader("Location");

        assertThat(location).isNotBlank().contains("/api/v1/members/");
    }

    @Test
    void 입장_토큰_없이_회원가입하면_잘못된_요청과_입장_토큰_필수_오류이다() throws Exception {
        String email = "no-ad-" + System.nanoTime() + "@test.com";
        String phone = String.format("010%08d", Math.abs(System.nanoTime()) % 100_000_000);

        mockMvc.perform(
                        post("/api/v1/members")
                                .header("Idempotency-Key", UUID.randomUUID().toString())
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(
                                        objectMapper.writeValueAsString(
                                                Map.of(
                                                        "email", email,
                                                        "name", "무입장",
                                                        "password", "Aa1!aaaa",
                                                        "phone", phone))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("MEMBER_ADMISSION_REQUIRED"));
    }

    @Test
    void 멱등성_키_없이_회원가입하면_잘못된_요청과_멱등성_키_필수_오류이다() throws Exception {
        String email = "no-idem-" + System.nanoTime() + "@test.com";
        String phone = String.format("010%08d", Math.abs(System.nanoTime()) % 100_000_000);

        String admissionToken =
                objectMapper
                        .readTree(
                                mockMvc.perform(post("/api/v1/members/admission"))
                                        .andExpect(status().isOk())
                                        .andReturn()
                                        .getResponse()
                                        .getContentAsString())
                        .path("data")
                        .path("admissionToken")
                        .asText();

        mockMvc.perform(
                        post("/api/v1/members")
                                .header("Admission-Token", admissionToken)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(
                                        objectMapper.writeValueAsString(
                                                Map.of(
                                                        "email", email,
                                                        "name", "무멱등",
                                                        "password", "Aa1!aaaa",
                                                        "phone", phone))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("MEMBER_IDEMPOTENCY_KEY_REQUIRED"));
    }

    @Test
    void 동일한_멱등성_키로_동시에_요청하면_하나만_성공하고_나머지는_충돌_상태이다() throws Exception {
        String email = "idem-" + System.nanoTime() + "@test.com";
        String phone = String.format("010%08d", Math.abs(System.nanoTime()) % 100_000_000);
        String idempotencyKey = UUID.randomUUID().toString();

        String admissionToken =
                objectMapper
                        .readTree(
                                mockMvc.perform(post("/api/v1/members/admission"))
                                        .andExpect(status().isOk())
                                        .andReturn()
                                        .getResponse()
                                        .getContentAsString())
                        .path("data")
                        .path("admissionToken")
                        .asText();

        String body =
                objectMapper.writeValueAsString(
                        Map.of(
                                "email", email,
                                "name", "멱등",
                                "password", "Aa1!aaaa",
                                "phone", phone));

        int threads = 8;
        ExecutorService executor = Executors.newFixedThreadPool(threads);
        CountDownLatch start = new CountDownLatch(1);
        CountDownLatch done = new CountDownLatch(threads);
        AtomicInteger created = new AtomicInteger();
        AtomicInteger conflict = new AtomicInteger();
        AtomicInteger other = new AtomicInteger();

        for (int i = 0; i < threads; i++) {
            executor.submit(
                    () -> {
                        try {
                            start.await();
                            int status =
                                    mockMvc.perform(
                                                    post("/api/v1/members")
                                                            .header("Admission-Token", admissionToken)
                                                            .header("Idempotency-Key", idempotencyKey)
                                                            .contentType(MediaType.APPLICATION_JSON)
                                                            .content(body))
                                            .andReturn()
                                            .getResponse()
                                            .getStatus();
                            if (status == 201) {
                                created.incrementAndGet();
                            } else if (status == 409) {
                                conflict.incrementAndGet();
                            } else {
                                other.incrementAndGet();
                            }
                        } catch (Exception ex) {
                            other.incrementAndGet();
                        } finally {
                            done.countDown();
                        }
                    });
        }

        start.countDown();
        assertThat(done.await(60, TimeUnit.SECONDS)).isTrue();
        executor.shutdown();

        assertThat(created).hasValue(1);
        assertThat(conflict.get()).isEqualTo(threads - 1);
        assertThat(other).hasValue(0);
    }

    @Test
    void 이미_사용한_입장_토큰으로_다시_회원가입하면_유효하지_않은_입장_토큰_오류이다() throws Exception {
        String firstEmail = "reuse-first-" + System.nanoTime() + "@test.com";
        String firstPhone = String.format("010%08d", Math.abs(System.nanoTime()) % 100_000_000);

        String admissionToken =
                objectMapper
                        .readTree(
                                mockMvc.perform(post("/api/v1/members/admission"))
                                        .andExpect(status().isOk())
                                        .andReturn()
                                        .getResponse()
                                        .getContentAsString())
                        .path("data")
                        .path("admissionToken")
                        .asText();

        mockMvc.perform(
                        post("/api/v1/members")
                                .header("Admission-Token", admissionToken)
                                .header("Idempotency-Key", UUID.randomUUID().toString())
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(
                                        objectMapper.writeValueAsString(
                                                Map.of(
                                                        "email", firstEmail,
                                                        "name", "첫가입",
                                                        "password", "Aa1!aaaa",
                                                        "phone", firstPhone))))
                .andExpect(status().isCreated());

        String secondEmail = "reuse-second-" + System.nanoTime() + "@test.com";
        String secondPhone = String.format("010%08d", Math.abs(System.nanoTime()) % 100_000_000);

        mockMvc.perform(
                        post("/api/v1/members")
                                .header("Admission-Token", admissionToken)
                                .header("Idempotency-Key", UUID.randomUUID().toString())
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(
                                        objectMapper.writeValueAsString(
                                                Map.of(
                                                        "email", secondEmail,
                                                        "name", "재사용",
                                                        "password", "Aa1!aaaa",
                                                        "phone", secondPhone))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("MEMBER_ADMISSION_INVALID"));
    }
}
