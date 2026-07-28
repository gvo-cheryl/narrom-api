package com.naroom.api.ai.outcome;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.naroom.api.ai.AiJobService;
import com.naroom.api.ai.domain.entity.AiGenerationRun;
import com.naroom.api.ai.domain.entity.AiJob;
import com.naroom.api.ai.domain.entity.AiPromptVersion;
import com.naroom.api.ai.domain.error.AiErrorCode;
import com.naroom.api.ai.domain.repository.AiGenerationRunRepository;
import com.naroom.api.ai.domain.repository.AiJobRepository;
import com.naroom.api.ai.result.PeriodReflectionResult;
import com.naroom.api.global.error.exception.BusinessException;
import com.naroom.api.lifetime.domain.entity.PeriodReflection;
import com.naroom.api.lifetime.domain.error.LifetimeErrorCode;
import com.naroom.api.lifetime.domain.repository.PeriodReflectionRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;

// 3-C: EntryReflectionOutcomeService와 같은 구조다(판정은 호출자가 이미 수행, 여기서는 저장과 AiJob 상태
// 확정만 담당). PeriodReflection은 요청 시점에 이미 PENDING으로 존재하므로(EntryReflectionOutcomeService의
// AiReflection과 달리 새로 만들지 않고) ID로 조회해 상태만 전이시킨다.
@Service
@Transactional(readOnly = true)
public class PeriodReflectionOutcomeService {

	private final PeriodReflectionRepository periodReflectionRepository;
	private final AiJobRepository aiJobRepository;
	private final AiPromptVersionResolver aiPromptVersionResolver;
	private final AiGenerationRunRepository aiGenerationRunRepository;
	private final AiJobService aiJobService;
	private final ObjectMapper objectMapper = new ObjectMapper();

	public PeriodReflectionOutcomeService(
			PeriodReflectionRepository periodReflectionRepository,
			AiJobRepository aiJobRepository,
			AiPromptVersionResolver aiPromptVersionResolver,
			AiGenerationRunRepository aiGenerationRunRepository,
			AiJobService aiJobService) {
		this.periodReflectionRepository = periodReflectionRepository;
		this.aiJobRepository = aiJobRepository;
		this.aiPromptVersionResolver = aiPromptVersionResolver;
		this.aiGenerationRunRepository = aiGenerationRunRepository;
		this.aiJobService = aiJobService;
	}

	@Transactional
	public PeriodReflectionOutcome persist(PeriodReflectionGenerationContext context) {
		PeriodReflection periodReflection = periodReflectionRepository.findById(context.periodReflectionId())
				.orElseThrow(() -> new BusinessException(LifetimeErrorCode.PERIOD_REFLECTION_NOT_FOUND));
		AiJob aiJob = aiJobRepository.findById(context.aiJobId())
				.orElseThrow(() -> new BusinessException(AiErrorCode.JOB_NOT_FOUND));

		AiPromptVersion commonVersion = aiPromptVersionResolver.getOrCreateCommon(context.commonInstructionsVersion());
		AiPromptVersion featureVersion = aiPromptVersionResolver.getOrCreateFeature(
				aiJob.getFeatureType(), context.featureInstructionsVersion(), context.outputSchemaVersion());

		AiGenerationRun run = aiGenerationRunRepository.save(
				AiGenerationRun.start(aiJob, context.modelName(), commonVersion, featureVersion, context.outputSchemaVersion()));
		run.complete(
				Math.toIntExact(context.generationResult().inputTokens()),
				Math.toIntExact(context.generationResult().outputTokens()),
				context.inputSafetyGrade(),
				context.outputSafetyGrade(),
				context.latencyMs(),
				Instant.now());

		applyOutcome(context, periodReflection, run);

		return new PeriodReflectionOutcome(periodReflection.getId(), run.getId(), context.outputSafetyGrade());
	}

	private void applyOutcome(PeriodReflectionGenerationContext context, PeriodReflection periodReflection, AiGenerationRun run) {
		switch (context.outputSafetyGrade()) {
			case NORMAL -> {
				periodReflection.complete(
						run,
						context.parsedResult().summary(),
						toInsightsJson(context.parsedResult()),
						context.parsedResult().reflectionQuestion(),
						Instant.now());
				aiJobService.completeJob(context.aiJobId(), context.leaseStartedAt());
			}
			case RESTRICTED -> {
				periodReflection.blockAsUnsafe(run, "OUTPUT_RESTRICTED", Instant.now());
				aiJobService.blockJob(context.aiJobId(), context.leaseStartedAt());
			}
			case CRISIS -> {
				periodReflection.markSafetySupport(run, "OUTPUT_CRISIS", Instant.now());
				aiJobService.markJobSafetySupport(context.aiJobId(), context.leaseStartedAt());
			}
			case BLOCKED_OUTPUT -> throw new IllegalArgumentException(
					"outputSafetyGrade must come from a fresh moderation classification (NORMAL/RESTRICTED/CRISIS), not BLOCKED_OUTPUT");
		}
	}

	// L6 화면의 반복된 감정과 상황/어려웠던 순간/감사한 일/시도한 대응/도움이 된 조건을 하나의 jsonb로 묶는다.
	private String toInsightsJson(PeriodReflectionResult result) {
		try {
			return objectMapper.writeValueAsString(new Insights(
					result.repeatedEmotionsAndSituations(),
					result.difficultMoments(),
					result.gratefulMoments(),
					result.triedResponses(),
					result.helpfulConditions()));
		} catch (JsonProcessingException e) {
			throw new IllegalStateException("insights JSON 직렬화에 실패했습니다", e);
		}
	}

	private record Insights(
			List<String> repeatedEmotionsAndSituations,
			List<String> difficultMoments,
			List<String> gratefulMoments,
			List<String> triedResponses,
			List<String> helpfulConditions) {
	}

}
