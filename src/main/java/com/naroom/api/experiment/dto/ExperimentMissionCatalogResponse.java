package com.naroom.api.experiment.dto;

import com.naroom.api.experiment.domain.entity.ExperimentMission;
import com.naroom.api.experiment.domain.entity.ExperimentMissionType;

import java.util.UUID;

// 시작 전 미션 교체(E06)·진행 중 미션 교체(E08/E11)에서 대체 후보를 고를 때 쓰는 목록 조회 응답.
public record ExperimentMissionCatalogResponse(
		UUID id, String code, String title, String description, String topicCode,
		ExperimentMissionType missionType, short estimatedMinutes) {

	public static ExperimentMissionCatalogResponse from(ExperimentMission mission) {
		return new ExperimentMissionCatalogResponse(
				mission.getId(), mission.getCode(), mission.getTitle(), mission.getDescription(),
				mission.getTopic().getCode(), mission.getMissionType(), mission.getEstimatedMinutes());
	}

}
