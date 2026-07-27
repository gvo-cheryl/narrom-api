package com.naroom.api.record.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.util.UUID;

// aiReflectionId는 선택 값이다: 있으면 특정 AI 정리(reflectionQuestion)에 대한 답변으로 연결하고,
// 없으면 기존과 동일한 일반 생각 덧붙이기로 저장한다(하위 호환).
public record EntrySelfReflectionRequest(@NotBlank @Size(max = 1000) String content, UUID aiReflectionId) {
}
