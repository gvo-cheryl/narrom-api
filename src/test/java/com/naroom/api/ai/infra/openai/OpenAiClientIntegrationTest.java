package com.naroom.api.ai.infra.openai;

import com.openai.client.OpenAIClient;
import com.openai.client.okhttp.OpenAIOkHttpClient;
import com.openai.models.models.Model;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;

// 실제 OpenAI API에 붙는 opt-in 테스트. 기본 test 태스크는 이 태그를 제외한다(build.gradle.kts).
// 실행: OPENAI_API_KEY를 실제 값으로 export한 뒤 ./gradlew openaiIntegrationTest
// fromEnv()는 실제 OS 환경변수를 읽는다 - CI Secret/운영 Secret Manager 값은 여기 해당하므로
// 앱 런타임 빈(OpenAiClientConfig, Spring 설정 바인딩 경유)과 달리 이 테스트에서는 그대로 사용한다.
@Tag("openai-integration")
class OpenAiClientIntegrationTest {

	@Test
	void fromEnv_authenticatesAndListsModels() {
		OpenAIClient client = OpenAIOkHttpClient.fromEnv();

		List<Model> models = client.models().list().data();

		assertFalse(models.isEmpty());
	}

}
