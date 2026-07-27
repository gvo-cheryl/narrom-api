package com.naroom.api.ai.infra.openai;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.naroom.api.ai.GenerationRequest;
import com.naroom.api.ai.GenerationResult;
import com.openai.client.OpenAIClient;
import com.openai.client.okhttp.OpenAIOkHttpClient;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertTrue;

// 실제 OpenAI Responses API에 붙는 opt-in 테스트. 기본 test 태스크는 제외한다(build.gradle.kts).
// 실행: 실제 OPENAI_API_KEY/OPENAI_MODEL을 export한 뒤 ./gradlew openaiIntegrationTest
// 실제 기능별 스키마(4-G 예정)가 아니라, Structured Output 연동 자체가 동작하는지 확인하는 최소 스키마를 쓴다.
@Tag("openai-integration")
class OpenAiResponseGenerationClientIntegrationTest {

	@Test
	void generate_withMinimalSchema_returnsJsonMatchingSchemaAndTokenUsage() throws Exception {
		OpenAIClient client = OpenAIOkHttpClient.fromEnv();
		OpenAiProperties properties = new OpenAiProperties("", System.getenv("OPENAI_MODEL"));
		OpenAiResponseGenerationClient generationClient = new OpenAiResponseGenerationClient(client, properties);

		Map<String, Object> schema = Map.of(
				"type", "object",
				"properties", Map.of("summary", Map.of("type", "string")),
				"required", List.of("summary"),
				"additionalProperties", false);
		GenerationRequest request = new GenerationRequest(
				"입력을 한 문장으로 요약해서 summary 필드에만 담아 응답하라.",
				"오늘은 산책을 하며 마음이 편안해졌다.",
				200,
				"smoke-test-schema",
				schema);

		GenerationResult result = generationClient.generate(request);

		JsonNode json = new ObjectMapper().readTree(result.outputJson());
		assertTrue(json.has("summary"));
		assertTrue(result.inputTokens() > 0);
		assertTrue(result.outputTokens() > 0);
	}

}
