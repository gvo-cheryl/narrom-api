package com.naroom.api.ai.pipeline;

import com.naroom.api.ai.AiJobService;
import com.naroom.api.ai.AiModerationClient;
import com.naroom.api.ai.AiResponseGenerationClient;
import com.naroom.api.ai.GenerationRequest;
import com.naroom.api.ai.GenerationResult;
import com.naroom.api.ai.domain.entity.AiFeatureType;
import com.naroom.api.ai.domain.entity.AiSafetyGrade;
import com.naroom.api.ai.dto.AiJobResponse;
import com.naroom.api.ai.infra.openai.OpenAiProperties;
import com.naroom.api.ai.outcome.PeriodReflectionGenerationContext;
import com.naroom.api.ai.outcome.PeriodReflectionOutcomeService;
import com.naroom.api.ai.prompt.AiInstructionCatalog;
import com.naroom.api.ai.prompt.AssembledPrompt;
import com.naroom.api.ai.prompt.PromptAssembler;
import com.naroom.api.ai.result.PeriodReflectionResponseParser;
import com.naroom.api.ai.result.PeriodReflectionResult;
import com.naroom.api.ai.result.SummaryOriginalityValidator;
import com.naroom.api.lifetime.PeriodReflectionService;
import com.naroom.api.lifetime.PeriodReflectionService.ProcessingContext;
import com.naroom.api.lifetime.domain.entity.PeriodReflection;
import com.naroom.api.record.domain.entity.Entry;
import com.openai.errors.InternalServerException;
import com.openai.errors.OpenAIIoException;
import com.openai.errors.OpenAIRetryableException;
import com.openai.errors.RateLimitException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

// EntryReflectionJobProcessor와 같은 구조로 3일/주간 회고를 처리한다(§7 전체 처리 흐름). job.entryId()는
// PeriodReflection의 봉투 Entry이므로, 실제 회고 행과 근거 기록은 그 ID로 다시 조회해야 한다(3-C, 이슈 #21).
@Component
public class PeriodReflectionJobProcessor {

	private static final Logger log = LoggerFactory.getLogger(PeriodReflectionJobProcessor.class);

	private final PeriodReflectionService periodReflectionService;
	private final PromptAssembler promptAssembler;
	private final AiModerationClient moderationClient;
	private final AiResponseGenerationClient generationClient;
	private final PeriodReflectionResponseParser responseParser;
	private final PeriodReflectionOutcomeService outcomeService;
	private final AiJobService aiJobService;
	private final OpenAiProperties openAiProperties;

	public PeriodReflectionJobProcessor(
			PeriodReflectionService periodReflectionService,
			PromptAssembler promptAssembler,
			AiModerationClient moderationClient,
			AiResponseGenerationClient generationClient,
			PeriodReflectionResponseParser responseParser,
			PeriodReflectionOutcomeService outcomeService,
			AiJobService aiJobService,
			OpenAiProperties openAiProperties) {
		this.periodReflectionService = periodReflectionService;
		this.promptAssembler = promptAssembler;
		this.moderationClient = moderationClient;
		this.generationClient = generationClient;
		this.responseParser = responseParser;
		this.outcomeService = outcomeService;
		this.aiJobService = aiJobService;
		this.openAiProperties = openAiProperties;
	}

	public void process(AiJobResponse job) {
		if (job.featureType() != AiFeatureType.WEEKLY_REFLECTION && job.featureType() != AiFeatureType.THREE_DAY_REFLECTION) {
			// dispatch()가 featureType으로 이미 걸러 보내므로 실제로는 도달하지 않는 방어 분기다 - 이 경우
			// PeriodReflection 자체가 없을 수 있어 markFailed를 호출하지 않는다.
			aiJobService.failJobPermanently(job.id(), job.startedAt(), "UNSUPPORTED_FEATURE_TYPE");
			return;
		}
		try {
			processPeriodReflection(job);
		} catch (Exception e) {
			handleFailure(job, e);
		}
	}

	// 21.2절: EntryReflectionJobProcessor와 동일한 재시도 분류 기준을 그대로 따른다. 영구 실패이거나(비재시도성)
	// 재시도 가능한 종류라도 이번이 마지막 허용 시도였다면(attempt_count가 max_attempts에 도달) ai_jobs뿐
	// 아니라 period_reflections.status도 FAILED로 맞춰야 한다 - 그렇지 않으면 ai_jobs는 조용히 재시도 불가
	// 상태(attempt_count>=max_attempts)로 멈추는데 period_reflections는 영원히 PENDING으로 남아,
	// generate()가 이를 정상 진행 중인 회고로 오인해 재시도를 만들지 않는다.
	private void handleFailure(AiJobResponse job, Exception e) {
		String errorCode = e.getClass().getSimpleName();
		boolean retryable = e instanceof OpenAIIoException
				|| e instanceof RateLimitException
				|| e instanceof InternalServerException
				|| e instanceof OpenAIRetryableException
				|| e instanceof IllegalArgumentException;
		boolean attemptsExhausted = job.attemptCount() + 1 >= job.maxAttempts();

		log.warn("Period reflection job processing failed. jobId={} errorCode={} retryable={} attemptsExhausted={}",
				job.id(), errorCode, retryable, attemptsExhausted);

		if (retryable && !attemptsExhausted) {
			aiJobService.failJob(job.id(), job.startedAt(), errorCode, computeBackoff(job.attemptCount()));
		} else {
			aiJobService.failJobPermanently(job.id(), job.startedAt(), errorCode);
			periodReflectionService.markFailed(job.entryId(), errorCode);
		}
	}

