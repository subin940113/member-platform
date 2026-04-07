package com.example.auth.integration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.example.admin.domain.AuditAction;
import com.example.admin.infrastructure.persistence.AdminAuditLogRepository;
import com.example.app.MemberPlatformApplication;
import com.example.app.testsupport.AbstractPostgresIntegrationTest;
import com.example.member.testsupport.RegisterApiTestSupport;
import com.example.auth.security.JwtTokenProvider;
import com.example.member.domain.Member;
import com.example.member.domain.MemberRole;
import com.example.member.domain.MemberStatus;
import com.example.member.infrastructure.crypto.PhoneHasher;
import com.example.member.infrastructure.persistence.MemberRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest(classes = MemberPlatformApplication.class)
@ActiveProfiles("test")
@AutoConfigureMockMvc
class AuthorizationIntegrationTest extends AbstractPostgresIntegrationTest {

    private record Tokens(String accessToken, String refreshToken) {}

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private MemberRepository memberRepository;

    @Autowired
    private JwtTokenProvider jwtTokenProvider;

    @Autowired
    private PhoneHasher phoneHasher;

    @Autowired
    private AdminAuditLogRepository adminAuditLogRepository;

    @Test
    void 인증_토큰이_없으면_관리자_회원_목록은_미인증으로_거절된다() throws Exception {
        mockMvc.perform(get("/api/v1/admin/members")).andExpect(status().isUnauthorized());
    }

    @Test
    void 일반_권한으로_관리자_회원_목록을_요청하면_권한_없음으로_거절된다() throws Exception {
        Member member = createMember("user-" + System.nanoTime() + "@test.com", "사용자", uniquePhone(), MemberRole.USER);

        String accessToken = jwtTokenProvider.createAccessToken(member);

        mockMvc.perform(get("/api/v1/admin/members").header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("AUTH_FORBIDDEN"));
    }

