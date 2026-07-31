package com.naroom.api.experiment.dto;

import jakarta.validation.constraints.NotNull;

import java.util.UUID;

// 시작 전(§5.2 "부담스러운 미션 교체")에 특정 일차의 카탈로그 기본 미션을 다른 미션으로 바꿔 시작한다.
public record ExperimentMissionOverride(
		short dayNumber,
		@NotNull UUID missionId) {
}
