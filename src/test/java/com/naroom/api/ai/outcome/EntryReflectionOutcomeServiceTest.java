package com.naroom.api.ai.outcome;

import com.naroom.api.account.domain.entity.Member;
import com.naroom.api.account.domain.repository.MemberRepository;
import com.naroom.api.ai.AiJobService;
import com.naroom.api.ai.GenerationResult;
import com.naroom.api.ai.domain.entity.AiFeatureType;
import com.naroom.api.ai.domain.entity.AiJobStatus;
import com.naroom.api.ai.domain.entity.AiReflection;
import com.naroom.api.ai.domain.entity.AiSafetyGrade;
import com.naroom.api.ai.domain.repository.AiGenerationRunRepository;
import com.naroom.api.ai.domain.repository.AiPromptVersionRepository;
import com.naroom.api.ai.domain.repository.AiReflectionRepository;
import com.naroom.api.ai.dto.AiJobResponse;
import com.naroom.api.ai.result.EntryReflectionResult;
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

@SpringBootTest
@Transactional
@DirtiesContext
class EntryReflectionOutcomeServiceTest {

	@Autowired
	private EntryReflectionOutcomeService outcomeService;

	@Autowired
	private AiJobService aiJobService;

	@Autowired
	private EntryRepository entryRepository;

	@Autowired
	private MemberRepository memberRepository;

	@Autowired
	private AiReflectionRepository aiReflectionRepository;

	@Autowired
	private AiGenerationRunRepository aiGenerationRunRepository;

	@Autowired
	private AiPromptVersionRepository aiPromptVersionRepository;

	@Test
	void persist_normalOutput_completesReflectionAndJob() {
		Member member = memberRepository.save(Member.create("지연"));
		Entry entry = entryRepository.save(
				Entry.create(member, EntryType.FREE, null, "본문", LocalDate.now(), null, null, null));
		aiJobService.createForEntry(member.getId(), AiFeatureType.ENTRY_REFLECTION, entry.getId(), "key-" + System.nanoTime());
		AiJobResponse claimed = aiJobService.claimNextBatch(10).get(0);
		EntryReflectionGenerationContext context = context(claimed, entry, AiSafetyGrade.NORMAL, "normal-" + System.nanoTime());

		EntryReflectionOutcome outcome = outcomeService.persist(context);

		assertEquals(AiSafetyGrade.NORMAL, outcome.outputSafetyGrade());
		AiReflection reflection = aiReflectionRepository.findById(outcome.reflectionId()).orElseThrow();
		assertEquals(AiJobStatus.COMPLETED, reflection.getStatus());
		assertEquals("요약", reflection.getReflectionText());
		assertEquals("질문", reflection.getQuestionText());
		assertNotNull(reflection.getGenerationRun());
		assertEquals(AiJobStatus.COMPLETED, aiJobService.getJob(member.getId(), claimed.id()).status());
	}

	@Test
	void persist_restrictedOutput_blocksReflectionAndJobWithoutStoringText() {
		Member member = memberRepository.save(Member.create("지연"));
		Entry entry = entryRepository.save(
				Entry.create(member, EntryType.FREE, null, "본문", LocalDate.now(), null, null, null));
		aiJobService.createForEntry(member.getId(), AiFeatureType.ENTRY_REFLECTION, entry.getId(), "key-" + System.nanoTime());
		AiJobResponse claimed = aiJobService.claimNextBatch(10).get(0);
		EntryReflectionGenerationContext context = context(claimed, entry, AiSafetyGrade.RESTRICTED, "restricted-" + System.nanoTime());

		EntryReflectionOutcome outcome = outcomeService.persist(context);

		AiReflection reflection = aiReflectionRepository.findById(outcome.reflectionId()).orElseThrow();
		assertEquals(AiJobStatus.BLOCKED, reflection.getStatus());
		assertNull(reflection.getReflectionText());
		assertEquals("OUTPUT_RESTRICTED", reflection.getSafetyCode());
		assertEquals(AiJobStatus.BLOCKED, aiJobService.getJob(member.getId(), claimed.id()).status());
	}

