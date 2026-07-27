package com.naroom.api.ai.infra.openai;

import com.naroom.api.ai.domain.entity.AiSafetyGrade;
import com.openai.models.moderations.Moderation;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

// toSafetyGrade는 실제 SDK 타입(Moderation)을 받아 순수하게 분류만 하므로, 네트워크 없이 빌더로 값을 구성해 검증한다.
class OpenAiModerationClientTest {

	@Test
	void toSafetyGrade_notFlagged_returnsNormal() {
		Moderation moderation = Moderation.builder()
				.flagged(false)
				.categories(allFalseCategories())
				.categoryScores(zeroScores())
				.categoryAppliedInputTypes(emptyAppliedInputTypes())
				.build();

		assertEquals(AiSafetyGrade.NORMAL, OpenAiModerationClient.toSafetyGrade(moderation));
	}

	@Test
	void toSafetyGrade_flaggedWithoutSelfHarm_returnsRestricted() {
		Moderation moderation = Moderation.builder()
				.flagged(true)
				.categories(allFalseCategories().toBuilder().violence(true).build())
				.categoryScores(zeroScores())
				.categoryAppliedInputTypes(emptyAppliedInputTypes())
				.build();

		assertEquals(AiSafetyGrade.RESTRICTED, OpenAiModerationClient.toSafetyGrade(moderation));
	}

	@Test
	void toSafetyGrade_flaggedWithSelfHarm_returnsCrisis() {
		Moderation moderation = Moderation.builder()
				.flagged(true)
				.categories(allFalseCategories().toBuilder().selfHarm(true).build())
				.categoryScores(zeroScores())
				.categoryAppliedInputTypes(emptyAppliedInputTypes())
				.build();

		assertEquals(AiSafetyGrade.CRISIS, OpenAiModerationClient.toSafetyGrade(moderation));
	}

	@Test
	void toSafetyGrade_flaggedWithSelfHarmIntentOnly_returnsCrisis() {
		Moderation moderation = Moderation.builder()
				.flagged(true)
				.categories(allFalseCategories().toBuilder().selfHarmIntent(true).build())
				.categoryScores(zeroScores())
				.categoryAppliedInputTypes(emptyAppliedInputTypes())
				.build();

		assertEquals(AiSafetyGrade.CRISIS, OpenAiModerationClient.toSafetyGrade(moderation));
	}

	private Moderation.Categories allFalseCategories() {
		return Moderation.Categories.builder()
				.harassment(false)
				.harassmentThreatening(false)
				.hate(false)
				.hateThreatening(false)
				.illicit(false)
				.illicitViolent(false)
				.selfHarm(false)
				.selfHarmInstructions(false)
				.selfHarmIntent(false)
				.sexual(false)
				.sexualMinors(false)
				.violence(false)
				.violenceGraphic(false)
				.build();
	}

	private Moderation.CategoryScores zeroScores() {
		return Moderation.CategoryScores.builder()
				.harassment(0.0)
				.harassmentThreatening(0.0)
				.hate(0.0)
				.hateThreatening(0.0)
				.illicit(0.0)
				.illicitViolent(0.0)
				.selfHarm(0.0)
				.selfHarmInstructions(0.0)
				.selfHarmIntent(0.0)
				.sexual(0.0)
				.sexualMinors(0.0)
				.violence(0.0)
				.violenceGraphic(0.0)
				.build();
	}

	private Moderation.CategoryAppliedInputTypes emptyAppliedInputTypes() {
		return Moderation.CategoryAppliedInputTypes.builder()
				.harassment(java.util.List.of())
				.harassmentThreatening(java.util.List.of())
				.hate(java.util.List.of())
				.hateThreatening(java.util.List.of())
				.illicit(java.util.List.of())
				.illicitViolent(java.util.List.of())
				.selfHarm(java.util.List.of())
				.selfHarmInstructions(java.util.List.of())
				.selfHarmIntent(java.util.List.of())
				.sexual(java.util.List.of())
				.sexualMinors(java.util.List.of())
				.violence(java.util.List.of())
				.violenceGraphic(java.util.List.of())
				.build();
	}

}
