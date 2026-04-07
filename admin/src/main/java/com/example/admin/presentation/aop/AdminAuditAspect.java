package com.example.admin.presentation.aop;

import com.example.admin.domain.AdminAuditLog;
import com.example.admin.domain.AuditAction;
import com.example.admin.domain.AuditResult;
import com.example.admin.infrastructure.persistence.AdminAuditLogRepository;
import com.example.common.Constants;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

@Aspect
@Component
@RequiredArgsConstructor
@Slf4j
public class AdminAuditAspect {

    private final AdminAuditLogRepository adminAuditLogRepository;

    @Around("@annotation(adminAudit)")
    public Object audit(ProceedingJoinPoint joinPoint, AdminAudit adminAudit) throws Throwable {
        AuditResult result = AuditResult.FAILURE;

        try {
            Object response = joinPoint.proceed();
            result = AuditResult.SUCCESS;
            return response;
        } finally {
            saveAuditLog(joinPoint, adminAudit, result);
        }
    }

    private void saveAuditLog(ProceedingJoinPoint joinPoint, AdminAudit adminAudit, AuditResult result) {
        try {
            Long adminId = resolveAdminId(joinPoint);
            String ipAddress = resolveIpAddress();
            String requestUri = resolveRequestUri();
            Long targetMemberId = resolveTargetMemberId(joinPoint, adminAudit);

            adminAuditLogRepository.save(
                    AdminAuditLog.create(adminId, targetMemberId, adminAudit.action(), result, ipAddress, requestUri));

        } catch (RuntimeException e) {
            HttpServletRequest request = safeCurrentRequest();
            log.error(
                    "[member-admin] audit log persistence failed. auditAction={}, auditResult={}, httpMethod={},"
                            + " requestUri={}, targetIdParam={}, joinPointSignature={}",
                    adminAudit.action(),
                    result,
                    request != null ? request.getMethod() : "unknown",
                    request != null ? request.getRequestURI() : "unknown",
                    adminAudit.targetIdParam(),
                    joinPoint.getSignature().toShortString(),
                    e);
        }
    }

    private Long resolveAdminId(ProceedingJoinPoint joinPoint) {
        Object id = currentRequest().getAttribute(Constants.MEMBER_ID);
        if (id instanceof Long adminId) {
            return adminId;
        }
        throw new IllegalStateException(
                "요청 속성에서 관리자 ID를 찾을 수 없습니다: " + joinPoint.getSignature().toShortString());
    }

    private String resolveIpAddress() {
        HttpServletRequest request = currentRequest();
        String forwarded = request.getHeader("X-Forwarded-For");
        if (StringUtils.hasText(forwarded)) {
            return forwarded.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }

    private String resolveRequestUri() {
        return currentRequest().getRequestURI();
    }

    private Long resolveTargetMemberId(ProceedingJoinPoint joinPoint, AdminAudit adminAudit) {
        String paramName = adminAudit.targetIdParam();
        if (!StringUtils.hasText(paramName)) {
            return null;
        }

        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        String[] paramNames = signature.getParameterNames();
        Object[] args = joinPoint.getArgs();

        for (int i = 0; i < paramNames.length; i++) {
            if (paramName.equals(paramNames[i])) {
                Object value = args[i];
                if (value instanceof Long id) {
                    return id;
                }
                throw new IllegalArgumentException(
                        "파라미터 '" + paramName + "'의 타입이 Long이 아닙니다. 실제 타입: "
                                + (value == null ? "null" : value.getClass().getName()));
            }
        }
        throw new IllegalArgumentException("파라미터 '" + paramName + "'을(를) 메서드에서 찾을 수 없습니다.");
    }

    private HttpServletRequest safeCurrentRequest() {
        try {
            return currentRequest();
        } catch (IllegalStateException ex) {
            return null;
        }
    }

    private HttpServletRequest currentRequest() {
        ServletRequestAttributes attributes =
                (ServletRequestAttributes) RequestContextHolder.currentRequestAttributes();
        return attributes.getRequest();
    }
}
