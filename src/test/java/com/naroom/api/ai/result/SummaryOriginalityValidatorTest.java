package com.naroom.api.ai.result;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

class SummaryOriginalityValidatorTest {

	@Test
	void verbatimCopyOfContext_throws() {
		String context = "[본문]\n오늘은 회사에서 팀장님과 의견이 부딪혀서 마음이 많이 답답하고 속상했다";
		String summary = "오늘은 회사에서 팀장님과 의견이 부딪혀서 마음이 많이 답답하고 속상했다";

		assertThrows(IllegalArgumentException.class, () -> SummaryOriginalityValidator.validate(context, summary));
	}

	@Test
	void rephrasedSummary_doesNotThrow() {
		String context = "[본문]\n오늘은 회사에서 팀장님과 의견이 부딪혀서 마음이 많이 답답하고 속상했다";
		String summary = "팀장님과의 의견 차이로 답답한 하루를 보내셨네요. 그 순간 어떤 마음이 가장 컸는지 궁금해요.";

		assertDoesNotThrow(() -> SummaryOriginalityValidator.validate(context, summary));
	}

	@Test
	void shortNaturalOverlap_doesNotThrow() {
		String context = "[본문]\n오늘은 산책을 하며 마음이 편안해졌다";
		String summary = "산책하며 편안해짐";

		assertDoesNotThrow(() -> SummaryOriginalityValidator.validate(context, summary));
	}

	@Test
	void partialVerbatimChunkInLongerSummary_doesNotThrow() {
		// 원문 일부를 인용하더라도, summary 전체 길이 대비 비중이 작고 실제 정리가 뒤에 이어지면 위반이 아니다.
		String context = "[본문]\n오늘은 회사에서 팀장님과 의견이 부딪혀서 마음이 많이 답답하고 속상했다";
		String summary = "회사에서 팀장님과 의견이 부딪혀서 답답하셨군요. 그럼에도 끝까지 자기 생각을 전달하려 했던 점이 눈에 띄어요. "
				+ "다음에 비슷한 상황이 온다면 어떤 방식으로 대화를 이어가고 싶으신가요?";

		assertDoesNotThrow(() -> SummaryOriginalityValidator.validate(context, summary));
	}

	@Test
	void blankSummary_doesNotThrow() {
		assertDoesNotThrow(() -> SummaryOriginalityValidator.validate("[본문]\n오늘 하루", ""));
	}

}
