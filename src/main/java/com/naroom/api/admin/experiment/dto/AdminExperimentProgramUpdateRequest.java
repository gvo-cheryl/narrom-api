package com.naroom.api.admin.experiment.dto;

import com.naroom.api.experiment.domain.entity.ExperimentSourceType;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.util.List;
import java.util.UUID;

public record AdminExperimentProgramUpdateRequest(
		@NotNull UUID primaryTopicId,
		@NotBlank String title,
		@NotBlank String description,
		@Positive short durationDays,
		@NotNull ExperimentSourceType sourceType,
		@Positive short estimatedMinutesMin,
		@Positive short estimatedMinutesMax,
		@NotNull Boolean featured,
		@NotNull Boolean beginner,
		int displayOrder,
		@NotEmpty @Valid List<AdminExperimentProgramDayMissionRequest> days) {
}
