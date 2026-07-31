package com.naroom.api.experiment.dto;

import com.naroom.api.experiment.domain.entity.ExperimentMission;
import com.naroom.api.experiment.domain.entity.ExperimentMissionType;
import com.naroom.api.experiment.domain.entity.ExperimentProgramMission;

import java.util.UUID;

// 코스 상세/랜덤 코스 미리보기에서 일차별로 보여줄 요약이다. 전체 안내문·질문 등은 오늘의 미션
// 화면(E08)에서만 노출한다.
public record ExperimentProgramMissionResponse(
		short dayNumber, UUID missionId, String missionCode, String title,
		ExperimentMissionType missionType, short estimatedMinutes) {

	public static ExperimentProgramMissionResponse from(ExperimentProgramMission programMission) {
		ExperimentMission mission = programMission.getMission();
		return new ExperimentProgramMissionResponse(
				programMission.getDayNumber(), mission.getId(), mission.getCode(), mission.getTitle(),
				mission.getMissionType(), mission.getEstimatedMinutes());
	}

	public static ExperimentProgramMissionResponse of(short dayNumber, ExperimentMission mission) {
		return new ExperimentProgramMissionResponse(
				dayNumber, mission.getId(), mission.getCode(), mission.getTitle(),
				mission.getMissionType(), mission.getEstimatedMinutes());
	}

}
