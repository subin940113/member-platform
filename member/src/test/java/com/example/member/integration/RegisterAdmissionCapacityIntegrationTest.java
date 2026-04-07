package com.example.member.integration;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.example.app.MemberPlatformApplication;
import com.example.app.testsupport.AbstractPostgresIntegrationTest;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest(classes = MemberPlatformApplication.class)
@ActiveProfiles("test")
@TestPropertySource(properties = "app.register.admission.max-concurrent=1")
@AutoConfigureMockMvc
class RegisterAdmissionCapacityIntegrationTest extends AbstractPostgresIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void 활성_슬롯이_가득_차면_두_번째_입장_요청은_요청_과다_상태이다() throws Exception {
        mockMvc.perform(post("/api/v1/members/admission")).andExpect(status().isOk());

        mockMvc.perform(post("/api/v1/members/admission"))
                .andExpect(status().isTooManyRequests())
                .andExpect(jsonPath("$.code").value("TOO_MANY_REQUESTS"));
    }

    @Test
    void 회원가입이_끝나면_슬롯이_해제되어_다음_입장_요청이_가능하다() throws Exception {
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
                                                        "email", "slot-release-" + System.nanoTime() + "@test.com",
                                                        "name", "슬롯해제",
                                                        "password", "Aa1!aaaa",
                                                        "phone", "01012345678"))))
                .andExpect(status().isCreated());

        mockMvc.perform(post("/api/v1/members/admission"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.admissionToken").exists());
    }
}
