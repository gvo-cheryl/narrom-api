package com.naroom.api.ai.domain.repository;

import com.naroom.api.ai.domain.entity.AiFeatureType;

// AiJobRepository.aggregateStatusCountsSince의 JPQL 프로젝션 결과.
public interface AiJobStatusAggregate {

	AiFeatureType getFeatureType();

	long getTotalCount();

	long getCompletedCount();

}
