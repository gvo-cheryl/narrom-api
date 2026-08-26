package com.naroom.api.admin.ai.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;

public record AdminAiPromptUpdateRequest(
		@NotBlank String versionLabel,
		@NotBlank String content,
		String modelName,
		@Positive Integer outputMaxLength) {
}