	private void processPeriodReflection(AiJobResponse job) {
		ProcessingContext processingContext = periodReflectionService.loadForProcessing(job.entryId());
		PeriodReflection periodReflection = processingContext.periodReflection();
		List<Entry> evidenceEntries = processingContext.evidenceEntries();
		Set<UUID> allowedEvidenceEntryIds = evidenceEntries.stream().map(Entry::getId).collect(Collectors.toSet());

		AssembledPrompt prompt = promptAssembler.assembleForPeriodReflection(
				periodReflection.getMember(),
				job.featureType(),
				periodReflection.getPeriodStart(),
				periodReflection.getPeriodEnd(),
				evidenceEntries);

		AiSafetyGrade inputGrade = moderationClient.classify(prompt.contextContent());
		boolean proceed = aiJobService.applyInputSafetyGrade(job.id(), job.startedAt(), inputGrade);
		if (!proceed) {
			return;
		}

		GenerationRequest request = new GenerationRequest(
				prompt.combinedInstructions(),
				prompt.contextContent(),
				maxOutputTokens(job.featureType()),
				prompt.outputSchemaVersion(),
				AiInstructionCatalog.outputSchema(job.featureType()));
		long generationStartedAt = System.currentTimeMillis();
		GenerationResult generationResult = generationClient.generate(request);
		int latencyMs = (int) (System.currentTimeMillis() - generationStartedAt);

		PeriodReflectionResult parsedResult = responseParser.parse(generationResult.outputJson(), allowedEvidenceEntryIds);

		String outputText = String.join("\n",
				parsedResult.summary(),
				String.join("\n", parsedResult.repeatedEmotionsAndSituations()),
				String.join("\n", parsedResult.difficultMoments()),
				String.join("\n", parsedResult.gratefulMoments()),
				String.join("\n", parsedResult.triedResponses()),
				String.join("\n", parsedResult.helpfulConditions()),
				parsedResult.reflectionQuestion());
		AiSafetyGrade outputGrade = moderationClient.classify(outputText);
		// 안전 분류가 항상 먼저 확정돼야 한다(EntryReflectionJobProcessor와 같은 이유). NORMAL로 판정된
		// 응답에만 "따라 읽기" 재검증을 적용한다. 비교 대상도 prompt.contextContent() 전체가 아니라 근거
		// 기록의 원문(evidenceEntries)만 쓴다 - contextContent에는 이전 개별 기록 AI 요약도 섞여 있어서,
		// 그걸 그대로 쓰면 모델이 자신이 이미 만든 AI 요약 문구를 재사용한 것까지 "사용자 원문을 베꼈다"로
		// 오탐할 수 있다.
		if (outputGrade == AiSafetyGrade.NORMAL) {
			String evidenceEntryBodies = evidenceEntries.stream()
					.map(Entry::getBody)
					.filter(Objects::nonNull)
					.collect(Collectors.joining("\n"));
			SummaryOriginalityValidator.validate(evidenceEntryBodies, parsedResult.summary());
		}

		PeriodReflectionGenerationContext context = new PeriodReflectionGenerationContext(
				job.id(),
				job.startedAt(),
				periodReflection.getId(),
				openAiProperties.model(),
				prompt.commonInstructionsVersion(),
				prompt.featureInstructionsVersion(),
				prompt.outputSchemaVersion(),
				inputGrade,
				outputGrade,
				generationResult,
				parsedResult,
				latencyMs);
		outcomeService.persist(context);
	}

	// §5.3 토큰 상한(2026-07-30 상향): 원래 3일 회고 600·주간 회고 900이었으나, 리스트 필드 6개+요약+질문+
	// evidenceEntryIds를 담기엔 부족해 응답이 중간에 잘려(incomplete) 매번 JSON 파싱에 실패했다. 우선 2배까지만
	// 올려두고, 실제 사용량을 보며 다시 조정한다.
	private long maxOutputTokens(AiFeatureType featureType) {
		return switch (featureType) {
			case THREE_DAY_REFLECTION -> 1200L;
			case WEEKLY_REFLECTION -> 1800L;
			default -> throw new IllegalArgumentException(featureType + "은 기간별 회고 대상이 아닙니다");
		};
	}

	private Instant computeBackoff(int attemptCount) {
		long delaySeconds = Math.min(30L * (1L << Math.max(attemptCount, 0)), 600L);
		return Instant.now().plusSeconds(delaySeconds);
	}

}
