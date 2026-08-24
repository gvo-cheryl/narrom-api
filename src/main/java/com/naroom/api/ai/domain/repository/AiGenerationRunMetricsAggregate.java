package com.naroom.api.ai.domain.repository;

import com.naroom.api.ai.domain.entity.AiFeatureType;

// AiGenerationRunRepository.aggregateMetricsSince의 JPQL 프로젝션 결과.
public interface AiGenerationRunMetricsAggregate {

	AiFeatureType getFeatureType();

	Double getAvgLatencyMs();

	Double getAvgInputTokens();

	Double getAvgOutputTokens();

}
