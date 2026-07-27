package com.naroom.api.ai.prompt;

// 14장 5단계 조립 구조를 그대로 표현한다. 4-F가 이 레이어들을 Responses API 호출 형태(예: instructions/input)로
// 매핑하므로, 여기서 하나의 문자열로 미리 합쳐두지 않는다.
public record AssembledPrompt(
		String commonInstructionsVersion,
		String commonInstructions,
		String featureInstructionsVersion,
		String featureInstructions,
		String preferenceInstructions,
		String contextContent,
		String outputSchemaVersion) {
}
