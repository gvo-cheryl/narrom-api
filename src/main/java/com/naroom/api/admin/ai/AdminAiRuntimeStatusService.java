package com.naroom.api.admin.ai;

import com.naroom.api.admin.ai.dto.AdminAiRuntimeStatusResponse;
import com.naroom.api.ai.domain.entity.AiFeatureType;
import com.naroom.api.ai.domain.entity.AiJobStatus;
import com.naroom.api.ai.domain.entity.AiPromptScope;
import com.naroom.api.ai.domain.entity.AiPromptVersion;
import com.naroom.api.ai.domain.repository.AiGenerationRunMetricsAggregate;
import com.naroom.api.ai.domain.repository.AiGenerationRunRepository;
import com.naroom.api.ai.domain.repository.AiJobRepository;
import com.naroom.api.ai.domain.repository.AiJobStatusAggregate;
import com.naroom.api.ai.domain.repository.AiPromptVersionRepository;
import com.naroom.api.ai.infra.openai.OpenAiProperties;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

// AI 운영 화면의 "런타임 현황"(관리자 웹 스펙 §15.1) 조회 전용 서비스. §14.6 정책상 프롬프트·모델은
// 여전히 코드로 관리하므로, 여기서는 활성 프롬프트 버전 라벨과 ai_jobs/ai_generation_runs 집계만
// 보여준다 - 편집·활성화 기능은 포함하지 않는다.
@Service
@Transactional(readOnly = true)
public class AdminAiRuntimeStatusService {

	private static final int WINDOW_DAYS = 7;

	private final AiPromptVersionRepository aiPromptVersionRepository;
	private final AiJobRepository aiJobRepository;
	private final AiGenerationRunRepository aiGenerationRunRepository;
	private final OpenAiProperties openAiProperties;

	public AdminAiRuntimeStatusService(
			AiPromptVersionRepository aiPromptVersionRepository,
			AiJobRepository aiJobRepository,
			AiGenerationRunRepository aiGenerationRunRepository,
			OpenAiProperties openAiProperties) {
		this.aiPromptVersionRepository = aiPromptVersionRepository;
		this.aiJobRepository = aiJobRepository;
		this.aiGenerationRunRepository = aiGenerationRunRepository;
		this.openAiProperties = openAiProperties;
	}

	public List<AdminAiRuntimeStatusResponse> list() {
		Instant since = Instant.now().minus(WINDOW_DAYS, ChronoUnit.DAYS);
		Map<AiFeatureType, AiJobStatusAggregate> jobAggregates = aiJobRepository
				.aggregateStatusCountsSince(since, AiJobStatus.COMPLETED).stream()
				.collect(Collectors.toMap(AiJobStatusAggregate::getFeatureType, aggregate -> aggregate));
		Map<AiFeatureType, AiGenerationRunMetricsAggregate> runAggregates = aiGenerationRunRepository
				.aggregateMetricsSince(since).stream()
				.collect(Collectors.toMap(AiGenerationRunMetricsAggregate::getFeatureType, aggregate -> aggregate));
		AiPromptVersion latestCommon = latestActive(aiPromptVersionRepository.findByScopeAndActiveTrue(AiPromptScope.COMMON));

		return Arrays.stream(AiFeatureType.values())
				.map(featureType -> build(featureType, latestCommon, jobAggregates.get(featureType), runAggregates.get(featureType)))
				.toList();
	}

	private AdminAiRuntimeStatusResponse build(
			AiFeatureType featureType,
			AiPromptVersion latestCommon,
			AiJobStatusAggregate jobAggregate,
			AiGenerationRunMetricsAggregate runAggregate) {
		AiPromptVersion latestFeature = latestActive(aiPromptVersionRepository.findByFeatureTypeAndActiveTrue(featureType));
		long totalJobCount = jobAggregate != null ? jobAggregate.getTotalCount() : 0;
		long completedJobCount = jobAggregate != null ? jobAggregate.getCompletedCount() : 0;
		Double successRate = totalJobCount > 0 ? (double) completedJobCount / totalJobCount : null;
		return new AdminAiRuntimeStatusResponse(
				featureType,
				openAiProperties.model(),
				latestCommon != null ? latestCommon.getVersionLabel() : null,
				latestCommon != null ? latestCommon.getOutputSchemaVersion() : null,
				latestFeature != null ? latestFeature.getVersionLabel() : null,
				latestFeature != null ? latestFeature.getOutputSchemaVersion() : null,
				WINDOW_DAYS,
				totalJobCount,
				completedJobCount,
				successRate,
				runAggregate != null ? runAggregate.getAvgLatencyMs() : null,
				runAggregate != null ? runAggregate.getAvgInputTokens() : null,
				runAggregate != null ? runAggregate.getAvgOutputTokens() : null);
	}

	// is_active에는 DB 제약이 없어 이론상 여러 row가 동시에 active일 수 있다 - 가장 최근 생성된 것을 현재값으로 본다.
	private AiPromptVersion latestActive(List<AiPromptVersion> versions) {
		return versions.stream().max(Comparator.comparing(AiPromptVersion::getCreatedAt)).orElse(null);
	}

}
