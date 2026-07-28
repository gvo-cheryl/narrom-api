package com.naroom.api.ai.outcome;

import com.naroom.api.ai.GenerationResult;
import com.naroom.api.ai.domain.entity.AiSafetyGrade;
import com.naroom.api.ai.result.PeriodReflectionResult;

import java.time.Instant;
import java.util.UUID;

// entryId/versionNo가 없다: PeriodReflection은 EntryReflectionGenerationContext의 AiReflection과 달리
// 요청 시점(PeriodReflectionService.generate())에 이미 PENDING 행으로 만들어져 있고 근거 기록도 그때
// 연결된다. 이 컨텍스트는 그 기존 행을 완료 처리하는 데만 쓰인다(periodReflectionId로 조회).
public record PeriodReflectionGenerationContext(
		UUID aiJobId,
		Instant leaseStartedAt,
		UUID periodReflectionId,
		String modelName,
		String commonInstructionsVersion,
		String featureInstructionsVersion,
		String outputSchemaVersion,
		AiSafetyGrade inputSafetyGrade,
		AiSafetyGrade outputSafetyGrade,
		GenerationResult generationResult,
		PeriodReflectionResult parsedResult,
		Integer latencyMs) {
}
