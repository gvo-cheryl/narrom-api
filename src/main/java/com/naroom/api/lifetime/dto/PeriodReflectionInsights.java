package com.naroom.api.lifetime.dto;

import java.util.List;

// L6 화면 흐름의 반복된 감정과 상황/어려웠던 순간/감사한 일/시도한 대응/도움이 된 조건을 그대로 옮긴 응답.
// PeriodReflection.insights(jsonb 문자열)를 그대로 파싱한 것이다.
public record PeriodReflectionInsights(
		List<String> repeatedEmotionsAndSituations,
		List<String> difficultMoments,
		List<String> gratefulMoments,
		List<String> triedResponses,
		List<String> helpfulConditions) {
}
