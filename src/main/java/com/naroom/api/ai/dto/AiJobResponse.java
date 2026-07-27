package com.naroom.api.ai.dto;

import com.naroom.api.ai.domain.entity.AiFeatureType;
import com.naroom.api.ai.domain.entity.AiJob;
import com.naroom.api.ai.domain.entity.AiJobStatus;

import java.time.Instant;
import java.util.UUID;

public record AiJobResponse(
		UUID id,
		UUID memberId,
		AiFeatureType featureType,
		UUID entryId,
		UUID conversationId,
		AiJobStatus status,
		int attemptCount,
		int maxAttempts,
		Instant nextRetryAt,
		Instant startedAt,
		Instant completedAt,
		String errorCode,
		Instant createdAt) {

	public static AiJobResponse from(AiJob job) {
		return new AiJobResponse(
				job.getId(),
				job.getMember().getId(),
				job.getFeatureType(),
				job.getEntry() == null ? null : job.getEntry().getId(),
				job.getConversation() == null ? null : job.getConversation().getId(),
				job.getStatus(),
				job.getAttemptCount(),
				job.getMaxAttempts(),
				job.getNextRetryAt(),
				job.getStartedAt(),
				job.getCompletedAt(),
				job.getErrorCode(),
				job.getCreatedAt());
	}

}
