package com.naroom.api.ai.infra.openai;

import com.naroom.api.ai.domain.entity.AiSafetyGrade;
import com.openai.client.OpenAIClient;
import com.openai.client.okhttp.OpenAIOkHttpClient;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

// 실제 OpenAI Moderation API에 붙는 opt-in 테스트. 기본 test 태스크는 제외한다(build.gradle.kts).
// 실행: 실제 OPENAI_API_KEY를 export한 뒤 ./gradlew openaiIntegrationTest
// 분류 위험 카테고리(자해 등)를 유발하는 문구는 테스트 픽스처에 두지 않는다 - 정상 입력에 대한 NORMAL 판정만 검증한다.
@Tag("openai-integration")
class OpenAiModerationClientIntegrationTest {

	@Test
	void classify_benignText_returnsNormal() {
		OpenAIClient client = OpenAIOkHttpClient.fromEnv();
		OpenAiModerationClient moderationClient = new OpenAiModerationClient(client);

		AiSafetyGrade grade = moderationClient.classify("오늘 하루도 무사히 잘 마쳤다. 감사한 하루였다.");

		assertEquals(AiSafetyGrade.NORMAL, grade);
	}

}
