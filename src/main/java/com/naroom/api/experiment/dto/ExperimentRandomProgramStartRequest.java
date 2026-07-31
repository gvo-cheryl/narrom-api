package com.naroom.api.experiment.dto;

// replaceActiveProgram은 Boolean으로 둔다(ExperimentProgramStartRequest와 동일한 이유 - Jackson 3의
// FAIL_ON_NULL_FOR_PRIMITIVES 회피).
public record ExperimentRandomProgramStartRequest(short durationDays, Boolean replaceActiveProgram) {

	public ExperimentRandomProgramStartRequest {
		replaceActiveProgram = replaceActiveProgram == null ? Boolean.FALSE : replaceActiveProgram;
	}

}
