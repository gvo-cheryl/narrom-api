package com.naroom.api.ai.outcome;

import com.naroom.api.ai.AiJobService;
import com.naroom.api.ai.domain.entity.AiGenerationRun;
import com.naroom.api.ai.domain.entity.AiJob;
import com.naroom.api.ai.domain.entity.AiPromptVersion;
import com.naroom.api.ai.domain.entity.AiReflection;
import com.naroom.api.ai.domain.error.AiErrorCode;
import com.naroom.api.ai.domain.repository.AiGenerationRunRepository;
import com.naroom.api.ai.domain.repository.AiJobRepository;
import com.naroom.api.ai.domain.repository.AiReflectionRepository;
import com.naroom.api.ai.result.EmotionCandidateResult;
import com.naroom.api.global.error.exception.BusinessException;
import com.naroom.api.record.domain.entity.Entry;
import com.naroom.api.record.domain.entity.EntryTag;
import com.naroom.api.record.domain.entity.Tag;
import com.naroom.api.record.domain.error.RecordErrorCode;
import com.naroom.api.record.domain.repository.EntryRepository;
import com.naroom.api.record.domain.repository.EntryTagRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;

// 4-H: 출력 Moderation 자체(실제 API 호출)는 호출자가 이미 수행해 outputSafetyGrade로 넘겨준다(4-D의
// AiJobService.applyInputSafetyGrade와 같은 방식 - 판정과 저장을 분리). 여기서는 판정 결과를 ai_reflections/
// ai_generation_runs에 저장하고 AiJob 상태를 최종 확정한다.
@Service
@Transactional(readOnly = true)
public class EntryReflectionOutcomeService {

	private final EntryRepository entryRepository;
	private final AiJobRepository aiJobRepository;
	private final AiPromptVersionResolver aiPromptVersionResolver;
	private final AiGenerationRunRepository aiGenerationRunRepository;
	private final AiReflectionRepository aiReflectionRepository;
	private final AiJobService aiJobService;
	private final EntryTagRepository entryTagRepository;

	public EntryReflectionOutcomeService(
			EntryRepository entryRepository,
			AiJobRepository aiJobRepository,
			AiPromptVersionResolver aiPromptVersionResolver,
			AiGenerationRunRepository aiGenerationRunRepository,
			AiReflectionRepository aiReflectionRepository,
			AiJobService aiJobService,
			EntryTagRepository entryTagRepository) {
		this.entryRepository = entryRepository;
		this.aiJobRepository = aiJobRepository;
		this.aiPromptVersionResolver = aiPromptVersionResolver;
		this.aiGenerationRunRepository = aiGenerationRunRepository;
		this.aiReflectionRepository = aiReflectionRepository;
		this.aiJobService = aiJobService;
		this.entryTagRepository = entryTagRepository;
	}

	@Transactional
	public EntryReflectionOutcome persist(EntryReflectionGenerationContext context) {
		Entry entry = entryRepository.findById(context.entryId())
				.orElseThrow(() -> new BusinessException(RecordErrorCode.ENTRY_NOT_FOUND));
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

		AiReflection reflection = aiReflectionRepository.save(AiReflection.request(entry, context.versionNo()));
		applyOutcome(context, entry, reflection, run);

		return new EntryReflectionOutcome(reflection.getId(), run.getId(), context.outputSafetyGrade());
	}

	private void applyOutcome(EntryReflectionGenerationContext context, Entry entry, AiReflection reflection, AiGenerationRun run) {
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
				persistSuggestedTags(context, entry);
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

	// 5-B(§24.5): 4-G에서 SYSTEM 태그와 매칭된 감정·태그 후보를 실제 entry_tags(SUGGESTED, AI_INFERRED)로
	// 남긴다 - 이걸 남겨야 기존에 있던 확인/거부 API(EntryTagService.confirmTag/rejectTag)가 다룰 대상이 생긴다.
	// 매칭되지 않은 감정 후보(9.3절의 unmapped emotion candidate)는 표준 태그가 아니므로 entry_tags에 붙이지 않는다.
	private void persistSuggestedTags(EntryReflectionGenerationContext context, Entry entry) {
		for (EmotionCandidateResult candidate : context.parsedResult().emotionCandidates()) {
			if (candidate.isMapped()) {
				saveSuggestedTagIfAbsent(entry, candidate.matchedTag(), candidate.confidence());
			}
		}
		for (Tag tag : context.parsedResult().suggestedTags()) {
			saveSuggestedTagIfAbsent(entry, tag, null);
		}
	}

	private void saveSuggestedTagIfAbsent(Entry entry, Tag tag, BigDecimal confidence) {
		if (entryTagRepository.findByEntry_IdAndTag_Id(entry.getId(), tag.getId()).isPresent()) {
			return;
		}
		entryTagRepository.save(EntryTag.suggestByAi(entry, tag, confidence, null, null, null));
	}

}
