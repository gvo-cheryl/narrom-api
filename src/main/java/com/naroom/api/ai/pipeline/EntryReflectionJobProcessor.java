package com.naroom.api.ai.pipeline;

import com.naroom.api.ai.AiJobService;
import com.naroom.api.ai.AiModerationClient;
import com.naroom.api.ai.AiResponseGenerationClient;
import com.naroom.api.ai.GenerationRequest;
import com.naroom.api.ai.GenerationResult;
import com.naroom.api.ai.domain.entity.AiFeatureType;
import com.naroom.api.ai.domain.entity.AiSafetyGrade;
import com.naroom.api.ai.dto.AiJobResponse;
import com.naroom.api.ai.outcome.EntryReflectionGenerationContext;
import com.naroom.api.ai.outcome.EntryReflectionOutcomeService;
import com.naroom.api.ai.prompt.AiInstructionCatalog;
import com.naroom.api.ai.prompt.AssembledPrompt;
import com.naroom.api.ai.prompt.PromptAssembler;
import com.naroom.api.ai.result.EntryReflectionResponseParser;
import com.naroom.api.ai.result.EntryReflectionResult;
import com.naroom.api.ai.result.SummaryOriginalityValidator;
import com.openai.errors.InternalServerException;
import com.openai.errors.OpenAIIoException;
import com.openai.errors.OpenAIRetryableException;
import com.openai.errors.RateLimitException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.time.Instant;

// 4-D~4-H를 한 번의 claim된 작업 처리로 잇는다(§7 전체 처리 흐름). 각 단계는 이미 개별적으로 구현·테스트된
// 조각(PromptAssembler/AiModerationClient/AiResponseGenerationClient/EntryReflectionResponseParser/
// EntryReflectionOutcomeService)을 그대로 호출만 한다 - 새 비즈니스 로직은 여기서 만들지 않는다.
@Component
public class EntryReflectionJobProcessor {

	// 5.3절: 개별 기록 AI 정리의 최대 출력 토큰.
	private static final long MAX_OUTPUT_TOKENS = 350;

	private static final Logger log = LoggerFactory.getLogger(EntryReflectionJobProcessor.class);

	private final PromptAssembler promptAssembler;
	private final AiModerationClient moderationClient;
	private final AiResponseGenerationClient generationClient;
	private final EntryReflectionResponseParser responseParser;
	private final EntryReflectionOutcomeService outcomeService;
	private final AiJobService aiJobService;

	public EntryReflectionJobProcessor(
			PromptAssembler promptAssembler,
			AiModerationClient moderationClient,
			AiResponseGenerationClient generationClient,
			EntryReflectionResponseParser responseParser,
			EntryReflectionOutcomeService outcomeService,
			AiJobService aiJobService) {
		this.promptAssembler = promptAssembler;
		this.moderationClient = moderationClient;
		this.generationClient = generationClient;
		this.responseParser = responseParser;
		this.outcomeService = outcomeService;
		this.aiJobService = aiJobService;
	}

	public void process(AiJobResponse job) {
		if (job.featureType() != AiFeatureType.ENTRY_REFLECTION) {
			// 3일/주간 회고·후속 대화는 아직 파이프라인이 없다. 재시도해도 달라지지 않으므로 영구 실패로 종결한다.
			aiJobService.failJobPermanently(job.id(), job.startedAt(), "UNSUPPORTED_FEATURE_TYPE");
			return;
		}
		try {
			processEntryReflection(job);
		} catch (Exception e) {
			handleFailure(job, e);
		}
	}

	private void processEntryReflection(AiJobResponse job) {
		AssembledPrompt prompt = promptAssembler.assembleForEntryReflection(job.entryId());

		// 8장: 입력 Moderation. 5.2절대로 회원 선호도·현재 맥락까지 조립된 하나의 텍스트를 한 번에 검사한다.
		AiSafetyGrade inputGrade = moderationClient.classify(prompt.contextContent());
		boolean proceed = aiJobService.applyInputSafetyGrade(job.id(), job.startedAt(), inputGrade);
		if (!proceed) {
			return;
		}

		GenerationRequest request = new GenerationRequest(
				prompt.combinedInstructions(),
				prompt.contextContent(),
				MAX_OUTPUT_TOKENS,
				prompt.outputSchemaVersion(),
				AiInstructionCatalog.outputSchema(AiFeatureType.ENTRY_REFLECTION, prompt.outputMaxLength()));
		long generationStartedAt = System.currentTimeMillis();
		GenerationResult generationResult = generationClient.generate(request);
		int latencyMs = (int) (System.currentTimeMillis() - generationStartedAt);

		EntryReflectionResult parsedResult = responseParser.parse(generationResult.outputJson(), job.memberId());

		// 8장: 출력 Moderation. 사용자에게 실제로 보여줄 자연어(summary+reflectionQuestion)만 검사한다.
		String outputText = parsedResult.summary() + "\n" + parsedResult.reflectionQuestion();
		AiSafetyGrade outputGrade = moderationClient.classify(outputText);
		// 안전 분류가 항상 먼저 확정돼야 한다 - "따라 읽기" 재검증을 먼저 하면 위기·제한 상태를 걸러야 할
		// 응답이 CRISIS/RESTRICTED 라우팅(EntryReflectionOutcomeService)에 닿기도 전에 예외로 재시도되어
		// 버려질 수 있다. NORMAL로 판정된 응답에만 이 품질 재검증을 적용한다.
		if (outputGrade == AiSafetyGrade.NORMAL) {
			SummaryOriginalityValidator.validate(prompt.contextContent(), parsedResult.summary());
		}

		EntryReflectionGenerationContext context = new EntryReflectionGenerationContext(
				job.id(),
				job.startedAt(),
				job.entryId(),
				1, // TODO: 4-J/재생성 기능이 붙으면 실제 회원 재생성 횟수 기준 버전 번호로 교체한다.
				prompt.modelName(),
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

	// 21.2절: 네트워크 오류·일시적 서버 오류만 자동 재시도한다. 잘못된 입력·권한 오류·정책 차단은 재시도하지 않는다.
	// IllegalArgumentException은 EntryReflectionResponseParser의 스키마 파싱 실패(21.1절 "출력 스키마 실패:
	// 제한된 재시도 후 FAILED")에 해당해 재시도 대상에 포함한다.
	private void handleFailure(AiJobResponse job, Exception e) {
		String errorCode = e.getClass().getSimpleName();
		boolean retryable = e instanceof OpenAIIoException
				|| e instanceof RateLimitException
				|| e instanceof InternalServerException
				|| e instanceof OpenAIRetryableException
				|| e instanceof IllegalArgumentException;

		// 21.2절: 실패 원문이나 전체 AI 응답을 로그에 남기지 않는다 - 예외 메시지·스택트레이스는 넘기지 않고
		// 작업 ID·에러코드·재시도 가능 여부만 남긴다.
		log.warn("AI job processing failed. jobId={} errorCode={} retryable={}", job.id(), errorCode, retryable);

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
