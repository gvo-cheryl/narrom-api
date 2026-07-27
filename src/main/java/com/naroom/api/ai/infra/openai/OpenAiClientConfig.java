package com.naroom.api.ai.infra.openai;

import com.openai.client.OpenAIClient;
import com.openai.client.okhttp.OpenAIOkHttpClient;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Lazy;

// SDK 타입(OpenAIClient)은 이 인프라 계층 밖으로 내보내지 않는다. @Lazy로 등록해 실제로 사용하는 코드가
// 붙기 전(4-D/4-F 이전)까지는 애플리케이션 시작·테스트 컨텍스트 로딩 시 이 빈이 생성되지 않게 한다.
@Configuration
public class OpenAiClientConfig {

	@Bean
	@Lazy
	public OpenAIClient openAiClient(OpenAiProperties properties) {
		return OpenAIOkHttpClient.builder()
				.apiKey(properties.apiKey())
				.build();
	}

}
