package com.naroom.api.experiment.dto;

import com.naroom.api.experiment.domain.entity.UserExperimentProgram;
import com.naroom.api.experiment.domain.entity.UserExperimentProgramStatus;

import java.time.Instant;
import java.util.UUID;

// E13(지난 작은 실험) 목록. COMPLETED/ENDED_EARLY 상태의 코스만 대상이다.
public record ExperimentPastProgramResponse(
		UUID userExperimentProgramId,
		UserExperimentProgramStatus status,
		String title,
		short durationDays,
		short currentDay,
		Instant startedAt,
		Instant completedAt,
		Instant endedEarlyAt) {

	public static ExperimentPastProgramResponse from(UserExperimentProgram program) {
		return new ExperimentPastProgramResponse(
				program.getId(),
				program.getStatus(),
				program.getTitleSnapshot(),
				program.getDurationDays(),
				program.getCurrentDay(),
				program.getStartedAt(),
				program.getCompletedAt(),
				program.getEndedEarlyAt());
	}

}
