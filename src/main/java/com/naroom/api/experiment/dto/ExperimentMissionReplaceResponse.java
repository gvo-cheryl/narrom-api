package com.naroom.api.experiment.dto;

import java.util.UUID;

public record ExperimentMissionReplaceResponse(
		UUID userProgramMissionId,
		short dayNumber,
		UUID originalMissionId,
		UUID missionId,
		int replacementCount) {
}
