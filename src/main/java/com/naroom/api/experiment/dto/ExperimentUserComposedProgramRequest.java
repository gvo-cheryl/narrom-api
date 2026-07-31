package com.naroom.api.experiment.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;

import java.util.List;

public record ExperimentUserComposedProgramRequest(
		@NotBlank String title,
		short durationDays,
		@NotEmpty @Valid List<ExperimentUserComposedMissionRequest> missions) {
}
