package com.naroom.api.admin.experiment.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.util.UUID;

public record AdminExperimentProgramDayMissionRequest(
		@Positive short dayNumber,
		@NotNull UUID missionId,
		@NotNull Boolean replaceable,
		String replacementGroup) {
}
