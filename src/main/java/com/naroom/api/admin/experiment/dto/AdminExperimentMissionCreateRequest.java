package com.naroom.api.admin.experiment.dto;

import com.naroom.api.experiment.domain.entity.ExperimentEmotionalLoad;
import com.naroom.api.experiment.domain.entity.ExperimentMissionType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.util.UUID;

public record AdminExperimentMissionCreateRequest(
		@NotBlank String code,
		@NotNull UUID topicId,
		@NotBlank String title,
		@NotBlank String description,
		@NotBlank String instruction,
		@NotNull ExperimentMissionType missionType,
		@NotBlank String responseType,
		@Positive short estimatedMinutes,
		@NotNull ExperimentEmotionalLoad emotionalLoad,
		@NotBlank String reflectionQuestions,
		@NotBlank String examples,
		@NotBlank String responseSchema,
		String safetyNote,
		@NotNull Boolean active) {
}