    @Test
    void 탈퇴_후_리프레시_토큰으로_재발급하면_실패한다() throws Exception {
        String email = "withdraw-" + System.nanoTime() + "@test.com";
        String password = "Aa1!aaaa";
        String phone = "01012345678";

        RegisterApiTestSupport.registerMember(mockMvc, objectMapper, email, "탈퇴테스트", password, phone);
        Tokens tokens = login(email, password);

        mockMvc.perform(delete("/api/v1/members/me").header("Authorization", "Bearer " + tokens.accessToken()))
                .andExpect(status().isNoContent());

        mockMvc.perform(
                        put("/api/v1/auth/token")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(Map.of("refreshToken", tokens.refreshToken()))))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("AUTH_INVALID_TOKEN"));
    }

    @Test
    void 탈퇴_후_기존_액세스_토큰으로_내_정보를_조회하면_활성이_아니어서_거절된다() throws Exception {
        String email = "withdraw-acc-" + System.nanoTime() + "@test.com";
        String password = "Aa1!aaaa";
        String phone = String.format("010%08d", Math.abs(System.nanoTime()) % 100_000_000);

        RegisterApiTestSupport.registerMember(mockMvc, objectMapper, email, "탈퇴액세스", password, phone);
        Tokens tokens = login(email, password);

        mockMvc.perform(delete("/api/v1/members/me").header("Authorization", "Bearer " + tokens.accessToken()))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/api/v1/members/me").header("Authorization", "Bearer " + tokens.accessToken()))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("AUTH_INVALID_TOKEN"));
    }

    @Test
    void 로그아웃_후_리프레시_토큰으로_재발급하면_실패한다() throws Exception {
        String email = "logout-rf-" + System.nanoTime() + "@test.com";
        String password = "Aa1!aaaa";
        String phone = String.format("010%08d", Math.abs(System.nanoTime()) % 100_000_000);

        RegisterApiTestSupport.registerMember(mockMvc, objectMapper, email, "로그아웃리프", password, phone);
        Tokens tokens = login(email, password);

        mockMvc.perform(delete("/api/v1/auth/token").header("Authorization", "Bearer " + tokens.accessToken()))
                .andExpect(status().isOk());

        mockMvc.perform(
                        put("/api/v1/auth/token")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(Map.of("refreshToken", tokens.refreshToken()))))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("AUTH_INVALID_TOKEN"));
    }

    @Test
    void 로그인_후_엑세스_토큰으로_내_정보_조회가_성공한다() throws Exception {
        String email = "me-ok-" + System.nanoTime() + "@test.com";
        String password = "Aa1!aaaa";
        String phone = String.format("010%08d", Math.abs(System.nanoTime()) % 100_000_000);

        RegisterApiTestSupport.registerMember(mockMvc, objectMapper, email, "본인조회", password, phone);
        Tokens tokens = login(email, password);

        mockMvc.perform(get("/api/v1/members/me").header("Authorization", "Bearer " + tokens.accessToken()))
                .andExpect(status().isOk());
    }

    @Test
    void 관리자_권한으로_관리자_회원_목록_조회가_성공한다() throws Exception {
        Member admin = createMember("admin-" + System.nanoTime() + "@test.com", "관리자", uniquePhone(), MemberRole.ADMIN);

        String accessToken = jwtTokenProvider.createAccessToken(admin);

        mockMvc.perform(get("/api/v1/admin/members").header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isOk());
    }

    @Test
    void 관리자_목록_조회_성공_시_감사_로그에_목록_조회_행위가_기록된다() throws Exception {
        long before = adminAuditLogRepository.count();
        Member admin = createMember("audit-admin-" + System.nanoTime() + "@test.com", "감사관리자", uniquePhone(), MemberRole.ADMIN);

        String accessToken = jwtTokenProvider.createAccessToken(admin);

        mockMvc.perform(get("/api/v1/admin/members").header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isOk());

        assertThat(adminAuditLogRepository.count()).isEqualTo(before + 1);
        assertThat(
                        adminAuditLogRepository.findAll().stream()
                                .filter(l -> l.getAdminId().equals(admin.getId()))
                                .filter(l -> l.getAction() == AuditAction.READ_LIST)
                                .count())
                .isEqualTo(1);
    }

    @Test
    void 잘못된_액세스_토큰으로_내_정보_조회하면_유효하지_않은_토큰으로_거절된다() throws Exception {
        mockMvc.perform(get("/api/v1/members/me").header("Authorization", "Bearer not-a-valid-jwt"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("AUTH_INVALID_TOKEN"));
    }

    @Test
    void 리프레시_토큰으로_재발급하면_새_액세스와_새_리프레시_토큰을_반환한다() throws Exception {
        String email = "refresh-ok-" + System.nanoTime() + "@test.com";
        String password = "Aa1!aaaa";
        String phone = String.format("010%08d", Math.abs(System.nanoTime()) % 100_000_000);

        RegisterApiTestSupport.registerMember(mockMvc, objectMapper, email, "리프레시", password, phone);
        Tokens tokens = login(email, password);

        String refreshResponse =
                mockMvc.perform(
                                put("/api/v1/auth/token")
                                        .contentType(MediaType.APPLICATION_JSON)
                                        .content(
                                                objectMapper.writeValueAsString(Map.of("refreshToken", tokens.refreshToken()))))
                        .andExpect(status().isOk())
                        .andExpect(jsonPath("$.data.accessToken").exists())
                        .andExpect(jsonPath("$.data.refreshToken").exists())
                        .andReturn()
                        .getResponse()
                        .getContentAsString();

        String newRefresh =
                objectMapper.readTree(refreshResponse).path("data").path("refreshToken").asText();
        assertThat(newRefresh).isNotBlank().isNotEqualTo(tokens.refreshToken());
    }

    @Test
    void 재발급에_사용한_기존_리프레시_토큰을_다시_사용하면_유효하지_않은_토큰으로_거절된다() throws Exception {
        String email = "refresh-reuse-" + System.nanoTime() + "@test.com";
        String password = "Aa1!aaaa";
        String phone = uniquePhone();

        RegisterApiTestSupport.registerMember(mockMvc, objectMapper, email, "리프레시재사용", password, phone);
        Tokens tokens = login(email, password);

        mockMvc.perform(
                        put("/api/v1/auth/token")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(Map.of("refreshToken", tokens.refreshToken()))))
                .andExpect(status().isOk());

        mockMvc.perform(
                        put("/api/v1/auth/token")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(Map.of("refreshToken", tokens.refreshToken()))))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("AUTH_INVALID_TOKEN"));
    }

    @Test
    void 관리자_회원_상세_조회는_마스킹된_정보를_반환하고_감사_로그를_기록한다() throws Exception {
        long before = adminAuditLogRepository.count();
        Member admin = createMember("detail-admin-" + System.nanoTime() + "@test.com", "상세관리자", uniquePhone(), MemberRole.ADMIN);

        String targetEmail = "target-" + System.nanoTime() + "@test.com";
        String targetPhone = uniquePhone();
        Member target = createMember(targetEmail, "대상자", targetPhone, MemberRole.USER);

        mockMvc.perform(get("/api/v1/admin/members/{memberId}", target.getId())
                        .header("Authorization", "Bearer " + jwtTokenProvider.createAccessToken(admin)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.id").value(target.getId()))
                .andExpect(jsonPath("$.data.email").value(maskEmail(targetEmail)))
                .andExpect(jsonPath("$.data.name").value("대**"))
                .andExpect(jsonPath("$.data.phone").value(maskPhone(targetPhone)));

        assertThat(adminAuditLogRepository.count()).isEqualTo(before + 1);
        assertThat(
                        adminAuditLogRepository.findAll().stream()
                                .filter(l -> l.getAdminId().equals(admin.getId()))
                                .filter(l -> target.getId().equals(l.getTargetMemberId()))
                                .filter(l -> l.getAction() == AuditAction.READ_DETAIL)
                                .count())
                .isEqualTo(1);
    }

    @Test
    void 관리자_PII_조회는_원문을_반환하고_감사_로그를_기록한다() throws Exception {
        long before = adminAuditLogRepository.count();
        Member admin = createMember("pii-admin-" + System.nanoTime() + "@test.com", "피아이관리자", uniquePhone(), MemberRole.ADMIN);

        String targetEmail = "pii-target-" + System.nanoTime() + "@test.com";
        String targetPhone = uniquePhone();
        Member target = createMember(targetEmail, "원문대상", targetPhone, MemberRole.USER);

        mockMvc.perform(get("/api/v1/admin/members/{memberId}/pii", target.getId())
                        .header("Authorization", "Bearer " + jwtTokenProvider.createAccessToken(admin)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.id").value(target.getId()))
                .andExpect(jsonPath("$.data.email").value(targetEmail))
                .andExpect(jsonPath("$.data.name").value("원문대상"))
                .andExpect(jsonPath("$.data.phone").value(targetPhone));

        assertThat(adminAuditLogRepository.count()).isEqualTo(before + 1);
        assertThat(
                        adminAuditLogRepository.findAll().stream()
                                .filter(l -> l.getAdminId().equals(admin.getId()))
                                .filter(l -> target.getId().equals(l.getTargetMemberId()))
                                .filter(l -> l.getAction() == AuditAction.READ_DETAIL_PII)
                                .count())
                .isEqualTo(1);
    }

    @Test
    void 일반_권한으로_관리자_회원_상세와_PII_조회는_권한_없음으로_거절된다() throws Exception {
        String userEmail = "detail-user-" + System.nanoTime() + "@test.com";
        String userPhone = uniquePhone();
        Member user = createMember(userEmail, "일반사용자", userPhone, MemberRole.USER);
        Member target = createMember("detail-target-" + System.nanoTime() + "@test.com", "대상회원", uniquePhone(), MemberRole.USER);

        String accessToken = jwtTokenProvider.createAccessToken(user);

        mockMvc.perform(get("/api/v1/admin/members/{memberId}", target.getId())
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("AUTH_FORBIDDEN"));

        mockMvc.perform(get("/api/v1/admin/members/{memberId}/pii", target.getId())
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("AUTH_FORBIDDEN"));
    }

    @Test
    void 내_정보_수정은_정상_입력으로_성공한다() throws Exception {
        String email = "update-ok-" + System.nanoTime() + "@test.com";
        String password = "Aa1!aaaa";
        String phone = uniquePhone();

        RegisterApiTestSupport.registerMember(mockMvc, objectMapper, email, "수정전", password, phone);
        Tokens tokens = login(email, password);

        mockMvc.perform(
                        patch("/api/v1/members/me")
                                .header("Authorization", "Bearer " + tokens.accessToken())
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(Map.of("phone", "010-9876-5432", "name", "수정후"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.email").value(email))
                .andExpect(jsonPath("$.data.name").value("수정후"))
                .andExpect(jsonPath("$.data.phone").value("01098765432"));
    }

    @Test
    void 내_정보_수정_시_잘못된_입력이면_INVALID_INPUT을_반환한다() throws Exception {
        String email = "update-invalid-" + System.nanoTime() + "@test.com";
        String password = "Aa1!aaaa";
        String phone = uniquePhone();

        RegisterApiTestSupport.registerMember(mockMvc, objectMapper, email, "검증대상", password, phone);
        Tokens tokens = login(email, password);

        mockMvc.perform(
                        patch("/api/v1/members/me")
                                .header("Authorization", "Bearer " + tokens.accessToken())
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(Map.of("phone", "123", "name", "검증실패"))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_INPUT"))
                .andExpect(jsonPath("$.data.phone").exists());
    }

    @Test
    void 내_정보_수정_시_중복_휴대폰_번호면_충돌_오류를_반환한다() throws Exception {
        String duplicatePhone = uniquePhone();
        RegisterApiTestSupport.registerMember(
                mockMvc,
                objectMapper,
                "dup-owner-" + System.nanoTime() + "@test.com",
                "기존회원",
                "Aa1!aaaa",
                duplicatePhone);

        String email = "update-dup-" + System.nanoTime() + "@test.com";
        String password = "Aa1!aaaa";
        RegisterApiTestSupport.registerMember(mockMvc, objectMapper, email, "수정회원", password, uniquePhone());
        Tokens tokens = login(email, password);

        mockMvc.perform(
                        patch("/api/v1/members/me")
                                .header("Authorization", "Bearer " + tokens.accessToken())
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(Map.of("phone", duplicatePhone, "name", "충돌"))))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("MEMBER_DUPLICATE_PHONE"));
    }

    @Test
    void 비밀번호를_연속으로_틀리면_다섯_번까지는_자격_증명_오류이고_이후에는_계정_잠금이다() throws Exception {
        String email = "lockout-" + System.nanoTime() + "@test.com";
        String password = "Aa1!aaaa";
        String phone = String.format("010%08d", Math.abs(System.nanoTime()) % 100_000_000);

        RegisterApiTestSupport.registerMember(mockMvc, objectMapper, email, "잠금테스트", password, phone);

        for (int i = 0; i < 5; i++) {
            mockMvc.perform(
                            post("/api/v1/auth/token")
                                    .contentType(MediaType.APPLICATION_JSON)
                                    .content(
                                            objectMapper.writeValueAsString(
                                                    java.util.Map.of("email", email, "password", "wrong-pass!"))))
                    .andExpect(status().isUnauthorized())
                    .andExpect(jsonPath("$.code").value("AUTH_INVALID_CREDENTIALS"));
        }

        mockMvc.perform(
                        post("/api/v1/auth/token")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(
                                        objectMapper.writeValueAsString(
                                                java.util.Map.of("email", email, "password", "wrong-pass!"))))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("AUTH_ACCOUNT_LOCKED"));

        mockMvc.perform(
                        post("/api/v1/auth/token")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(
                                        objectMapper.writeValueAsString(
                                                java.util.Map.of("email", email, "password", password))))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("AUTH_ACCOUNT_LOCKED"));
    }

    private Tokens login(String email, String password) throws Exception {
        String loginBody =
                mockMvc.perform(
                                post("/api/v1/auth/token")
                                        .contentType(MediaType.APPLICATION_JSON)
                                        .content(objectMapper.writeValueAsString(Map.of("email", email, "password", password))))
                        .andExpect(status().isOk())
                        .andReturn()
                        .getResponse()
                        .getContentAsString();

        return new Tokens(
                objectMapper.readTree(loginBody).path("data").path("accessToken").asText(),
                objectMapper.readTree(loginBody).path("data").path("refreshToken").asText());
    }

    private Member createMember(String email, String name, String phone, MemberRole role) {
        return memberRepository.save(
                Member.create(
                        email,
                        name,
                        phone,
                        phoneHasher.hash(phone),
                        "HASH",
                        role,
                        MemberStatus.ACTIVE));
    }

    private String uniquePhone() {
        return String.format("010%08d", Math.abs(System.nanoTime()) % 100_000_000);
    }

    private String maskEmail(String email) {
        int at = email.indexOf('@');
        return email.charAt(0) + "***" + email.substring(at);
    }

    private String maskPhone(String phone) {
        return phone.substring(0, 3) + "****" + phone.substring(phone.length() - 4);
    }
}
