package com.naroom.api.experiment.dto;

import com.naroom.api.experiment.domain.entity.ExperimentTopic;

import java.util.UUID;

public record ExperimentTopicResponse(UUID id, String code, String name, String description, int displayOrder) {

	public static ExperimentTopicResponse from(ExperimentTopic topic) {
		return new ExperimentTopicResponse(
				topic.getId(), topic.getCode(), topic.getName(), topic.getDescription(), topic.getDisplayOrder());
	}

}
