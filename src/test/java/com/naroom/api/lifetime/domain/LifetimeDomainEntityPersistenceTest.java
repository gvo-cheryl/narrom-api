package com.naroom.api.lifetime.domain;

import com.naroom.api.account.domain.entity.Member;
import com.naroom.api.account.domain.repository.MemberRepository;
import com.naroom.api.ai.domain.entity.AiFeatureType;
import com.naroom.api.ai.domain.entity.AiGenerationRun;
import com.naroom.api.ai.domain.entity.AiJob;
import com.naroom.api.ai.domain.entity.AiJobStatus;
import com.naroom.api.ai.domain.entity.AiPromptVersion;
import com.naroom.api.ai.domain.entity.AiSafetyGrade;
import com.naroom.api.ai.domain.repository.AiGenerationRunRepository;
import com.naroom.api.ai.domain.repository.AiJobRepository;
import com.naroom.api.ai.domain.repository.AiPromptVersionRepository;
import com.naroom.api.lifetime.domain.entity.PeriodReflection;
import com.naroom.api.lifetime.domain.entity.PeriodReflectionEntry;
import com.naroom.api.lifetime.domain.entity.PersonalSummary;
import com.naroom.api.lifetime.domain.entity.SummaryScope;
import com.naroom.api.lifetime.domain.repository.PeriodReflectionEntryRepository;
import com.naroom.api.lifetime.domain.repository.PeriodReflectionRepository;
import com.naroom.api.lifetime.domain.repository.PersonalSummaryRepository;
import com.naroom.api.record.domain.entity.Entry;
import com.naroom.api.record.domain.entity.EntryType;
import com.naroom.api.record.domain.repository.EntryRepository;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

/**
 * period_reflections의 복합 UNIQUE(member_id, feature_type, period_start, version_no),
 * period_reflection_entries의 복합 PK(@EmbeddedId), personal_summaries.entry_id UNIQUE처럼
 * 스키마 검증만으로는 확인되지 않는 실제 저장/조회 왕복을 검증한다.
 */
@SpringBootTest
@Transactional
@DirtiesContext
class LifetimeDomainEntityPersistenceTest {

	@Autowired
	private MemberRepository memberRepository;

	@Autowired
	private EntryRepository entryRepository;

	@Autowired
	private AiJobRepository aiJobRepository;

	@Autowired
	private AiGenerationRunRepository aiGenerationRunRepository;

	@Autowired
	private AiPromptVersionRepository aiPromptVersionRepository;

	@Autowired
	private PeriodReflectionRepository periodReflectionRepository;

	@Autowired
	private PeriodReflectionEntryRepository periodReflectionEntryRepository;

	@Autowired
	private PersonalSummaryRepository personalSummaryRepository;

	@Autowired
	private EntityManager entityManager;

	@Test
	void periodReflection_completesAndLinksEvidenceEntries() {
		Member member = memberRepository.save(Member.create("지연"));
		Entry evidenceEntry = entryRepository.save(
				Entry.create(member, EntryType.FREE, null, "본문", LocalDate.now(), null, null, null));
		Entry envelopeEntry = entryRepository.save(
				Entry.create(member, EntryType.WEEKLY_REFLECTION, null, null, LocalDate.now(), null, null, null));
		AiGenerationRun run = createCompletedGenerationRun(member, envelopeEntry, AiFeatureType.WEEKLY_REFLECTION);

		PeriodReflection reflection = periodReflectionRepository.save(PeriodReflection.request(
				member, envelopeEntry, AiFeatureType.WEEKLY_REFLECTION, LocalDate.now().minusDays(6), LocalDate.now()));
		reflection.complete(run, "이번 주 요약", "{\"emotions\":[]}", "질문", Instant.now());

		PeriodReflectionEntry link = periodReflectionEntryRepository.save(
				PeriodReflectionEntry.link(reflection, evidenceEntry, "EMOTION"));

		entityManager.flush();
		entityManager.clear();

		PeriodReflection reloaded = periodReflectionRepository.findById(reflection.getId()).orElseThrow();
		assertEquals(AiJobStatus.COMPLETED, reloaded.getStatus());
		assertEquals("이번 주 요약", reloaded.getSummaryText());
		assertEquals(run.getId(), reloaded.getGenerationRun().getId());
		assertEquals(1, reloaded.getVersionNo());

		List<PeriodReflectionEntry> links = periodReflectionEntryRepository.findByPeriodReflection_Id(reflection.getId());
		assertEquals(1, links.size());
		assertEquals(evidenceEntry.getId(), links.get(0).getEntry().getId());
		assertEquals("EMOTION", links.get(0).getEvidenceRole());
		assertEquals(link.getId(), links.get(0).getId());
	}

