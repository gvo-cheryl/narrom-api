package com.naroom.api.experiment.dto;

import java.util.List;

// §11.3 코스 전체 회고. requestAiReflection이 true면서 3일 코스일 때만 THREE_DAY_REFLECTION AI 잡을
// 만든다 - 7일 코스는 Beta 1 정책상 AI 회고 대상이 아니고 사용자의 돌아보기만 저장한다.
public record ExperimentCourseReviewRequest(
		Short mostMemorableDay,
		Short leastBurdensomeDay,
		Short notFitDay,
		List<String> helpfulConditions,
		List<String> difficultConditions,
		String discovery,
		String continueAction,
		String userSummary,
		Boolean requestAiReflection) {

	public ExperimentCourseReviewRequest {
		helpfulConditions = helpfulConditions == null ? List.of() : helpfulConditions;
		difficultConditions = difficultConditions == null ? List.of() : difficultConditions;
		requestAiReflection = requestAiReflection == null ? Boolean.FALSE : requestAiReflection;
	}

}
