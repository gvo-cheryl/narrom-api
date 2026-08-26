package com.naroom.api.ai.result;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

// 9.1절 예시를 실제 데이터 모델에 맞게 조정한 Structured Output 스키마다.
// 문서 예시는 emotionCandidates.code(예: "FRUSTRATION")/suggestedTagIds(예: "WORK")처럼 영문 심볼 코드를 쓰지만,
// Tag 엔티티에는 그런 코드 컬럼이 없고 name/normalizedName(예: "답답함")만 있다. 없는 코드 체계를 만들어내는 대신
// 모델이 실제 태그 표시명과 같은 표현으로 응답하게 하고, EntryReflectionResponseParser가 normalizedName으로 매칭한다.
// safetyStatus는 모델 자기보고 값일 뿐이며, 실제 출력 안전 판정은 별도 Moderation API 호출(4-H)이 담당한다.
public final class EntryReflectionSchema {

	// summaryMaxLength는 관리자가 발행한 지침의 output_max_length다 - null이면 글자 수 제한을 두지 않는다.
	public static Map<String, Object> schema(Integer summaryMaxLength) {
		Map<String, Object> summaryField = new HashMap<>(Map.of(
				"type", "string",
				"description",
				"기록을 다정하게 다듬어 돌려주는 정리. 사용자가 쓴 문장을 그대로 나열하거나 기계적으로 요약하지 "
						+ "않는다 - 문맥을 자연스럽게 다듬어 표현하고, 사용자가 쓰지 않은 내용을 지어내지 않는다."));
		if (summaryMaxLength != null) {
			summaryField.put("maxLength", summaryMaxLength);
		}

		return Map.of(
				"type", "object",
				"properties", Map.ofEntries(
						Map.entry("summary", summaryField),
						Map.entry("emotionCandidates", Map.of(
								"type", "array",
								"maxItems", 5,
								"items", Map.of(
										"type", "object",
										"properties", Map.of(
												"name", Map.of("type", "string"),
												"confidence", Map.of(
														"type", "number",
														"description", "0(전혀 확신 없음)부터 1(매우 확신) 사이의 값.")),
										"required", List.of("name", "confidence"),
										"additionalProperties", false))),
						Map.entry("suggestedTagNames", Map.of(
								"type", "array",
								"maxItems", 5,
								"items", Map.of("type", "string"),
								"description", "이미 존재하는 표준 태그 표시명 중 관련 있는 것만 고른다. 새 태그명을 지어내지 않는다.")),
						Map.entry("reflectionQuestion", Map.of(
								"type", "string",
								"description", "사용자가 스스로 생각해볼 후속 질문 정확히 1개.")),
						Map.entry("evidenceEntryIds", Map.of(
								"type", "array",
								"maxItems", 5,
								"items", Map.of("type", "string"),
								"description", "이 정리의 근거가 된 기록 ID.")),
						Map.entry("safetyStatus", Map.of(
								"type", "string",
								"enum", List.of("NORMAL", "RESTRICTED", "CRISIS")))),
				"required", List.of(
						"summary", "emotionCandidates", "suggestedTagNames",
						"reflectionQuestion", "evidenceEntryIds", "safetyStatus"),
				"additionalProperties", false);
	}

	private EntryReflectionSchema() {
	}

}
