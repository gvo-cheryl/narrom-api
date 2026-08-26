package com.naroom.api.admin.ai.dto;

import com.naroom.api.ai.domain.entity.AiFeatureType;

public record AdminAiRuntimeStatusResponse(
		AiFeatureType featureType,
		String modelName,
		String commonPromptVersionLabel,
		String commonPromptOutputSchemaVersion,
		String featurePromptVersionLabel,
		String featurePromptOutputSchemaVersion,
		Integer outputMaxLength,
		int windowDays,
		long totalJobCount,
		long completedJobCount,
		Double successRate,
		Double avgLatencyMs,
		Double avgInputTokens,
		Double avgOutputTokens) {
}
