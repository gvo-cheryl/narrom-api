package com.naroom.api.admin.ai.dto;

import com.naroom.api.ai.domain.entity.AiFeatureType;
import com.naroom.api.ai.domain.entity.AiPromptScope;
import com.naroom.api.ai.domain.entity.AiPromptVersion;
import com.naroom.api.ai.domain.entity.AiPromptVersionStatus;

import java.time.Instant;
import java.util.UUID;

public record AdminAiPromptResponse(
		UUID id,
		AiPromptScope scope,
		AiFeatureType featureType,
		String versionLabel,
		String content,
		String modelName,
		Integer outputMaxLength,
		AiPromptVersionStatus status,
		UUID supersedesVersionId,
		UUID createdByAdminId,
		Instant createdAt) {

	public static AdminAiPromptResponse from(AiPromptVersion version) {
		return new AdminAiPromptResponse(
				version.getId(),
				version.getScope(),
				version.getFeatureType(),
				version.getVersionLabel(),
				version.getContent(),
				version.getModelName(),
				version.getOutputMaxLength(),
				version.getStatus(),
				version.getSupersedesVersionId(),
				version.getCreatedByAdminId(),
				version.getCreatedAt());
	}

}
