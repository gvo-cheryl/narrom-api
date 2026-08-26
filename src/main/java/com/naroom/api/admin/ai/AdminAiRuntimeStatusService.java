package com.naroom.api.admin.ai;

import com.naroom.api.admin.ai.dto.AdminAiRuntimeStatusResponse;
import com.naroom.api.ai.domain.entity.AiFeatureType;
import com.naroom.api.ai.domain.entity.AiJobStatus;
import com.naroom.api.ai.domain.repository.AiGenerationRunMetricsAggregate;
import com.naroom.api.ai.domain.repository.AiGenerationRunRepository;
import com.naroom.api.ai.domain.repository.AiJobRepository;
import com.naroom.api.ai.domain.repository.AiJobStatusAggregate;
import com.naroom.api.ai.prompt.AiInstructionCatalog;
import com.naroom.api.ai.prompt.AiPromptResolver;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

// AI 운영 화면의 "런타임 현황"(관리자 웹 스펙 §15.1) 조회 전용 서비스. AiPromptResolver와 같은 로직으로
// 지침 버전·모델·출력 제약을 보여줘서 실제 생성 파이프라인이 지금 쓰는 값과 항상 일치하게 한다.
@Service
@Transactional(readOnly = true)
public class AdminAiRuntimeStatusService {

	private static final int WINDOW_DAYS = 7;

	private final AiJobRepository aiJobRepository;
	private final AiGenerationRunRepository aiGenerationRunRepository;
	private final AiPromptResolver aiPromptResolver;

	public AdminAiRuntimeStatusService(
			AiJobRepository aiJobRepository,
			AiGenerationRunRepository aiGenerationRunRepository,
			AiPromptResolver aiPromptResolver) {
		this.aiJobRepository = aiJobRepository;
		this.aiGenerationRunRepository = aiGenerationRunRepository;
		this.aiPromptResolver = aiPromptResolver;
	}

	public List<AdminAiRuntimeStatusResponse> list() {
		Instant since = Instant.now().minus(WINDOW_DAYS, ChronoUnit.DAYS);
		Map<AiFeatureType, AiJobStatusAggregate> jobAggregates = aiJobRepository
				.aggregateStatusCountsSince(since, AiJobStatus.COMPLETED).stream()
				.collect(Collectors.toMap(AiJobStatusAggregate::getFeatureType, aggregate -> aggregate));
		Map<AiFeatureType, AiGenerationRunMetricsAggregate> runAggregates = aiGenerationRunRepository
				.aggregateMetricsSince(since).stream()
				.collect(Collectors.toMap(AiGenerationRunMetricsAggregate::getFeatureType, aggregate -> aggregate));
		AiPromptResolver.ResolvedCommonInstructions common = aiPromptResolver.resolveCommon();

		return Arrays.stream(AiFeatureType.values())
				.map(featureType -> build(featureType, common, jobAggregates.get(featureType), runAggregates.get(featureType)))
				.toList();
	}

	private AdminAiRuntimeStatusResponse build(
			AiFeatureType featureType,
			AiPromptResolver.ResolvedCommonInstructions common,
			AiJobStatusAggregate jobAggregate,
			AiGenerationRunMetricsAggregate runAggregate) {
		// CONVERSATION_REPLY/CONVERSATION_SUMMARY는 아직 지침 자체가 없다(AiInstructionCatalog 미지원) -
		// 관리자가 아직 아무것도 발행하지 않았다면 빈 값으로 보여준다.
		AiPromptResolver.ResolvedFeatureInstructions feature = resolveFeatureOrNull(featureType);
		long totalJobCount = jobAggregate != null ? jobAggregate.getTotalCount() : 0;
		long completedJobCount = jobAggregate != null ? jobAggregate.getCompletedCount() : 0;
		Double successRate = totalJobCount > 0 ? (double) completedJobCount / totalJobCount : null;
		return new AdminAiRuntimeStatusResponse(
				featureType,
				feature != null ? feature.modelName() : null,
				common.versionLabel(),
				null,
				feature != null ? feature.versionLabel() : null,
				outputSchemaVersionOrNull(featureType),
				feature != null ? feature.outputMaxLength() : null,
				WINDOW_DAYS,
				totalJobCount,
				completedJobCount,
				successRate,
				runAggregate != null ? runAggregate.getAvgLatencyMs() : null,
				runAggregate != null ? runAggregate.getAvgInputTokens() : null,
				runAggregate != null ? runAggregate.getAvgOutputTokens() : null);
	}

	private AiPromptResolver.ResolvedFeatureInstructions resolveFeatureOrNull(AiFeatureType featureType) {
		try {
			return aiPromptResolver.resolveFeature(featureType);
		} catch (UnsupportedOperationException e) {
			return null;
		}
	}

	// 출력 스키마 버전은 항상 코드 정의를 따른다(관리자가 바꿀 수 없음) - CONVERSATION_REPLY/
	// CONVERSATION_SUMMARY처럼 아직 스키마 자체가 없는 기능은 null로 보여준다.
	private String outputSchemaVersionOrNull(AiFeatureType featureType) {
		try {
			return AiInstructionCatalog.outputSchemaVersion(featureType);
		} catch (UnsupportedOperationException e) {
			return null;
		}
	}

}
