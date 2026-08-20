package com.naroom.api.admin.experiment.dto;

import com.naroom.api.experiment.domain.entity.ExperimentTopic;

import java.time.Instant;
import java.util.UUID;

public record AdminExperimentTopicResponse(
		UUID id,
		String code,
		String name,
		String description,
		int displayOrder,
		boolean active,
		Instant createdAt,
		Instant updatedAt) {

	public static AdminExperimentTopicResponse from(ExperimentTopic topic) {
		return new AdminExperimentTopicResponse(
				topic.getId(),
				topic.getCode(),
				topic.getName(),
				topic.getDescription(),
				topic.getDisplayOrder(),
				topic.isActive(),
				topic.getCreatedAt(),
				topic.getUpdatedAt());
	}

}
