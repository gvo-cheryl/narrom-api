package com.naroom.api.experiment.dto;

import com.naroom.api.experiment.domain.entity.ExperimentSourceType;
import com.naroom.api.experiment.domain.entity.UserExperimentProgramStatus;

import java.util.UUID;

public record ExperimentUserComposedProgramResponse(
		UUID userExperimentProgramId,
		UUID programId,
		ExperimentSourceType configurationSource,
		UserExperimentProgramStatus status,
		short durationDays,
		int missionCount) {
}
