package com.example.member.presentation.dto.request;

import com.example.member.presentation.validation.KoreanMobilePhone;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record UpdateMemberRequest(
        @NotBlank @KoreanMobilePhone String phone,
        @Pattern(regexp = "^(?!\\s*$).{1,100}$") String name) {}
