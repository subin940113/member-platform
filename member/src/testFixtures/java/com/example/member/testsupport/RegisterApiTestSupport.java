package com.example.member.testsupport;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Map;
import java.util.UUID;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

public final class RegisterApiTestSupport {

    private RegisterApiTestSupport() {}

    public static void registerMember(
            MockMvc mockMvc, ObjectMapper objectMapper, String email, String name, String password, String phone)
            throws Exception {
        String admissionJson =
                mockMvc.perform(post("/api/v1/members/admission"))
                        .andExpect(status().isOk())
                        .andReturn()
                        .getResponse()
                        .getContentAsString();
        String admissionToken =
                objectMapper.readTree(admissionJson).path("data").path("admissionToken").asText();
        String idempotencyKey = UUID.randomUUID().toString();
        mockMvc.perform(
                        post("/api/v1/members")
                                .header("Admission-Token", admissionToken)
                                .header("Idempotency-Key", idempotencyKey)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(
                                        objectMapper.writeValueAsString(
                                                Map.of(
                                                        "email", email,
                                                        "name", name,
                                                        "password", password,
                                                        "phone", phone))))
                .andExpect(status().isCreated());
    }
}
