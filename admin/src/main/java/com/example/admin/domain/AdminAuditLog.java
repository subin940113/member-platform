package com.example.admin.domain;

import com.example.common.domain.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "admin_audit_logs")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class AdminAuditLog extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "admin_id", nullable = false)
    private Long adminId;

    @Column(name = "target_member_id")
    private Long targetMemberId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50)
    private AuditAction action;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private AuditResult result;

    @Column(name = "ip_address", nullable = false, length = 45)
    private String ipAddress;

    @Column(name = "request_uri", nullable = false)
    private String requestUri;

    private AdminAuditLog(
            Long adminId,
            Long targetMemberId,
            AuditAction action,
            AuditResult result,
            String ipAddress,
            String requestUri) {
        this.adminId = adminId;
        this.targetMemberId = targetMemberId;
        this.action = action;
        this.result = result;
        this.ipAddress = ipAddress;
        this.requestUri = requestUri;
    }

    public static AdminAuditLog create(
            Long adminId,
            Long targetMemberId,
            AuditAction action,
            AuditResult result,
            String ipAddress,
            String requestUri) {
        return new AdminAuditLog(adminId, targetMemberId, action, result, ipAddress, requestUri);
    }
}
