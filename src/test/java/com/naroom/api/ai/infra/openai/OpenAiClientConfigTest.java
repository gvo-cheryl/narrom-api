package com.naroom.api.ai.infra.openai;

import com.openai.client.OpenAIClient;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertNotNull;

// 네트워크 호출 없이(빈 문자열 키 포함) 클라이언트 빌더 자체가 안전하게 구성되는지만 확인한다.
// 실제 API 인증·응답 검증은 opt-in 테스트(OpenAiClientIntegrationTest, @Tag("openai-integration"))가 맡는다.
class OpenAiClientConfigTest {

	@Test
	void openAiClient_buildsWithoutNetworkCallEvenWithBlankKey() {
		OpenAIClient client = new OpenAiClientConfig().openAiClient(new OpenAiProperties("", ""));

		assertNotNull(client);
	}

}
