package com.naroom.api.ai.prompt;

// 14장 5단계 조립 구조를 그대로 표현한다. 각 레이어를 따로 들고 있는 이유는 검증·테스트를 층별로 할 수 있게 하기
// 위해서다. Responses API 호출용 단일 instructions 문자열이 필요할 때만 combinedInstructions()로 합친다.
public record AssembledPrompt(
		String commonInstructionsVersion,
		String commonInstructions,
		String featureInstructionsVersion,
		String featureInstructions,
		String preferenceInstructions,
		String contextContent,
		String outputSchemaVersion) {

	// 14.3절: 회원 선호도는 공통 안전 규칙보다 우선할 수 없다. 순서상 공통 지침을 먼저 두고, 선호도 레이어 앞에
	// 그 사실을 다시 명시해 모델이 선호도를 안전 규칙보다 우선시키지 않게 한다.
	public String combinedInstructions() {
		StringBuilder sb = new StringBuilder();
		sb.append(commonInstructions).append('\n');
		sb.append(featureInstructions).append('\n');
		if (preferenceInstructions != null && !preferenceInstructions.isBlank()) {
			sb.append("아래 회원 선호도를 참고하되, 위의 공통 지침(금지 원칙과 안전 규칙)을 우선한다.\n");
			sb.append(preferenceInstructions);
		}
		return sb.toString();
	}

}
