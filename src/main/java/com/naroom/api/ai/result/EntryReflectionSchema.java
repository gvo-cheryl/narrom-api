package com.naroom.api.ai.result;

import java.util.List;
import java.util.Map;

// 9.1절 예시를 실제 데이터 모델에 맞게 조정한 Structured Output 스키마다.
// 문서 예시는 emotionCandidates.code(예: "FRUSTRATION")/suggestedTagIds(예: "WORK")처럼 영문 심볼 코드를 쓰지만,
// Tag 엔티티에는 그런 코드 컬럼이 없고 name/normalizedName(예: "답답함")만 있다. 없는 코드 체계를 만들어내는 대신
// 모델이 실제 태그 표시명과 같은 표현으로 응답하게 하고, EntryReflectionResponseParser가 normalizedName으로 매칭한다.
// safetyStatus는 모델 자기보고 값일 뿐이며, 실제 출력 안전 판정은 별도 Moderation API 호출(4-H)이 담당한다.
public final class EntryReflectionSchema {

	public static final Map<String, Object> SCHEMA = Map.of(
			"type", "object",
			"properties", Map.ofEntries(
					Map.entry("summary", Map.of("type", "string")),
					Map.entry("emotionCandidates", Map.of(
							"type", "array",
							"maxItems", 5,
							"items", Map.of(
									"type", "object",
									"properties", Map.of(
											"name", Map.of("type", "string"),
											"confidence", Map.of("type", "number")),
									"required", List.of("name", "confidence"),
									"additionalProperties", false))),
					Map.entry("suggestedTagNames", Map.of(
							"type", "array",
							"maxItems", 5,
							"items", Map.of("type", "string"))),
					Map.entry("reflectionQuestion", Map.of("type", "string")),
					Map.entry("evidenceEntryIds", Map.of(
							"type", "array",
							"maxItems", 5,
							"items", Map.of("type", "string"))),
					Map.entry("safetyStatus", Map.of(
							"type", "string",
							"enum", List.of("NORMAL", "RESTRICTED", "CRISIS")))),
			"required", List.of(
					"summary", "emotionCandidates", "suggestedTagNames",
					"reflectionQuestion", "evidenceEntryIds", "safetyStatus"),
			"additionalProperties", false);

	private EntryReflectionSchema() {
	}

}
