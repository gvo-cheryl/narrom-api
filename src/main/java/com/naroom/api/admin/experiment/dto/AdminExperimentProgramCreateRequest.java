package com.naroom.api.admin.experiment.dto;

import com.naroom.api.experiment.domain.entity.ExperimentSourceType;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.util.List;
import java.util.UUID;

// code는 사용자가 입력하지 않는다 - 생성 시 서버가 자동으로 부여한다.
public record AdminExperimentProgramCreateRequest(
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
