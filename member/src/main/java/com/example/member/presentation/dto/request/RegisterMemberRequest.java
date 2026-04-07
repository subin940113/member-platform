package com.example.member.presentation.dto.request;

import com.example.member.presentation.validation.KoreanMobilePhone;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record RegisterMemberRequest(
        @NotBlank @Email @Size(max = 255) String email,
        @NotBlank @Size(max = 100) String name,
        @NotBlank @Size(min = 8, max = 72)
        @Pattern(
                regexp = "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[@$!%*?&])[A-Za-z\\d@$!%*?&]+$",
                message = "비밀번호는 영문 대소문자, 숫자, 특수문자(@$!%*?&)를 각각 하나 이상 포함해야 합니다")
        String password,
        @NotBlank @KoreanMobilePhone String phone) {}
