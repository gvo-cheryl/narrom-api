package com.naroom.api.admin.experiment.dto;

import com.naroom.api.experiment.domain.entity.ExperimentProgramMission;

import java.util.UUID;

public record AdminExperimentProgramDayMissionResponse(
		short dayNumber,
		UUID missionId,
		String missionCode,
		String missionTitle,
		boolean missionActive,
		boolean replaceable,
		String replacementGroup) {

	public static AdminExperimentProgramDayMissionResponse from(ExperimentProgramMission programMission) {
		return new AdminExperimentProgramDayMissionResponse(
				programMission.getDayNumber(),
				programMission.getMission().getId(),
				programMission.getMission().getCode(),
				programMission.getMission().getTitle(),
				programMission.getMission().isActive(),
				programMission.isReplaceable(),
				programMission.getReplacementGroup());
	}

}
