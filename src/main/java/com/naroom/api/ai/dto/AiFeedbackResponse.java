package com.naroom.api.ai.dto;

import com.naroom.api.ai.domain.entity.AiFeedback;
import com.naroom.api.ai.domain.entity.AiFeedbackHelpfulness;

import java.time.Instant;
import java.util.UUID;

public record AiFeedbackResponse(
		UUID id,
		UUID generationRunId,
		AiFeedbackHelpfulness helpfulness,
		String reasonCode,
		String customReason,
		Boolean applyLongTerm,
		Instant createdAt,
		Instant updatedAt) {

	public static AiFeedbackResponse from(AiFeedback feedback) {
		return new AiFeedbackResponse(
				feedback.getId(),
				feedback.getGenerationRun().getId(),
				feedback.getHelpfulness(),
				feedback.getReasonCode(),
				feedback.getCustomReason(),
				feedback.getApplyLongTerm(),
				feedback.getCreatedAt(),
				feedback.getUpdatedAt());
	}

}
