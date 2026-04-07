package com.example.member.infrastructure.register;

import com.example.common.exception.BusinessException;
import com.example.common.exception.ErrorCode;
import com.example.member.presentation.dto.response.AdmissionTokenResponse;
import java.time.Duration;
import java.util.Optional;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataAccessException;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Slf4j
@Component
@RequiredArgsConstructor
public class RegisterAdmissionStore {

    private final StringRedisTemplate stringRedisTemplate;
    private final RegisterProperties registerProperties;

    public AdmissionTokenResponse issue() {
        Duration ttl = registerProperties.admission().tokenTtl();
        int maxSlots = registerProperties.admission().maxConcurrent();
        for (int slotNumber = 0; slotNumber < maxSlots; slotNumber++) {
            Optional<AdmissionTokenResponse> issued = tryIssueAtSlot(slotNumber, ttl);
            if (issued.isPresent()) {
                return issued.get();
            }
        }
        throw new BusinessException(ErrorCode.TOO_MANY_REQUESTS);
    }

    private Optional<AdmissionTokenResponse> tryIssueAtSlot(int slotNumber, Duration ttl) {
        String admissionToken = UUID.randomUUID().toString();
        String slotKey = RegisterRedisKeySpec.admissionSlotKey(slotNumber);
        Boolean slotClaimed = stringRedisTemplate.opsForValue().setIfAbsent(slotKey, admissionToken, ttl);
        if (!Boolean.TRUE.equals(slotClaimed)) {
            return Optional.empty();
        }
        try {
            String tokenKey = RegisterRedisKeySpec.admissionTokenKey(admissionToken);
            Boolean tokenStored =
                    stringRedisTemplate
                            .opsForValue()
                            .setIfAbsent(tokenKey, Integer.toString(slotNumber), ttl);
            if (Boolean.TRUE.equals(tokenStored)) {
                return Optional.of(new AdmissionTokenResponse(admissionToken, ttl.toSeconds()));
            }
            stringRedisTemplate.delete(slotKey);
            return Optional.empty();
        } catch (DataAccessException ex) {
            try {
                stringRedisTemplate.delete(slotKey);
            } catch (DataAccessException rollbackEx) {
                log.warn(
                        "[member-register] admission rollback failed. slotNumber={}",
                        slotNumber,
                        rollbackEx);
            }
            throw ex;
        }
    }

    public void validate(String admissionToken) {
        if (!StringUtils.hasText(admissionToken)) {
            throw new BusinessException(ErrorCode.MEMBER_ADMISSION_INVALID);
        }
        String trimmedAdmissionToken = admissionToken.strip();
        String tokenKey = RegisterRedisKeySpec.admissionTokenKey(trimmedAdmissionToken);
        String slotNumberValue = stringRedisTemplate.opsForValue().get(tokenKey);
        if (slotNumberValue == null) {
            throw new BusinessException(ErrorCode.MEMBER_ADMISSION_INVALID);
        }
        int slotNumber;
        try {
            slotNumber = Integer.parseInt(slotNumberValue);
        } catch (NumberFormatException ex) {
            throw new BusinessException(ErrorCode.MEMBER_ADMISSION_INVALID);
        }
        String slotKey = RegisterRedisKeySpec.admissionSlotKey(slotNumber);
        String slotAdmissionToken = stringRedisTemplate.opsForValue().get(slotKey);
        if (!trimmedAdmissionToken.equals(slotAdmissionToken)) {
            throw new BusinessException(ErrorCode.MEMBER_ADMISSION_INVALID);
        }
    }

    public void release(String admissionToken) {
        if (!StringUtils.hasText(admissionToken)) {
            return;
        }
        String trimmedAdmissionToken = admissionToken.strip();
        try {
            String tokenKey = RegisterRedisKeySpec.admissionTokenKey(trimmedAdmissionToken);
            String slotNumberValue = stringRedisTemplate.opsForValue().get(tokenKey);
            if (slotNumberValue == null) {
                return;
            }
            int slotNumber = Integer.parseInt(slotNumberValue);
            String slotKey = RegisterRedisKeySpec.admissionSlotKey(slotNumber);
            String slotAdmissionToken = stringRedisTemplate.opsForValue().get(slotKey);
            if (trimmedAdmissionToken.equals(slotAdmissionToken)) {
                stringRedisTemplate.delete(slotKey);
            }
            stringRedisTemplate.delete(tokenKey);
        } catch (DataAccessException | NumberFormatException ex) {
            log.error("[member-register] admission release failed. admissionToken={}", trimmedAdmissionToken, ex);
            throw new IllegalStateException("회원가입 입장 해제에 실패했습니다", ex);
        }
    }
}
