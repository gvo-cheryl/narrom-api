package com.naroom.api.experiment.dto;

import com.naroom.api.experiment.domain.entity.ExperimentAttemptStatus;
import com.naroom.api.experiment.domain.entity.ExperimentMissionRecord;

import java.time.LocalDate;

// E10(전체 진행 보기) 일차별 카드의 기록 미리보기. RESTED는 슬롯을 소비하지 않아 여기 포함되지
// 않는다(§13/DEC-03) - 쉬어간 날은 ExperimentRestedDateResponse로 별도로 내려준다.
public record ExperimentDayRecordResponse(
		ExperimentAttemptStatus attemptStatus, LocalDate recordDate, String responseText, String reflection) {

	public static ExperimentDayRecordResponse from(ExperimentMissionRecord record) {
		return new ExperimentDayRecordResponse(
				record.getAttemptStatus(), record.getRecordDate(), record.getResponseText(), record.getReflection());
	}

}
