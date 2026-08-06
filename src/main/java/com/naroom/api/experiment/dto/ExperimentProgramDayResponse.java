package com.naroom.api.experiment.dto;

import com.naroom.api.experiment.domain.entity.ExperimentMissionRecord;
import com.naroom.api.experiment.domain.entity.ExperimentMissionType;
import com.naroom.api.experiment.domain.entity.UserProgramMission;
import com.naroom.api.experiment.domain.entity.UserProgramMissionSlotStatus;

import java.util.UUID;

// E10(전체 진행 보기)의 일차 카드 하나. record가 없으면 아직 기록하지 않은(PENDING/CURRENT) 날이다.
// userProgramMissionId는 이 슬롯을 기록·교체(§13 미션 기록/교체 트랜잭션)할 때 그대로 쓸 수 있는
// 슬롯 자체의 id다 - missionId(카탈로그 미션 id)와는 다른 값이다.
public record ExperimentProgramDayResponse(
		short dayNumber, UUID userProgramMissionId, UUID missionId, String missionCode, String title,
		ExperimentMissionType missionType, short estimatedMinutes, UserProgramMissionSlotStatus slotStatus,
		boolean replaced, ExperimentDayRecordResponse record) {

	public static ExperimentProgramDayResponse of(UserProgramMission slot, ExperimentMissionRecord consumingRecord) {
		return new ExperimentProgramDayResponse(
				slot.getDayNumber(),
				slot.getId(),
				slot.getMission() != null ? slot.getMission().getId() : null,
				slot.getMission() != null ? slot.getMission().getCode() : null,
				slot.getTitleSnapshot(),
				slot.getMissionType(),
				slot.getEstimatedMinutes(),
				slot.getSlotStatus(),
				slot.getReplacementCount() > 0,
				consumingRecord == null ? null : ExperimentDayRecordResponse.from(consumingRecord));
	}

}
