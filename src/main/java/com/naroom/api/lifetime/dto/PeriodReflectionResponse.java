package com.naroom.api.lifetime.dto;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.naroom.api.ai.domain.entity.AiFeatureType;
import com.naroom.api.ai.domain.entity.AiJobStatus;
import com.naroom.api.lifetime.domain.entity.PeriodReflection;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

// safetyCode/errorCode 등 내부 실패·안전 판정 원인은 노출하지 않는다(21.3절과 동일한 원칙 - EntryAiReflectionResponse
// 참고). insights는 완료된 경우에만 값이 있다.
public record PeriodReflectionResponse(
		UUID id,
		AiFeatureType featureType,
		LocalDate periodStart,
		LocalDate periodEnd,
		int versionNo,
		AiJobStatus status,
		String summaryText,
		PeriodReflectionInsights insights,
		String questionText,
		Instant requestedAt,
		Instant completedAt) {

	private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

	public static PeriodReflectionResponse from(PeriodReflection reflection) {
		return new PeriodReflectionResponse(
				reflection.getId(),
				reflection.getFeatureType(),
				reflection.getPeriodStart(),
				reflection.getPeriodEnd(),
				reflection.getVersionNo(),
				reflection.getStatus(),
				reflection.getSummaryText(),
				parseInsights(reflection.getInsights()),
				reflection.getQuestionText(),
				reflection.getRequestedAt(),
				reflection.getCompletedAt());
	}

	private static PeriodReflectionInsights parseInsights(String insightsJson) {
		if (insightsJson == null) {
			return null;
		}
		try {
			return OBJECT_MAPPER.readValue(insightsJson, PeriodReflectionInsights.class);
		} catch (JsonProcessingException e) {
			return null;
		}
	}

}
