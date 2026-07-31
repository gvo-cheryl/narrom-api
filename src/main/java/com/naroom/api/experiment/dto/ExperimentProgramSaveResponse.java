package com.naroom.api.experiment.dto;

import com.naroom.api.experiment.domain.entity.UserExperimentProgramStatus;

import java.util.UUID;

// §5.2 "저장하고 나중에 시작" - 시작하지 않고 READY로만 만들어둔다.
public record ExperimentProgramSaveResponse(
		UUID userExperimentProgramId,
		UserExperimentProgramStatus status,
		String title,
		short durationDays) {
}
