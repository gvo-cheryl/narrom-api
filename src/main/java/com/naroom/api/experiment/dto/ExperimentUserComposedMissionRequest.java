package com.naroom.api.experiment.dto;

import com.naroom.api.experiment.domain.entity.ExperimentMissionType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record ExperimentUserComposedMissionRequest(
		short dayNumber,
		@NotBlank String title,
		@NotBlank String instruction,
		@NotNull ExperimentMissionType missionType,
		@Positive short estimatedMinutes) {
}
