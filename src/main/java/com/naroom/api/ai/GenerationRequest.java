package com.naroom.api.ai;

import java.util.Map;

// 5.3절: maxOutputTokens는 기능 유형별로 다르므로(개별 기록 350, 3일 회고 600, 주간 회고 900) 호출자가 정한다.
// jsonSchema는 4-G에서 기능별로 정의될 실제 Structured Output 스키마를 그대로 전달받는다 - 이 레이어는 스키마
// 내용을 모르고 그대로 전달만 한다.
public record GenerationRequest(
		String instructions,
		String input,
		long maxOutputTokens,
		String schemaName,
		Map<String, Object> jsonSchema) {
}
