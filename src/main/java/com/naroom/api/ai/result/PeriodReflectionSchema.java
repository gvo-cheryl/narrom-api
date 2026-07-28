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
					Map.entry("summary", Map.of("type", "string")),
					Map.entry("repeatedEmotionsAndSituations", stringArray()),
					Map.entry("difficultMoments", stringArray()),
					Map.entry("gratefulMoments", stringArray()),
					Map.entry("triedResponses", stringArray()),
					Map.entry("helpfulConditions", stringArray()),
					Map.entry("reflectionQuestion", Map.of("type", "string")),
					Map.entry("evidenceEntryIds", Map.of(
							"type", "array",
							"maxItems", 10,
							"items", Map.of("type", "string"))),
					Map.entry("safetyStatus", Map.of(
							"type", "string",
							"enum", List.of("NORMAL", "RESTRICTED", "CRISIS")))),
			"required", List.of(
					"summary", "repeatedEmotionsAndSituations", "difficultMoments", "gratefulMoments",
					"triedResponses", "helpfulConditions", "reflectionQuestion", "evidenceEntryIds", "safetyStatus"),
			"additionalProperties", false);

	private static Map<String, Object> stringArray() {
		return Map.of("type", "array", "maxItems", 5, "items", Map.of("type", "string"));
	}

	private PeriodReflectionSchema() {
	}

}
