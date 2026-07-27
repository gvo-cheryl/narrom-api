package com.naroom.api.ai.pipeline;

import com.naroom.api.ai.AiModerationClient;
import com.naroom.api.ai.domain.entity.AiSafetyGrade;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.List;

// 실제 API를 부르지 않는 테스트 전용 Fake(결정 사항: 일반 테스트는 Fake/Mock 구현을 사용). 여러 값을 넘기면
// 호출 순서대로 반환하고(입력 판정→출력 판정 순서), 값이 하나만 남으면 그 값을 계속 반환한다.
public class FakeAiModerationClient implements AiModerationClient {

	private final Deque<AiSafetyGrade> grades;

	public FakeAiModerationClient(AiSafetyGrade... grades) {
		this.grades = new ArrayDeque<>(List.of(grades));
	}

	@Override
	public AiSafetyGrade classify(String text) {
		return grades.size() > 1 ? grades.pollFirst() : grades.peekFirst();
	}

}
