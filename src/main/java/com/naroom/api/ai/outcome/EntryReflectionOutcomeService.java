package com.naroom.api.ai.outcome;

import com.naroom.api.ai.AiJobService;
import com.naroom.api.ai.domain.entity.AiFeatureType;
import com.naroom.api.ai.domain.entity.AiGenerationRun;
import com.naroom.api.ai.domain.entity.AiJob;
import com.naroom.api.ai.domain.entity.AiPromptScope;
import com.naroom.api.ai.domain.entity.AiPromptVersion;
import com.naroom.api.ai.domain.entity.AiReflection;
import com.naroom.api.ai.domain.entity.AiSafetyGrade;
import com.naroom.api.ai.domain.error.AiErrorCode;
import com.naroom.api.ai.domain.repository.AiGenerationRunRepository;
import com.naroom.api.ai.domain.repository.AiJobRepository;
import com.naroom.api.ai.domain.repository.AiPromptVersionRepository;
import com.naroom.api.ai.domain.repository.AiReflectionRepository;
import com.naroom.api.global.error.exception.BusinessException;
import com.naroom.api.record.domain.entity.Entry;
import com.naroom.api.record.domain.error.RecordErrorCode;
import com.naroom.api.record.domain.repository.EntryRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

// 4-H: 출력 Moderation 자체(실제 API 호출)는 호출자가 이미 수행해 outputSafetyGrade로 넘겨준다(4-D의
// AiJobService.applyInputSafetyGrade와 같은 방식 - 판정과 저장을 분리). 여기서는 판정 결과를 ai_reflections/
// ai_generation_runs에 저장하고 AiJob 상태를 최종 확정한다.
@Service
@Transactional(readOnly = true)
public class EntryReflectionOutcomeService {

	private final EntryRepository entryRepository;
	private final AiJobRepository aiJobRepository;
	private final AiPromptVersionRepository aiPromptVersionRepository;
	private final AiGenerationRunRepository aiGenerationRunRepository;
	private final AiReflectionRepository aiReflectionRepository;
	private final AiJobService aiJobService;

	public EntryReflectionOutcomeService(
			EntryRepository entryRepository,
			AiJobRepository aiJobRepository,
			AiPromptVersionRepository aiPromptVersionRepository,
			AiGenerationRunRepository aiGenerationRunRepository,
			AiReflectionRepository aiReflectionRepository,
			AiJobService aiJobService) {
		this.entryRepository = entryRepository;
		this.aiJobRepository = aiJobRepository;
		this.aiPromptVersionRepository = aiPromptVersionRepository;
		this.aiGenerationRunRepository = aiGenerationRunRepository;
		this.aiReflectionRepository = aiReflectionRepository;
		this.aiJobService = aiJobService;
	}

	@Transactional
	public EntryReflectionOutcome persist(EntryReflectionGenerationContext context) {
		Entry entry = entryRepository.findById(context.entryId())
				.orElseThrow(() -> new BusinessException(RecordErrorCode.ENTRY_NOT_FOUND));
		AiJob aiJob = aiJobRepository.findById(context.aiJobId())
				.orElseThrow(() -> new BusinessException(AiErrorCode.JOB_NOT_FOUND));

		AiPromptVersion commonVersion = getOrCreateCommonPromptVersion(context.commonInstructionsVersion());
		AiPromptVersion featureVersion = getOrCreateFeaturePromptVersion(
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

		AiReflection reflection = aiReflectionRepository.save(AiReflection.request(entry, context.versionNo()));
		applyOutcome(context, reflection, run);

		return new EntryReflectionOutcome(reflection.getId(), run.getId(), context.outputSafetyGrade());
	}

	private void applyOutcome(EntryReflectionGenerationContext context, AiReflection reflection, AiGenerationRun run) {
		switch (context.outputSafetyGrade()) {
			case NORMAL -> {
				reflection.complete(
						run,
						context.parsedResult().summary(),
						context.parsedResult().reflectionQuestion(),
						context.generationResult().outputJson(),
						null,
						Instant.now());
				aiJobService.completeJob(context.aiJobId(), context.leaseStartedAt());
			}
			case RESTRICTED -> {
				reflection.blockAsUnsafe(run, "OUTPUT_RESTRICTED", Instant.now());
				aiJobService.blockJob(context.aiJobId(), context.leaseStartedAt());
			}
			case CRISIS -> {
				reflection.markSafetySupport(run, "OUTPUT_CRISIS", Instant.now());
				aiJobService.markJobSafetySupport(context.aiJobId(), context.leaseStartedAt());
			}
			case BLOCKED_OUTPUT -> throw new IllegalArgumentException(
					"outputSafetyGrade must come from a fresh moderation classification (NORMAL/RESTRICTED/CRISIS), not BLOCKED_OUTPUT");
		}
	}

	private AiPromptVersion getOrCreateCommonPromptVersion(String versionLabel) {
		return aiPromptVersionRepository.findByScopeAndVersionLabel(AiPromptScope.COMMON, versionLabel)
				.orElseGet(() -> aiPromptVersionRepository.save(AiPromptVersion.forCommon(versionLabel)));
	}

	private AiPromptVersion getOrCreateFeaturePromptVersion(
			AiFeatureType featureType, String versionLabel, String outputSchemaVersion) {
		return aiPromptVersionRepository.findByFeatureTypeAndVersionLabel(featureType, versionLabel)
				.orElseGet(() -> aiPromptVersionRepository.save(
						AiPromptVersion.forFeature(featureType, versionLabel, outputSchemaVersion)));
	}

}
