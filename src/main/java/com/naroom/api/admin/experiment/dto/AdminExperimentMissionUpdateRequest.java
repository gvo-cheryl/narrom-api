package com.naroom.api.admin.experiment.dto;

import com.naroom.api.experiment.domain.entity.ExperimentEmotionalLoad;
import com.naroom.api.experiment.domain.entity.ExperimentMissionType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.util.UUID;

// code는 다른 테이블(experiment_program_missions 등)이 참조하는 안정 식별자라 수정 대상에서 제외한다.
public record AdminExperimentMissionUpdateRequest(
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
