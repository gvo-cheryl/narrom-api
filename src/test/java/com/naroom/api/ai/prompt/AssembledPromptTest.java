package com.naroom.api.ai.prompt;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AssembledPromptTest {

	@Test
	void combinedInstructions_withPreference_includesSafetyReminderBeforePreference() {
		AssembledPrompt prompt = new AssembledPrompt(
				"common-v1", "공통 지침", "feature-v1", "기능별 지침", "말투 선호: DIRECT", "본문", "schema-v1");

		String combined = prompt.combinedInstructions();

		int commonIndex = combined.indexOf("공통 지침");
		int featureIndex = combined.indexOf("기능별 지침");
		int reminderIndex = combined.indexOf("공통 지침(금지 원칙과 안전 규칙)을 우선한다");
		int preferenceIndex = combined.indexOf("말투 선호: DIRECT");
		assertTrue(commonIndex < featureIndex);
		assertTrue(featureIndex < reminderIndex);
		assertTrue(reminderIndex < preferenceIndex);
	}

	@Test
	void combinedInstructions_withoutPreference_omitsPreferenceSection() {
		AssembledPrompt prompt = new AssembledPrompt(
				"common-v1", "공통 지침", "feature-v1", "기능별 지침", "", "본문", "schema-v1");

		String combined = prompt.combinedInstructions();

		assertFalse(combined.contains("회원 선호도를 참고"));
	}

	@Test
	void combinedInstructions_blankPreference_treatedAsAbsent() {
		AssembledPrompt withNull = new AssembledPrompt(
				"common-v1", "공통 지침", "feature-v1", "기능별 지침", null, "본문", "schema-v1");
		AssembledPrompt withBlank = new AssembledPrompt(
				"common-v1", "공통 지침", "feature-v1", "기능별 지침", "   ", "본문", "schema-v1");

		assertEquals(withNull.combinedInstructions().contains("회원 선호도를 참고"), false);
		assertEquals(withBlank.combinedInstructions().contains("회원 선호도를 참고"), false);
	}

}
