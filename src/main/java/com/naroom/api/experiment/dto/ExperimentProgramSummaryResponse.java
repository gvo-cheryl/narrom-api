package com.naroom.api.experiment.dto;

import com.naroom.api.experiment.domain.entity.ExperimentProgram;

import java.util.UUID;

public record ExperimentProgramSummaryResponse(
		UUID programId,
		String code,
		String title,
		short durationDays,
		String topicCode,
		String description,
		EstimatedMinutesRange estimatedMinutes,
		int missionCount) {

	// missionCount는 durationDays와 항상 같다 - V16 시드 무결성 검사(각 코스는 기간 일수만큼의 미션을
	// 가져야 함)가 이를 보장하므로 별도 집계 쿼리 없이 그대로 쓴다.
	public static ExperimentProgramSummaryResponse of(ExperimentProgram program) {
		return new ExperimentProgramSummaryResponse(
				program.getId(),
				program.getCode(),
				program.getTitle(),
				program.getDurationDays(),
				program.getPrimaryTopic().getCode(),
				program.getDescription(),
				new EstimatedMinutesRange(program.getEstimatedMinutesMin(), program.getEstimatedMinutesMax()),
				program.getDurationDays());
	}

}
