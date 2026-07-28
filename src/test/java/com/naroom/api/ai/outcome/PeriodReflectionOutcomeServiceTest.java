package com.naroom.api.ai.outcome;

import com.naroom.api.account.domain.entity.Member;
import com.naroom.api.account.domain.repository.MemberRepository;
import com.naroom.api.ai.AiJobService;
import com.naroom.api.ai.GenerationResult;
import com.naroom.api.ai.domain.entity.AiFeatureType;
import com.naroom.api.ai.domain.entity.AiJobStatus;
import com.naroom.api.ai.domain.entity.AiSafetyGrade;
import com.naroom.api.ai.dto.AiJobResponse;
import com.naroom.api.ai.result.PeriodReflectionResult;
import com.naroom.api.lifetime.domain.entity.PeriodReflection;
import com.naroom.api.lifetime.domain.repository.PeriodReflectionRepository;
import com.naroom.api.record.domain.entity.Entry;
import com.naroom.api.record.domain.entity.EntryType;
import com.naroom.api.record.domain.repository.EntryRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest
@Transactional
@DirtiesContext
class PeriodReflectionOutcomeServiceTest {

	@Autowired
	private PeriodReflectionOutcomeService outcomeService;

	@Autowired
	private AiJobService aiJobService;

	@Autowired
	private EntryRepository entryRepository;

	@Autowired
	private MemberRepository memberRepository;

	@Autowired
	private PeriodReflectionRepository periodReflectionRepository;

	@Test
	void persist_normalOutput_completesReflectionAndJob() {
		Member member = memberRepository.save(Member.create("지연"));
		Entry envelope = envelopeEntry(member);
		aiJobService.createForEntry(member.getId(), AiFeatureType.WEEKLY_REFLECTION, envelope.getId(), "key-" + System.nanoTime());
		AiJobResponse claimed = aiJobService.claimNextBatch(10).get(0);
		PeriodReflection reflection = periodReflectionRepository.save(PeriodReflection.request(
				member, envelope, AiFeatureType.WEEKLY_REFLECTION, LocalDate.now().minusDays(6), LocalDate.now()));
		PeriodReflectionResult parsedResult = new PeriodReflectionResult(
				"이번 주 요약", List.of("서운함"), List.of(), List.of(), List.of(), List.of(),
				"질문", List.of(), AiSafetyGrade.NORMAL);
		PeriodReflectionGenerationContext context = context(claimed, reflection, AiSafetyGrade.NORMAL, parsedResult);

		PeriodReflectionOutcome outcome = outcomeService.persist(context);

		assertEquals(AiSafetyGrade.NORMAL, outcome.outputSafetyGrade());
		PeriodReflection reloaded = periodReflectionRepository.findById(reflection.getId()).orElseThrow();
		assertEquals(AiJobStatus.COMPLETED, reloaded.getStatus());
		assertEquals("이번 주 요약", reloaded.getSummaryText());
		assertTrue(reloaded.getInsights().contains("서운함"));
		assertNotNull(reloaded.getGenerationRun());
		assertEquals(AiJobStatus.COMPLETED, aiJobService.getJob(member.getId(), claimed.id()).status());
	}

	@Test
	void persist_restrictedOutput_blocksReflectionWithoutStoringText() {
		Member member = memberRepository.save(Member.create("지연"));
		Entry envelope = envelopeEntry(member);
		aiJobService.createForEntry(member.getId(), AiFeatureType.WEEKLY_REFLECTION, envelope.getId(), "key-" + System.nanoTime());
		AiJobResponse claimed = aiJobService.claimNextBatch(10).get(0);
		PeriodReflection reflection = periodReflectionRepository.save(PeriodReflection.request(
				member, envelope, AiFeatureType.WEEKLY_REFLECTION, LocalDate.now().minusDays(6), LocalDate.now()));
		PeriodReflectionResult parsedResult = emptyResult(AiSafetyGrade.RESTRICTED);
		PeriodReflectionGenerationContext context = context(claimed, reflection, AiSafetyGrade.RESTRICTED, parsedResult);

		outcomeService.persist(context);

		PeriodReflection reloaded = periodReflectionRepository.findById(reflection.getId()).orElseThrow();
		assertEquals(AiJobStatus.BLOCKED, reloaded.getStatus());
		assertNull(reloaded.getSummaryText());
		assertEquals("OUTPUT_RESTRICTED", reloaded.getSafetyCode());
		assertEquals(AiJobStatus.BLOCKED, aiJobService.getJob(member.getId(), claimed.id()).status());
	}

