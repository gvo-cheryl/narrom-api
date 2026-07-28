package com.naroom.api.ai.dto;

import com.naroom.api.ai.domain.entity.AiFeedbackReport;

import java.time.Instant;
import java.util.UUID;

public record AiFeedbackReportResponse(
		UUID id,
		UUID generationRunId,
		String reasonCode,
		String comment,
		Instant createdAt) {

	public static AiFeedbackReportResponse from(AiFeedbackReport report) {
		return new AiFeedbackReportResponse(
				report.getId(),
				report.getGenerationRun().getId(),
				report.getReasonCode(),
				report.getComment(),
				report.getCreatedAt());
	}

}
