package com.naroom.api.experiment.dto;

import com.naroom.api.experiment.domain.entity.UserExperimentProgramStatus;

import java.util.UUID;

// §5.3 진행 중 - "오늘의 작은 실험" 조회. todayMission은 AWAITING_REVIEW(마지막 미션까지 기록을 마친
// 상태)에서는 CURRENT 슬롯이 없으므로 null이다.
public record ExperimentActiveProgramResponse(
		UUID userExperimentProgramId,
		UserExperimentProgramStatus status,
		String title,
		short durationDays,
		short currentDay,
		int lookedAtMissionCount,
		int restedDateCount,
		ExperimentUserProgramMissionResponse todayMission) {
}
