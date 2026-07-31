package com.naroom.api.experiment.dto;

import com.naroom.api.experiment.domain.entity.ExperimentAttemptStatus;
import com.naroom.api.experiment.domain.entity.UserExperimentProgramStatus;

public record ExperimentMissionRecordResponse(
		ExperimentAttemptStatus attemptStatus,
		boolean missionConsumed,
		UserExperimentProgramStatus status,
		short currentDay,
		boolean sameMissionRemains,
		int lookedAtMissionCount,
		int restedDateCount) {
}
