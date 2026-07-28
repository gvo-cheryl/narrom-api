package com.naroom.api.ai.result;

import com.naroom.api.ai.domain.entity.AiSafetyGrade;

import java.util.List;
import java.util.UUID;

// evidenceEntryIds는 실제로 이 회고에 넘긴 근거 기록 집합과 다시 대조된 것만 남는다(모델이 다른 기간·다른
// 회원의 ID를 지어내거나 착각해 반환해도 걸러진다) - 9.1절 "AI가 반환한 DB ID를 신뢰하지 않고 재검증"을
// 개별 기록 회고보다 한 단계 더 엄격하게 적용한 것이다.
public record PeriodReflectionResult(
		String summary,
		List<String> repeatedEmotionsAndSituations,
		List<String> difficultMoments,
		List<String> gratefulMoments,
		List<String> triedResponses,
		List<String> helpfulConditions,
		String reflectionQuestion,
		List<UUID> evidenceEntryIds,
		AiSafetyGrade modelReportedSafetyStatus) {
}
