package com.naroom.api.experiment.dto;

import com.naroom.api.experiment.domain.entity.ExperimentMissionRecord;

import java.time.LocalDate;

// E10 "쉬어간 날" 목록 한 줄. 쉰 날은 미션을 소비하지 않고 코스 기간만 그만큼 늘어난다(§13/DEC-03).
public record ExperimentRestedDateResponse(LocalDate recordDate, short dayNumber, String missionTitle) {

	public static ExperimentRestedDateResponse from(ExperimentMissionRecord restRecord) {
		return new ExperimentRestedDateResponse(
				restRecord.getRecordDate(),
				restRecord.getUserProgramMission().getDayNumber(),
				restRecord.getUserProgramMission().getTitleSnapshot());
	}

}
