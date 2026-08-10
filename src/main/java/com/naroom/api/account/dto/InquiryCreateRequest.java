package com.naroom.api.account.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record InquiryCreateRequest(@NotBlank @Size(max = 2000) String content) {
}