	@Test
	void persist_crisisOutput_marksSafetySupport() {
		Member member = memberRepository.save(Member.create("지연"));
		Entry envelope = envelopeEntry(member);
		aiJobService.createForEntry(member.getId(), AiFeatureType.WEEKLY_REFLECTION, envelope.getId(), "key-" + System.nanoTime());
		AiJobResponse claimed = aiJobService.claimNextBatch(10).get(0);
		PeriodReflection reflection = periodReflectionRepository.save(PeriodReflection.request(
				member, envelope, AiFeatureType.WEEKLY_REFLECTION, LocalDate.now().minusDays(6), LocalDate.now()));
		PeriodReflectionGenerationContext context = context(claimed, reflection, AiSafetyGrade.CRISIS, emptyResult(AiSafetyGrade.CRISIS));

		outcomeService.persist(context);

		PeriodReflection reloaded = periodReflectionRepository.findById(reflection.getId()).orElseThrow();
		assertEquals(AiJobStatus.SAFETY_SUPPORT, reloaded.getStatus());
		assertEquals("OUTPUT_CRISIS", reloaded.getSafetyCode());
	}

	@Test
	void persist_blockedOutputGrade_throwsIllegalArgumentException() {
		Member member = memberRepository.save(Member.create("지연"));
		Entry envelope = envelopeEntry(member);
		aiJobService.createForEntry(member.getId(), AiFeatureType.WEEKLY_REFLECTION, envelope.getId(), "key-" + System.nanoTime());
		AiJobResponse claimed = aiJobService.claimNextBatch(10).get(0);
		PeriodReflection reflection = periodReflectionRepository.save(PeriodReflection.request(
				member, envelope, AiFeatureType.WEEKLY_REFLECTION, LocalDate.now().minusDays(6), LocalDate.now()));
		PeriodReflectionGenerationContext context =
				context(claimed, reflection, AiSafetyGrade.BLOCKED_OUTPUT, emptyResult(null));

		assertThrows(IllegalArgumentException.class, () -> outcomeService.persist(context));
	}

	private Entry envelopeEntry(Member member) {
		Entry entry = Entry.create(member, EntryType.WEEKLY_REFLECTION, null, null, LocalDate.now(), null, null, null);
		entry.publish();
		return entryRepository.save(entry);
	}

	private PeriodReflectionResult emptyResult(AiSafetyGrade safetyGrade) {
		return new PeriodReflectionResult(
				"요약", List.of(), List.of(), List.of(), List.of(), List.of(), "질문", List.of(), safetyGrade);
	}

	private PeriodReflectionGenerationContext context(
			AiJobResponse claimed, PeriodReflection reflection, AiSafetyGrade outputSafetyGrade, PeriodReflectionResult parsedResult) {
		GenerationResult generationResult = new GenerationResult("{}", 500, 200);
		return new PeriodReflectionGenerationContext(
				claimed.id(),
				claimed.startedAt(),
				reflection.getId(),
				"gpt-5.6-luna",
				"v-" + System.nanoTime() + "-common",
				"v-feature",
				"v-schema",
				AiSafetyGrade.NORMAL,
				outputSafetyGrade,
				generationResult,
				parsedResult,
				900);
	}

}
