package com.naroom.api.admin.experiment.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;

// code는 사용자가 입력하지 않는다 - 생성 시 서버가 자동으로 부여한다.
public record AdminExperimentTopicCreateRequest(
		@NotBlank String name,
		String description,
		@NotNull @PositiveOrZero Integer displayOrder,
		@NotNull Boolean active) {
}
