package com.naroom.api.experiment.dto;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.naroom.api.experiment.domain.entity.ExperimentMissionType;
import com.naroom.api.experiment.domain.entity.UserProgramMission;

import java.util.List;
import java.util.UUID;

// 오늘의 작은 실험(E08 등)에서 보여줄 사용자별 슬롯. 카탈로그 미션과 달리 스냅샷 문구를 쓰고
// userProgramMissionId로 기록·교체 API를 호출한다.
public record ExperimentUserProgramMissionResponse(
		short dayNumber, UUID missionId, String missionCode, String title, String instruction,
		ExperimentMissionType missionType, short estimatedMinutes, List<String> reflectionQuestions,
		UUID userProgramMissionId) {

	// 이 프로젝트는 ObjectMapper를 스프링 빈으로 노출하지 않는다 - reflection_questions_snapshot(jsonb)을
	// 문자열 배열로 풀어내는 용도로만 쓴다.
	private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

	public static ExperimentUserProgramMissionResponse from(UserProgramMission slot) {
		return new ExperimentUserProgramMissionResponse(
				slot.getDayNumber(),
				slot.getMission() != null ? slot.getMission().getId() : null,
				slot.getMission() != null ? slot.getMission().getCode() : null,
				slot.getTitleSnapshot(),
				slot.getInstructionSnapshot(),
				slot.getMissionType(),
				slot.getEstimatedMinutes(),
				parseReflectionQuestions(slot.getReflectionQuestionsSnapshot()),
				slot.getId());
	}

	private static List<String> parseReflectionQuestions(String reflectionQuestionsSnapshot) {
		if (reflectionQuestionsSnapshot == null || reflectionQuestionsSnapshot.isBlank()) {
			return List.of();
		}
		try {
			return OBJECT_MAPPER.readValue(reflectionQuestionsSnapshot, new TypeReference<List<String>>() {
			});
		} catch (Exception e) {
			return List.of();
		}
	}

}
