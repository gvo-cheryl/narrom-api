package com.naroom.api.admin.experiment.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;

public record AdminExperimentTopicCreateRequest(
		@NotBlank String code,
		@NotBlank String name,
		String description,
		@NotNull @PositiveOrZero Integer displayOrder,
		@NotNull Boolean active) {
}
