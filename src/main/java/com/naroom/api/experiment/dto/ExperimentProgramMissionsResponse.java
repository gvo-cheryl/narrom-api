package com.naroom.api.experiment.dto;

import java.util.List;

// E10(전체 진행 보기)·E11(남은 미션 바꾸기)·entry 상세 화면 코스 연결에서 쓰는 일차별 미션·기록 목록.
public record ExperimentProgramMissionsResponse(
		List<ExperimentProgramDayResponse> days, List<ExperimentRestedDateResponse> restedDates) {
}
