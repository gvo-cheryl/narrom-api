package com.naroom.api.lifetime.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

// §4 입력 글자 수 제한 "생각 덧붙이기·사용자 자기정리" 행(1,000자)을 그대로 적용한다.
public record PersonalSummaryUpdateRequest(@NotBlank @Size(max = 1000) String content) {
}
