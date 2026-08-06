package com.naroom.api.experiment.dto;

import com.naroom.api.experiment.domain.entity.ExperimentRecommendation;
import com.naroom.api.experiment.domain.entity.ExperimentRecommendationSourceType;
import com.naroom.api.experiment.domain.entity.ExperimentRecommendationStatus;

import java.time.Instant;
import java.util.UUID;

public record ExperimentRecommendationResponse(
		UUID recommendationId,
		ExperimentProgramSummaryResponse program,
		ExperimentRecommendationSourceType sourceType,
		String reasonText,
		ExperimentRecommendationStatus status,
		Instant createdAt) {

	public static ExperimentRecommendationResponse of(ExperimentRecommendation recommendation) {
		return new ExperimentRecommendationResponse(
				recommendation.getId(),
				ExperimentProgramSummaryResponse.of(recommendation.getProgram()),
				recommendation.getSourceType(),
				recommendation.getReasonText(),
				recommendation.getStatus(),
				recommendation.getCreatedAt());
	}

}
