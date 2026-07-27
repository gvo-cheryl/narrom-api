package com.naroom.api.ai.infra.openai;

import com.naroom.api.ai.AiModerationClient;
import com.naroom.api.ai.domain.entity.AiSafetyGrade;
import com.openai.client.OpenAIClient;
import com.openai.models.moderations.Moderation;
import com.openai.models.moderations.ModerationCreateParams;
import com.openai.models.moderations.ModerationCreateResponse;
import com.openai.models.moderations.ModerationModel;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

// 5.1절: 입력·출력 안전 분류 모델은 omni-moderation-latest로 고정한다. 생성 모델(OpenAiProperties.model())과
// 달리 환경변수로 바꾸지 않는다 - 안전 분류 모델 교체는 별도 정책 결정 사안이다.
@Component
public class OpenAiModerationClient implements AiModerationClient {

	private final OpenAIClient openAiClient;

	// OpenAIClient 빈은 @Lazy로 등록돼 있다(OpenAiClientConfig). 여기서도 @Lazy를 명시해 실제 SDK 클라이언트가
	// classify() 최초 호출 전까지 생성되지 않게 한다 - 생략하면 생성자 주입 시점에 즉시 실체화되어 버린다.
	public OpenAiModerationClient(@Lazy OpenAIClient openAiClient) {
		this.openAiClient = openAiClient;
	}

	@Override
	public AiSafetyGrade classify(String text) {
		ModerationCreateParams params = ModerationCreateParams.builder()
				.input(text)
				.model(ModerationModel.OMNI_MODERATION_LATEST)
				.build();
		ModerationCreateResponse response = openAiClient.moderations().create(params);
		Moderation result = response.results().get(0);
		return toSafetyGrade(result);
	}

	// 8.2/8.4절: 심각한 위험 신호(자해)는 일반 차단이 아니라 안전 지원 흐름으로 분기해야 하므로 CRISIS로 별도 분류한다.
	static AiSafetyGrade toSafetyGrade(Moderation moderation) {
		if (!moderation.flagged()) {
			return AiSafetyGrade.NORMAL;
		}
		Moderation.Categories categories = moderation.categories();
		boolean selfHarmSignal = categories.selfHarm() || categories.selfHarmIntent() || categories.selfHarmInstructions();
		return selfHarmSignal ? AiSafetyGrade.CRISIS : AiSafetyGrade.RESTRICTED;
	}

}
