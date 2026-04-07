package com.example.member.presentation;

import com.example.common.annotation.MemberId;
import com.example.common.response.ApiResponse;
import com.example.member.application.GetMemberUseCase;
import com.example.member.application.IssueRegisterAdmissionUseCase;
import com.example.member.application.RegisterMemberUseCase;
import com.example.member.application.UpdateMemberUseCase;
import com.example.member.application.WithdrawMemberUseCase;
import com.example.member.domain.Member;
import com.example.member.presentation.dto.request.RegisterMemberRequest;
import com.example.member.presentation.dto.request.UpdateMemberRequest;
import com.example.member.presentation.dto.response.AdmissionTokenResponse;
import com.example.member.presentation.dto.response.MemberResponse;
import jakarta.validation.Valid;
import java.net.URI;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/members")
@RequiredArgsConstructor
public class MemberController {

    private final IssueRegisterAdmissionUseCase issueRegisterAdmissionUseCase;
    private final RegisterMemberUseCase registerMemberUseCase;
    private final GetMemberUseCase getMemberUseCase;
    private final UpdateMemberUseCase updateMemberUseCase;
    private final WithdrawMemberUseCase withdrawMemberUseCase;

    @PostMapping("/admission")
    public ApiResponse<AdmissionTokenResponse> issueAdmission() {
        return ApiResponse.ok(issueRegisterAdmissionUseCase.issue());
    }

    @PostMapping
    public ResponseEntity<Void> register(
            @RequestHeader(value = "Admission-Token", required = false) String admissionToken,
            @RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey,
            @Valid @RequestBody RegisterMemberRequest request) {
        Member member = registerMemberUseCase.register(admissionToken, idempotencyKey, request);
        URI location = URI.create("/api/v1/members/" + member.getId());
        return ResponseEntity.created(location).build();
    }

    @GetMapping("/me")
    public ApiResponse<MemberResponse> getMe(@MemberId Long memberId) {
        Member member = getMemberUseCase.getActiveById(memberId);
        return ApiResponse.ok(MemberResponse.from(member));
    }

    @PatchMapping("/me")
    public ApiResponse<MemberResponse> updateMe(
            @MemberId Long memberId, @Valid @RequestBody UpdateMemberRequest request) {
        Member member = updateMemberUseCase.update(memberId, request.phone(), request.name());
        return ApiResponse.ok(MemberResponse.from(member));
    }

    @DeleteMapping("/me")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void withdrawMe(@MemberId Long memberId) {
        withdrawMemberUseCase.withdraw(memberId);
    }
}
