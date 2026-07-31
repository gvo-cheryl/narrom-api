package com.naroom.api.experiment.dto;

import java.util.List;

// 실제로 저장하지 않는 "제한적 랜덤 코스" 미리보기다 - 사용자가 E05에서 미션을 바꾸거나 그대로
// 시작할 때 비로소 8-C의 시작 API가 UserExperimentProgram으로 만든다.
public record ExperimentRandomProgramResponse(short durationDays, List<ExperimentProgramMissionResponse> missions) {
}
