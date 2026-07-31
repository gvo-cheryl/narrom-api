package com.naroom.api.experiment.dto;

import com.naroom.api.experiment.domain.entity.ExperimentAttemptStatus;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

// attemptStatus가 RESTED면 responseText/responseData/emotionTagIds/energyLevel/createLifeTimeEntry는
// 쓰지 않는다 - §13 미션 기록 트랜잭션은 RESTED와 그 외 시도 상태를 같은 트랜잭션(같은 엔드포인트)에서
// 처리하되 RESTED는 휴식 기록만 남기고 끝난다(§11.2).
public record ExperimentMissionRecordRequest(
		@NotNull ExperimentAttemptStatus attemptStatus,
		@NotNull LocalDate recordDate,
		String responseText,
		Object responseData,
		List<UUID> emotionTagIds,
		Short energyLevel,
		String reflection,
		Boolean createLifeTimeEntry) {

	public ExperimentMissionRecordRequest {
		emotionTagIds = emotionTagIds == null ? List.of() : emotionTagIds;
		createLifeTimeEntry = createLifeTimeEntry == null ? Boolean.FALSE : createLifeTimeEntry;
	}

}
