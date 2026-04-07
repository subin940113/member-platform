package com.example.member.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.example.common.exception.BusinessException;
import com.example.common.exception.ErrorCode;
import com.example.member.infrastructure.register.RegisterAdmissionStore;
import com.example.member.presentation.dto.response.AdmissionTokenResponse;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class IssueRegisterAdmissionUseCaseTest {

    @Mock
    private RegisterAdmissionStore admissionStore;

    @InjectMocks
    private IssueRegisterAdmissionUseCase issueRegisterAdmissionUseCase;

    @Test
    void 발급에_성공하면_저장소_발급_결과를_그대로_반환한다() {
        AdmissionTokenResponse expected = new AdmissionTokenResponse("token-uuid", 60L);
        when(admissionStore.issue()).thenReturn(expected);

        AdmissionTokenResponse result = issueRegisterAdmissionUseCase.issue();

        assertThat(result).isSameAs(expected);
        verify(admissionStore).issue();
    }

    @Test
    void 저장소가_요청_과다_오류를_던지면_그대로_전파된다() {
        when(admissionStore.issue()).thenThrow(new BusinessException(ErrorCode.TOO_MANY_REQUESTS));

        assertThatThrownBy(() -> issueRegisterAdmissionUseCase.issue())
                .isInstanceOf(BusinessException.class)
                .satisfies(ex -> assertThat(((BusinessException) ex).getErrorCode()).isEqualTo(ErrorCode.TOO_MANY_REQUESTS));

        verify(admissionStore).issue();
    }
}
