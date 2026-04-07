package com.example.admin.domain;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class AdminAuditLogTest {

    @Test
    void 생성된_감사_로그는_전달한_값이_필드에_반영된다() {
        AdminAuditLog log =
                AdminAuditLog.create(
                        10L,
                        20L,
                        AuditAction.READ_DETAIL,
                        AuditResult.SUCCESS,
                        "127.0.0.1",
                        "/api/v1/admin/members/20");

        assertThat(log.getAdminId()).isEqualTo(10L);
        assertThat(log.getTargetMemberId()).isEqualTo(20L);
        assertThat(log.getAction()).isEqualTo(AuditAction.READ_DETAIL);
        assertThat(log.getResult()).isEqualTo(AuditResult.SUCCESS);
        assertThat(log.getIpAddress()).isEqualTo("127.0.0.1");
        assertThat(log.getRequestUri()).isEqualTo("/api/v1/admin/members/20");
    }

    @Test
    void 목록_조회처럼_대상_회원이_없으면_대상_회원_식별자는_비어_있다() {
        AdminAuditLog log =
                AdminAuditLog.create(
                        1L,
                        null,
                        AuditAction.READ_LIST,
                        AuditResult.SUCCESS,
                        "0:0:0:0:0:0:0:1",
                        "/api/v1/admin/members");

        assertThat(log.getTargetMemberId()).isNull();
        assertThat(log.getAction()).isEqualTo(AuditAction.READ_LIST);
    }
}
