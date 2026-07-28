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
import com.naroom.api.global.error.exception.BusinessException;
import com.naroom.api.lifetime.domain.entity.PeriodReflection;
import com.naroom.api.lifetime.domain.entity.PeriodReflectionEntry;
import com.naroom.api.lifetime.domain.error.LifetimeErrorCode;
import com.naroom.api.lifetime.domain.repository.PeriodReflectionEntryRepository;
import com.naroom.api.lifetime.domain.repository.PeriodReflectionRepository;
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
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

// EntryReflectionJobProcessor와 같은 구조로 3일/주간 회고를 처리한다(§7 전체 처리 흐름). job.entryId()는
// PeriodReflection의 봉투 Entry이므로, 실제 회고 행과 근거 기록은 그 ID로 다시 조회해야 한다(3-C, 이슈 #21).
@Component
public class PeriodReflectionJobProcessor {

	private static final Logger log = LoggerFactory.getLogger(PeriodReflectionJobProcessor.class);

	private final PeriodReflectionRepository periodReflectionRepository;
	private final PeriodReflectionEntryRepository periodReflectionEntryRepository;
	private final PromptAssembler promptAssembler;
	private final AiModerationClient moderationClient;
	private final AiResponseGenerationClient generationClient;
	private final PeriodReflectionResponseParser responseParser;
	private final PeriodReflectionOutcomeService outcomeService;
	private final AiJobService aiJobService;
	private final OpenAiProperties openAiProperties;

	public PeriodReflectionJobProcessor(
			PeriodReflectionRepository periodReflectionRepository,
			PeriodReflectionEntryRepository periodReflectionEntryRepository,
			PromptAssembler promptAssembler,
			AiModerationClient moderationClient,
			AiResponseGenerationClient generationClient,
			PeriodReflectionResponseParser responseParser,
			PeriodReflectionOutcomeService outcomeService,
			AiJobService aiJobService,
			OpenAiProperties openAiProperties) {
		this.periodReflectionRepository = periodReflectionRepository;
		this.periodReflectionEntryRepository = periodReflectionEntryRepository;
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
			aiJobService.failJobPermanently(job.id(), job.startedAt(), "UNSUPPORTED_FEATURE_TYPE");
			return;
		}
		try {
			processPeriodReflection(job);
		} catch (Exception e) {
			handleFailure(job, e);
		}
	}

	private void processPeriodReflection(AiJobResponse job) {
		PeriodReflection periodReflection = periodReflectionRepository.findByEntry_Id(job.entryId())
				.orElseThrow(() -> new BusinessException(LifetimeErrorCode.PERIOD_REFLECTION_NOT_FOUND));
		List<Entry> evidenceEntries = periodReflectionEntryRepository.findByPeriodReflection_Id(periodReflection.getId()).stream()
				.map(PeriodReflectionEntry::getEntry)
				.toList();
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

	// §5.3 토큰 상한: 3일 회고 600, 주간 회고 900.
	private long maxOutputTokens(AiFeatureType featureType) {
		return switch (featureType) {
			case THREE_DAY_REFLECTION -> 600L;
			case WEEKLY_REFLECTION -> 900L;
			default -> throw new IllegalArgumentException(featureType + "은 기간별 회고 대상이 아닙니다");
		};
	}

	// 21.2절: EntryReflectionJobProcessor와 동일한 재시도 분류 기준을 그대로 따른다.
	private void handleFailure(AiJobResponse job, Exception e) {
		String errorCode = e.getClass().getSimpleName();
		boolean retryable = e instanceof OpenAIIoException
				|| e instanceof RateLimitException
				|| e instanceof InternalServerException
				|| e instanceof OpenAIRetryableException
				|| e instanceof IllegalArgumentException;

		log.warn("Period reflection job processing failed. jobId={} errorCode={} retryable={}", job.id(), errorCode, retryable);

		if (retryable) {
			aiJobService.failJob(job.id(), job.startedAt(), errorCode, computeBackoff(job.attemptCount()));
		} else {
			aiJobService.failJobPermanently(job.id(), job.startedAt(), errorCode);
		}
	}

	private Instant computeBackoff(int attemptCount) {
		long delaySeconds = Math.min(30L * (1L << Math.max(attemptCount, 0)), 600L);
		return Instant.now().plusSeconds(delaySeconds);
	}

}
