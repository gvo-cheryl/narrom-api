package com.naroom.api.experiment.dto;

import java.util.List;
import java.util.UUID;

// replaceActiveProgram은 Boolean으로 둔다 - Jackson 3(Spring Boot 4 기본)은 primitive boolean
// 컴포넌트가 요청 본문에 아예 없으면 FAIL_ON_NULL_FOR_PRIMITIVES로 예외를 던지므로, 선택 필드는
// 박싱 타입으로 받아 컴팩트 생성자에서 기본값을 채운다.
public record ExperimentProgramStartRequest(
		Integer expectedContentVersion,
		List<ExperimentMissionOverride> missionOverrides,
		UUID recommendationId,
		Boolean replaceActiveProgram) {

	public ExperimentProgramStartRequest {
		missionOverrides = missionOverrides == null ? List.of() : missionOverrides;
		replaceActiveProgram = replaceActiveProgram == null ? Boolean.FALSE : replaceActiveProgram;
	}

}
