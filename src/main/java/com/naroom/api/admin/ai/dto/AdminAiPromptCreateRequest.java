package com.naroom.api.admin.ai.dto;

import com.naroom.api.ai.domain.entity.AiFeatureType;
import com.naroom.api.ai.domain.entity.AiPromptScope;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

// featureType은 scope=FEATURE일 때만 필요하다(엔티티 CHECK 제약과 대응). modelName/outputMaxLength는
// FEATURE 범위에서만 의미 있고 COMMON 범위에서는 무시한다.
public record AdminAiPromptCreateRequest(
		@NotNull AiPromptScope scope,
		AiFeatureType featureType,
		@NotBlank String versionLabel,
		@NotBlank String content,
		String modelName,
		@Positive Integer outputMaxLength) {
}
