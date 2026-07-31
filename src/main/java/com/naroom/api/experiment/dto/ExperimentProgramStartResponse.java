package com.naroom.api.experiment.dto;

import com.naroom.api.experiment.domain.entity.UserExperimentProgramStatus;

import java.util.UUID;

public record ExperimentProgramStartResponse(
		UUID userExperimentProgramId,
		UserExperimentProgramStatus status,
		String title,
		short durationDays,
		short currentDay,
		int lookedAtMissionCount,
		int restedDateCount,
		ExperimentUserProgramMissionResponse todayMission) {
}
