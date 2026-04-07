package com.example.admin.presentation;

import com.example.admin.application.GetMemberDetailUseCase;
import com.example.admin.application.GetMemberListUseCase;
import com.example.admin.domain.AuditAction;
import com.example.admin.presentation.aop.AdminAudit;
import com.example.admin.presentation.dto.response.MemberPiiResponse;
import com.example.admin.presentation.dto.response.MemberResponse;
import com.example.common.response.ApiResponse;
import com.example.member.domain.Member;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/admin/members")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class AdminController {

    private final GetMemberListUseCase getMemberListUseCase;
    private final GetMemberDetailUseCase getMemberDetailUseCase;

    @AdminAudit(action = AuditAction.READ_LIST)
    @GetMapping
    public ApiResponse<Page<MemberResponse>> getMemberList(
            @PageableDefault(size = 20) Pageable pageable) {
        Page<Member> page = getMemberListUseCase.getList(pageable);
        return ApiResponse.ok(page.map(MemberResponse::from));
    }

    @AdminAudit(action = AuditAction.READ_DETAIL, targetIdParam = "memberId")
    @GetMapping("/{memberId}")
    public ApiResponse<MemberResponse> getMember(@PathVariable Long memberId) {
        Member member = getMemberDetailUseCase.getById(memberId);
        return ApiResponse.ok(MemberResponse.from(member));
    }

    @AdminAudit(action = AuditAction.READ_DETAIL_PII, targetIdParam = "memberId")
    @GetMapping("/{memberId}/pii")
    public ApiResponse<MemberPiiResponse> getMemberPii(@PathVariable Long memberId) {
        Member member = getMemberDetailUseCase.getById(memberId);
        return ApiResponse.ok(MemberPiiResponse.from(member));
    }
}
