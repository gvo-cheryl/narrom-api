package com.naroom.api.admin.experiment.dto;

import com.naroom.api.experiment.domain.entity.ExperimentProgram;
import com.naroom.api.experiment.domain.entity.ExperimentProgramMission;
import com.naroom.api.experiment.domain.entity.ExperimentProgramStatus;
import com.naroom.api.experiment.domain.entity.ExperimentSourceType;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record AdminExperimentProgramResponse(
		UUID id,
		String code,
		int contentVersion,
		UUID primaryTopicId,
		String primaryTopicCode,
		String primaryTopicName,
		String title,
		String description,
		short durationDays,
		ExperimentSourceType sourceType,
		ExperimentProgramStatus status,
		short estimatedMinutesMin,
		short estimatedMinutesMax,
		boolean featured,
		boolean beginner,
		int displayOrder,
		UUID supersedesProgramId,
		UUID createdByAdminId,
		List<AdminExperimentProgramDayMissionResponse> days,
		Instant createdAt,
		Instant updatedAt) {

	public static AdminExperimentProgramResponse from(
			ExperimentProgram program, List<ExperimentProgramMission> programMissions) {
		return new AdminExperimentProgramResponse(
				program.getId(),
				program.getCode(),
				program.getContentVersion(),
				program.getPrimaryTopic().getId(),
				program.getPrimaryTopic().getCode(),
				program.getPrimaryTopic().getName(),
				program.getTitle(),
				program.getDescription(),
				program.getDurationDays(),
				program.getSourceType(),
				program.getStatus(),
				program.getEstimatedMinutesMin(),
				program.getEstimatedMinutesMax(),
				program.isFeatured(),
				program.isBeginner(),
				program.getDisplayOrder(),
				program.getSupersedesProgramId(),
				program.getCreatedByAdminId(),
				programMissions.stream().map(AdminExperimentProgramDayMissionResponse::from).toList(),
				program.getCreatedAt(),
				program.getUpdatedAt());
	}

}
