package com.naroom.api.ai.outcome;

import com.naroom.api.ai.GenerationResult;
import com.naroom.api.ai.domain.entity.AiSafetyGrade;
import com.naroom.api.ai.result.EntryReflectionResult;

import java.time.Instant;
import java.util.UUID;

// 4-D(입력 Moderation)~4-G(파싱·검증)까지 각 단계의 산출물을 모아 4-H(출력 Moderation+저장)에 넘긴다.
// inputSafetyGrade는 이 시점엔 이미 NORMAL로 확정된 값이다(그렇지 않았다면 생성 단계까지 오지 않았을 것이다) -
// 하드코딩하지 않고 호출자가 그대로 넘겨 AiGenerationRun에 정확히 기록되게 한다.
public record EntryReflectionGenerationContext(
		UUID aiJobId,
		Instant leaseStartedAt,
		UUID entryId,
		int versionNo,
		String modelName,
		String commonInstructionsVersion,
		String featureInstructionsVersion,
		String outputSchemaVersion,
		AiSafetyGrade inputSafetyGrade,
		AiSafetyGrade outputSafetyGrade,
		GenerationResult generationResult,
		EntryReflectionResult parsedResult,
		Integer latencyMs) {
}
