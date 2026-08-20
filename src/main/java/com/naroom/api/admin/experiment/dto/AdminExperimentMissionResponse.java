package com.naroom.api.admin.experiment.dto;

import com.naroom.api.experiment.domain.entity.ExperimentEmotionalLoad;
import com.naroom.api.experiment.domain.entity.ExperimentMission;
import com.naroom.api.experiment.domain.entity.ExperimentMissionType;

import java.time.Instant;
import java.util.UUID;

public record AdminExperimentMissionResponse(
		UUID id,
		String code,
		int contentVersion,
		UUID topicId,
		String topicCode,
		String title,
		String description,
		String instruction,
		ExperimentMissionType missionType,
		String responseType,
		short estimatedMinutes,
		ExperimentEmotionalLoad emotionalLoad,
		String reflectionQuestions,
		String examples,
		String responseSchema,
		String safetyNote,
		boolean active,
		Instant createdAt,
		Instant updatedAt) {

	public static AdminExperimentMissionResponse from(ExperimentMission mission) {
		return new AdminExperimentMissionResponse(
				mission.getId(),
				mission.getCode(),
				mission.getContentVersion(),
				mission.getTopic().getId(),
				mission.getTopic().getCode(),
				mission.getTitle(),
				mission.getDescription(),
				mission.getInstruction(),
				mission.getMissionType(),
				mission.getResponseType(),
				mission.getEstimatedMinutes(),
				mission.getEmotionalLoad(),
				mission.getReflectionQuestions(),
				mission.getExamples(),
				mission.getResponseSchema(),
				mission.getSafetyNote(),
				mission.isActive(),
				mission.getCreatedAt(),
				mission.getUpdatedAt());
	}

}
