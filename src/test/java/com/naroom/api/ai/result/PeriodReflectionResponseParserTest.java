package com.naroom.api.ai.result;

import com.naroom.api.ai.domain.entity.AiSafetyGrade;
import org.junit.jupiter.api.Test;

import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PeriodReflectionResponseParserTest {

	private final PeriodReflectionResponseParser parser = new PeriodReflectionResponseParser();

	@Test
	void parse_validJson_returnsResult() {
		UUID allowedEntryId = UUID.randomUUID();
		String json = """
				{
				  "summary": "이번 주 요약",
				  "repeatedEmotionsAndSituations": ["서운함"],
				  "difficultMoments": ["회의가 길었던 날"],
				  "gratefulMoments": ["동료의 도움"],
				  "triedResponses": ["산책"],
				  "helpfulConditions": ["충분한 수면"],
				  "reflectionQuestion": "이번 주 무엇이 가장 힘들었나요?",
				  "evidenceEntryIds": ["%s"],
				  "safetyStatus": "NORMAL"
				}
				""".formatted(allowedEntryId);

		PeriodReflectionResult result = parser.parse(json, Set.of(allowedEntryId));

		assertEquals("이번 주 요약", result.summary());
		assertEquals(1, result.repeatedEmotionsAndSituations().size());
		assertEquals("이번 주 무엇이 가장 힘들었나요?", result.reflectionQuestion());
		assertEquals(1, result.evidenceEntryIds().size());
		assertEquals(allowedEntryId, result.evidenceEntryIds().get(0));
		assertEquals(AiSafetyGrade.NORMAL, result.modelReportedSafetyStatus());
	}

	@Test
	void parse_evidenceEntryIdNotInAllowedSet_isExcluded() {
		UUID allowedEntryId = UUID.randomUUID();
		UUID otherEntryId = UUID.randomUUID();
		String json = """
				{
				  "summary": "요약",
				  "repeatedEmotionsAndSituations": [],
				  "difficultMoments": [],
				  "gratefulMoments": [],
				  "triedResponses": [],
				  "helpfulConditions": [],
				  "reflectionQuestion": "질문",
				  "evidenceEntryIds": ["%s", "%s"],
				  "safetyStatus": "NORMAL"
				}
				""".formatted(allowedEntryId, otherEntryId);

		PeriodReflectionResult result = parser.parse(json, Set.of(allowedEntryId));

		assertEquals(1, result.evidenceEntryIds().size());
		assertEquals(allowedEntryId, result.evidenceEntryIds().get(0));
	}

	@Test
	void parse_blankSummary_throwsIllegalArgumentException() {
		String json = """
				{
				  "summary": "",
				  "repeatedEmotionsAndSituations": [],
				  "difficultMoments": [],
				  "gratefulMoments": [],
				  "triedResponses": [],
				  "helpfulConditions": [],
				  "reflectionQuestion": "질문",
				  "evidenceEntryIds": [],
				  "safetyStatus": "NORMAL"
				}
				""";

		assertThrows(IllegalArgumentException.class, () -> parser.parse(json, Set.of()));
	}

	@Test
	void parse_invalidJson_throwsIllegalArgumentException() {
		assertThrows(IllegalArgumentException.class, () -> parser.parse("not json", Set.of()));
	}

	@Test
	void parse_unknownSafetyStatus_returnsNull() {
		String json = """
				{
				  "summary": "요약",
				  "repeatedEmotionsAndSituations": [],
				  "difficultMoments": [],
				  "gratefulMoments": [],
				  "triedResponses": [],
				  "helpfulConditions": [],
				  "reflectionQuestion": "질문",
				  "evidenceEntryIds": [],
				  "safetyStatus": "UNKNOWN_VALUE"
				}
				""";

		PeriodReflectionResult result = parser.parse(json, Set.of());

		assertTrue(result.modelReportedSafetyStatus() == null);
	}

}
