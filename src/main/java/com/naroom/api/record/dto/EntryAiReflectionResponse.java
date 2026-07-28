package com.naroom.api.record.dto;

import com.naroom.api.ai.domain.entity.AiJob;
import com.naroom.api.ai.domain.entity.AiJobStatus;
import com.naroom.api.ai.domain.entity.AiReflection;

import java.time.Instant;

// status가 null이면 AI 작업이 아직 생성되지 않은 것이다(아주 짧은 커밋 직후 시점이거나, entry.aiProcessingAllowed
// =false라 애초에 생성되지 않은 경우). errorCode 등 내부 실패 원인은 노출하지 않는다 - 21.3절 UX는 "다시 시도"
// 안내로 충분하고, 예외 클래스명 같은 내부 정보를 공개 응답에 담지 않는다.
public record EntryAiReflectionResponse(
		AiJobStatus status,
		String reflectionText,
		String reflectionQuestion,
		Instant completedAt) {

	private static final EntryAiReflectionResponse NOT_REQUESTED = new EntryAiReflectionResponse(null, null, null, null);

	public static EntryAiReflectionResponse notRequested() {
		return NOT_REQUESTED;
	}

	public static EntryAiReflectionResponse from(AiJob job, AiReflection reflection) {
		return new EntryAiReflectionResponse(
				job.getStatus(),
				reflection == null ? null : reflection.getReflectionText(),
				reflection == null ? null : reflection.getQuestionText(),
				reflection == null ? null : reflection.getCompletedAt());
	}

}
