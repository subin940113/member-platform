package com.example.member.application;

import com.example.member.infrastructure.register.RegisterAdmissionStore;
import com.example.member.presentation.dto.response.AdmissionTokenResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class IssueRegisterAdmissionUseCase {

    private final RegisterAdmissionStore admissionStore;

    public AdmissionTokenResponse issue() {
        return admissionStore.issue();
    }
}
