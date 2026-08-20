package com.naroom.api.ai.result;

import java.util.List;
import java.util.Map;

// L6 화면 흐름(이번 주 기록 요약 → 반복된 감정과 상황 → 어려웠던 순간 → 감사하거나 다행이었던 일 →
// 시도했던 대응 → 도움이 되었던 조건 → 후속 질문)을 그대로 Structured Output 필드로 옮긴 것.
// "사용자의 자기정리"/"작은 실험 코스 선택"은 AI 출력이 아니라 사용자 행동이라 이 스키마에 없다.
// 3일/주간 회고가 같은 형식·기간만 다르므로(§3.2) 두 feature_type이 이 스키마를 공유한다.
public final class PeriodReflectionSchema {

	public static final Map<String, Object> SCHEMA = Map.of(
			"type", "object",
			"properties", Map.ofEntries(
					Map.entry("summary", Map.of(
							"type", "string",
							"description",
							"기간 전체 기록을 종합한 정리. 날짜별 기록을 그대로 나열하거나 이어붙이지 않는다 - "
									+ "자연스러운 문장으로 다정하게 정리해 돌려주고, 하루짜리 감정 상태를 기간 전체로 일반화하지 않는다.")),
					Map.entry("repeatedEmotionsAndSituations",
							stringArray("반복해서 나타난 감정과 상황. 한두 번뿐인 것은 반복으로 단정하지 않는다.")),
					Map.entry("difficultMoments", stringArray("어려웠던 순간.")),
					Map.entry("gratefulMoments", stringArray("감사하거나 다행이었던 일.")),
					Map.entry("triedResponses", stringArray("사용자가 실제로 시도한 대응.")),
					Map.entry("helpfulConditions", stringArray("도움이 되었던 조건.")),
					Map.entry("reflectionQuestion", Map.of(
							"type", "string",
							"description", "사용자가 스스로 생각해볼 후속 질문 정확히 1개.")),
					Map.entry("evidenceEntryIds", Map.of(
							"type", "array",
							"maxItems", 10,
							"items", Map.of("type", "string"),
							"description", "이 정리의 근거가 된 기록 ID.")),
					Map.entry("safetyStatus", Map.of(
							"type", "string",
							"enum", List.of("NORMAL", "RESTRICTED", "CRISIS")))),
			"required", List.of(
					"summary", "repeatedEmotionsAndSituations", "difficultMoments", "gratefulMoments",
					"triedResponses", "helpfulConditions", "reflectionQuestion", "evidenceEntryIds", "safetyStatus"),
			"additionalProperties", false);

	private static Map<String, Object> stringArray(String description) {
		return Map.of("type", "array", "maxItems", 5, "items", Map.of("type", "string"), "description", description);
	}

	private PeriodReflectionSchema() {
	}

}
