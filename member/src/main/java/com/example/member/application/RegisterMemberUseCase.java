package com.example.member.application;

import com.example.common.exception.BusinessException;
import com.example.common.exception.DataIntegrityViolations;
import com.example.common.exception.ErrorCode;
import com.example.member.domain.Member;
import com.example.member.domain.MemberRole;
import com.example.member.domain.MemberStatus;
import com.example.member.infrastructure.crypto.PhoneHasher;
import com.example.member.infrastructure.persistence.MemberRepository;
import com.example.member.infrastructure.register.RegisterAdmissionStore;
import com.example.member.infrastructure.register.RegisterIdempotencyStore;
import com.example.member.presentation.dto.request.RegisterMemberRequest;
import java.util.Locale;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
@RequiredArgsConstructor
public class RegisterMemberUseCase {

    private final RegisterAdmissionStore admissionStore;
    private final RegisterIdempotencyStore idempotencyStore;
    private final MemberRepository memberRepository;
    private final PasswordEncoder passwordEncoder;
    private final PhoneHasher phoneHasher;

    @Transactional
    public Member register(String admissionToken, String idempotencyKey, RegisterMemberRequest request) {
        if (!StringUtils.hasText(admissionToken)) {
            throw new BusinessException(ErrorCode.MEMBER_ADMISSION_REQUIRED);
        }
        if (!StringUtils.hasText(idempotencyKey)) {
            throw new BusinessException(ErrorCode.MEMBER_IDEMPOTENCY_KEY_REQUIRED);
        }

        String admissionTokenValue = admissionToken.strip();
        String idempotencyKeyValue = idempotencyKey.strip();

        admissionStore.validate(admissionTokenValue);

        if (!idempotencyStore.acquire(idempotencyKeyValue)) {
            admissionStore.release(admissionTokenValue);
            throw new BusinessException(ErrorCode.MEMBER_REQUEST_IN_PROGRESS);
        }

        RuntimeException primaryFailure = null;
        try {
            return createMember(request);
        } catch (RuntimeException ex) {
            primaryFailure = ex;
            throw ex;
        } finally {
            cleanupResources(admissionTokenValue, idempotencyKeyValue, primaryFailure);
        }
    }

    private Member createMember(RegisterMemberRequest request) {
        String normalizedEmail = request.email().toLowerCase(Locale.ROOT);
        String normalizedPhone = phoneHasher.normalize(request.phone());
        String phoneHash = phoneHasher.hash(normalizedPhone);

        Member member =
                Member.create(
                        normalizedEmail,
                        request.name().strip(),
                        normalizedPhone,
                        phoneHash,
                        passwordEncoder.encode(request.password()),
                        MemberRole.USER,
                        MemberStatus.ACTIVE);

        try {
            return memberRepository.save(member);
        } catch (DataIntegrityViolationException ex) {
            throw DataIntegrityViolations.toBusinessException(ex);
        }
    }

    private void cleanupResources(
            String admissionTokenValue, String idempotencyKeyValue, RuntimeException primaryFailure) {
        RuntimeException cleanupFailure = null;

        try {
            idempotencyStore.clear(idempotencyKeyValue);
        } catch (RuntimeException ex) {
            cleanupFailure = ex;
        }

        try {
            admissionStore.release(admissionTokenValue);
        } catch (RuntimeException ex) {
            cleanupFailure = mergeCleanupFailure(cleanupFailure, ex);
        }

        if (cleanupFailure == null) {
            return;
        }
        if (primaryFailure != null) {
            primaryFailure.addSuppressed(cleanupFailure);
            return;
        }
        throw cleanupFailure;
    }

    private RuntimeException mergeCleanupFailure(
            RuntimeException cleanupFailure, RuntimeException nextFailure) {
        if (cleanupFailure == null) {
            return nextFailure;
        }
        if (nextFailure != null) {
            cleanupFailure.addSuppressed(nextFailure);
        }
        return cleanupFailure;
    }
}
