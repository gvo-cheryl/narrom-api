package com.naroom.api.experiment.dto;

import com.naroom.api.experiment.domain.entity.ExperimentProgram;
import com.naroom.api.experiment.domain.entity.ExperimentProgramStatus;
import com.naroom.api.experiment.domain.entity.ExperimentSourceType;

import java.util.List;
import java.util.UUID;

public record ExperimentProgramDetailResponse(
		UUID id,
		String code,
		int contentVersion,
		String title,
		String description,
		short durationDays,
		String primaryTopicCode,
		ExperimentSourceType sourceType,
		ExperimentProgramStatus status,
		boolean isFeatured,
		boolean isBeginner,
		short estimatedMinutesMin,
		short estimatedMinutesMax,
		int displayOrder,
		List<ExperimentProgramMissionResponse> missions) {

	public static ExperimentProgramDetailResponse of(
			ExperimentProgram program, List<ExperimentProgramMissionResponse> missions) {
		return new ExperimentProgramDetailResponse(
				program.getId(),
				program.getCode(),
				program.getContentVersion(),
				program.getTitle(),
				program.getDescription(),
				program.getDurationDays(),
				program.getPrimaryTopic().getCode(),
				program.getSourceType(),
				program.getStatus(),
				program.isFeatured(),
				program.isBeginner(),
				program.getEstimatedMinutesMin(),
				program.getEstimatedMinutesMax(),
				program.getDisplayOrder(),
				missions);
	}

}
