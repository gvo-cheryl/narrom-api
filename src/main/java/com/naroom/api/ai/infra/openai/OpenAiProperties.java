package com.naroom.api.ai.infra.openai;

import org.springframework.boot.context.properties.ConfigurationProperties;

// apiKey는 .env.local/CI Secret/운영 Secret Manager에서만 실제 값이 채워진다(.env.example의 값은 항상 빈 문자열).
// System.getenv 기반 OpenAIOkHttpClient.fromEnv() 대신 Spring 설정 바인딩을 쓰는 이유는, 로컬 개발에서
// .env.local이 실제 OS 환경변수가 아니라 Spring의 config-import로만 주입되기 때문이다(fromEnv는 이를 못 본다).
@ConfigurationProperties(prefix = "naroom.ai.openai")
public record OpenAiProperties(String apiKey, String model) {
}
