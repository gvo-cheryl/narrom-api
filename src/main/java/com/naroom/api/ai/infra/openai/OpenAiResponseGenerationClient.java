package com.naroom.api.ai.infra.openai;

import com.naroom.api.ai.AiResponseGenerationClient;
import com.naroom.api.ai.GenerationRequest;
import com.naroom.api.ai.GenerationResult;
import com.openai.client.OpenAIClient;
import com.openai.core.JsonValue;
import com.openai.models.responses.Response;
import com.openai.models.responses.ResponseCreateParams;
import com.openai.models.responses.ResponseFormatTextJsonSchemaConfig;
import com.openai.models.responses.ResponseOutputMessage;
import com.openai.models.responses.ResponseTextConfig;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.stream.Collectors;

// 5.1/5.2절: 생성 호출은 Responses API를 쓰고 OpenAI 측에는 대화 상태를 저장하지 않는다(store:false).
@Component
public class OpenAiResponseGenerationClient implements AiResponseGenerationClient {

	private final OpenAIClient openAiClient;
	private final OpenAiProperties properties;

	// OpenAIClient 빈은 @Lazy로 등록돼 있다(OpenAiClientConfig). 여기서도 @Lazy를 명시해 실제 SDK 클라이언트가
	// generate() 최초 호출 전까지 생성되지 않게 한다.
	public OpenAiResponseGenerationClient(@Lazy OpenAIClient openAiClient, OpenAiProperties properties) {
		this.openAiClient = openAiClient;
		this.properties = properties;
	}

	@Override
	public GenerationResult generate(GenerationRequest request) {
		ResponseFormatTextJsonSchemaConfig.Schema.Builder schemaBuilder = ResponseFormatTextJsonSchemaConfig.Schema.builder();
		request.jsonSchema().forEach((key, value) -> schemaBuilder.putAdditionalProperty(key, JsonValue.from(value)));

		ResponseFormatTextJsonSchemaConfig format = ResponseFormatTextJsonSchemaConfig.builder()
				.name(request.schemaName())
				.schema(schemaBuilder.build())
				.strict(true)
				.build();
		ResponseTextConfig textConfig = ResponseTextConfig.builder().format(format).build();

		ResponseCreateParams params = ResponseCreateParams.builder()
				.model(properties.model())
				.instructions(request.instructions())
				.input(request.input())
				.text(textConfig)
				.maxOutputTokens(request.maxOutputTokens())
				.store(false)
				.build();

		Response response = openAiClient.responses().create(params);
		String outputJson = extractOutputText(response);
		long inputTokens = response.usage().map(usage -> usage.inputTokens()).orElse(0L);
		long outputTokens = response.usage().map(usage -> usage.outputTokens()).orElse(0L);
		return new GenerationResult(outputJson, inputTokens, outputTokens);
	}

	private String extractOutputText(Response response) {
		return response.output().stream()
				.flatMap(item -> item.message().stream())
				.flatMap(message -> message.content().stream())
				.filter(ResponseOutputMessage.Content::isOutputText)
				.map(content -> content.asOutputText().text())
				.collect(Collectors.joining());
	}

}
