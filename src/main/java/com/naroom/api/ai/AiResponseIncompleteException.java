package com.naroom.api.ai;

// OpenAI Responses API가 max_output_tokens 등의 이유로 응답을 끝까지 완성하지 못했을 때 던진다. 이 경우
// 남은 텍스트가 우연히 파싱되더라도 내용이 잘려 있을 수 있어, 원인을 뭉뚱그려 JSON 파싱 실패로 남기지 않고
// 구분해서 기록한다(§5.3 토큰 상한이 실제로 부족한지 진단하는 근거가 된다).
public class AiResponseIncompleteException extends RuntimeException {

	public AiResponseIncompleteException(String reason) {
		super("AI response incomplete: " + reason);
	}

}