	@Test
	void persist_crisisOutput_marksSafetySupport() {
		Member member = memberRepository.save(Member.create("지연"));
		Entry entry = entryRepository.save(
				Entry.create(member, EntryType.FREE, null, "본문", LocalDate.now(), null, null, null));
		aiJobService.createForEntry(member.getId(), AiFeatureType.ENTRY_REFLECTION, entry.getId(), "key-" + System.nanoTime());
		AiJobResponse claimed = aiJobService.claimNextBatch(10).get(0);
		EntryReflectionGenerationContext context = context(claimed, entry, AiSafetyGrade.CRISIS, "crisis-" + System.nanoTime());

		EntryReflectionOutcome outcome = outcomeService.persist(context);

		AiReflection reflection = aiReflectionRepository.findById(outcome.reflectionId()).orElseThrow();
		assertEquals(AiJobStatus.SAFETY_SUPPORT, reflection.getStatus());
		assertEquals("OUTPUT_CRISIS", reflection.getSafetyCode());
		assertEquals(AiJobStatus.SAFETY_SUPPORT, aiJobService.getJob(member.getId(), claimed.id()).status());
	}

	@Test
	void persist_blockedOutputGrade_throwsIllegalArgumentException() {
		Member member = memberRepository.save(Member.create("지연"));
		Entry entry = entryRepository.save(
				Entry.create(member, EntryType.FREE, null, "본문", LocalDate.now(), null, null, null));
		aiJobService.createForEntry(member.getId(), AiFeatureType.ENTRY_REFLECTION, entry.getId(), "key-" + System.nanoTime());
		AiJobResponse claimed = aiJobService.claimNextBatch(10).get(0);
		EntryReflectionGenerationContext context = context(claimed, entry, AiSafetyGrade.BLOCKED_OUTPUT, "blocked-" + System.nanoTime());

		assertThrows(IllegalArgumentException.class, () -> outcomeService.persist(context));
	}

	@Test
	void persist_reusesExistingPromptVersionRow_onSecondCall() {
		Member member = memberRepository.save(Member.create("지연"));
		Entry entryA = entryRepository.save(
				Entry.create(member, EntryType.FREE, null, "본문A", LocalDate.now(), null, null, null));
		Entry entryB = entryRepository.save(
				Entry.create(member, EntryType.FREE, null, "본문B", LocalDate.now(), null, null, null));
		aiJobService.createForEntry(member.getId(), AiFeatureType.ENTRY_REFLECTION, entryA.getId(), "key-a-" + System.nanoTime());
		aiJobService.createForEntry(member.getId(), AiFeatureType.ENTRY_REFLECTION, entryB.getId(), "key-b-" + System.nanoTime());
		List<AiJobResponse> claimed = aiJobService.claimNextBatch(10);
		String versionLabel = "shared-version-" + System.nanoTime();

		outcomeService.persist(context(claimed.get(0), entryA, AiSafetyGrade.NORMAL, versionLabel));
		outcomeService.persist(context(claimed.get(1), entryB, AiSafetyGrade.NORMAL, versionLabel));

		long commonVersionCount = aiPromptVersionRepository.findAll().stream()
				.filter(version -> version.getVersionLabel().equals(versionLabel + "-common"))
				.count();
		assertEquals(1, commonVersionCount);
	}

	private EntryReflectionGenerationContext context(AiJobResponse claimed, Entry entry, AiSafetyGrade outputSafetyGrade, String versionSuffix) {
		EntryReflectionResult parsedResult = new EntryReflectionResult(
				"요약", List.of(), List.of(), List.of(), "질문", List.of(entry.getId()), outputSafetyGrade);
		GenerationResult generationResult = new GenerationResult("{\"summary\":\"요약\"}", 120, 40);
		return new EntryReflectionGenerationContext(
				claimed.id(),
				claimed.startedAt(),
				entry.getId(),
				1,
				"gpt-5.6-luna",
				versionSuffix + "-common",
				versionSuffix + "-feature",
				versionSuffix + "-schema",
				AiSafetyGrade.NORMAL,
				outputSafetyGrade,
				generationResult,
				parsedResult,
				850);
	}

}
