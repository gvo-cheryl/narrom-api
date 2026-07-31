package com.naroom.api.experiment.dto;

import com.naroom.api.experiment.domain.entity.ExperimentMissionType;
import com.naroom.api.experiment.domain.entity.UserProgramMission;

import java.util.UUID;

// 오늘의 작은 실험(E08 등)에서 보여줄 사용자별 슬롯. 카탈로그 미션과 달리 스냅샷 문구를 쓰고
// userProgramMissionId로 기록·교체 API를 호출한다.
public record ExperimentUserProgramMissionResponse(
		short dayNumber, UUID missionId, String missionCode, String title,
		ExperimentMissionType missionType, short estimatedMinutes, UUID userProgramMissionId) {

	public static ExperimentUserProgramMissionResponse from(UserProgramMission slot) {
		return new ExperimentUserProgramMissionResponse(
				slot.getDayNumber(),
				slot.getMission() != null ? slot.getMission().getId() : null,
				slot.getMission() != null ? slot.getMission().getCode() : null,
				slot.getTitleSnapshot(),
				slot.getMissionType(),
				slot.getEstimatedMinutes(),
				slot.getId());
	}

}
