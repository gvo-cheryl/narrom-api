package com.naroom.api.admin.experiment.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;

// code는 다른 테이블이 참조하는 안정 식별자라 수정 대상에서 제외한다.
public record AdminExperimentTopicUpdateRequest(
		@NotBlank String name,
		String description,
		@NotNull @PositiveOrZero Integer displayOrder,
		@NotNull Boolean active) {
}