	@Test
	void periodReflection_regenerate_incrementsVersionAndKeepsPreviousLink() {
		Member member = memberRepository.save(Member.create("지연"));
		Entry firstEnvelope = entryRepository.save(
				Entry.create(member, EntryType.THREE_DAY_REFLECTION, null, null, LocalDate.now(), null, null, null));
		PeriodReflection first = periodReflectionRepository.save(PeriodReflection.request(
				member, firstEnvelope, AiFeatureType.THREE_DAY_REFLECTION, LocalDate.now().minusDays(2), LocalDate.now()));

		Entry secondEnvelope = entryRepository.save(
				Entry.create(member, EntryType.THREE_DAY_REFLECTION, null, null, LocalDate.now(), null, null, null));
		PeriodReflection second =
				periodReflectionRepository.save(PeriodReflection.regenerate(member, secondEnvelope, first));

		entityManager.flush();
		entityManager.clear();

		PeriodReflection reloaded = periodReflectionRepository.findById(second.getId()).orElseThrow();
		assertEquals(2, reloaded.getVersionNo());
		assertEquals(first.getId(), reloaded.getPreviousReflection().getId());
		assertEquals(first.getPeriodStart(), reloaded.getPeriodStart());
	}

	@Test
	void personalSummary_archive_setsArchivedAt() {
		Member member = memberRepository.save(Member.create("지연"));
		Entry entry = entryRepository.save(
				Entry.create(member, EntryType.SELF_SUMMARY, null, "요즘의 나", LocalDate.now(), null, null, null));

		PersonalSummary summary = personalSummaryRepository.save(
				PersonalSummary.create(member, entry, SummaryScope.CURRENT_SELF, null, null));
		assertNull(summary.getArchivedAt());

		summary.archive(Instant.now());
		entityManager.flush();
		entityManager.clear();

		PersonalSummary reloaded = personalSummaryRepository.findById(summary.getId()).orElseThrow();
		assertNotNull(reloaded.getArchivedAt());
		assertEquals(SummaryScope.CURRENT_SELF, reloaded.getScope());
	}

	private AiGenerationRun createCompletedGenerationRun(Member member, Entry envelopeEntry, AiFeatureType featureType) {
		AiPromptVersion commonPrompt =
				aiPromptVersionRepository.save(AiPromptVersion.forCommon("common-" + System.nanoTime()));
		AiPromptVersion featurePrompt = aiPromptVersionRepository.save(
				AiPromptVersion.forFeature(featureType, "feature-" + System.nanoTime(), "schema-v1"));
		AiJob job = aiJobRepository.save(AiJob.forEntry(member, featureType, envelopeEntry, "idem-" + System.nanoTime()));
		job.markCompleted(Instant.now());
		AiGenerationRun run = aiGenerationRunRepository.save(
				AiGenerationRun.start(job, "gpt-5.6-luna", commonPrompt, featurePrompt, "schema-v1"));
		run.complete(1000, 200, AiSafetyGrade.NORMAL, AiSafetyGrade.NORMAL, 700, Instant.now());
		return run;
	}

}
