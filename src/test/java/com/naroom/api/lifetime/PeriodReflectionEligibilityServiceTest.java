package com.naroom.api.lifetime;

import com.naroom.api.account.domain.entity.Member;
import com.naroom.api.account.domain.repository.MemberRepository;
import com.naroom.api.ai.domain.entity.AiFeatureType;
import com.naroom.api.global.error.exception.BusinessException;
import com.naroom.api.lifetime.domain.error.LifetimeErrorCode;
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
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest
@Transactional
@DirtiesContext
class PeriodReflectionEligibilityServiceTest {

	@Autowired
	private PeriodReflectionEligibilityService eligibilityService;

	@Autowired
	private EntryRepository entryRepository;

	@Autowired
	private MemberRepository memberRepository;

	@Test
	void selectEvidenceEntriesOrThrow_weeklyWithEnoughRecords_returnsPublishedEntriesOnly() {
		Member member = memberRepository.save(Member.create("지연"));
		LocalDate periodStart = LocalDate.of(2026, 7, 6);
		LocalDate periodEnd = LocalDate.of(2026, 7, 12);
		publishedEntry(member, EntryType.FREE, periodStart);
		publishedEntry(member, EntryType.GRATITUDE, periodStart.plusDays(1));
		publishedEntry(member, EntryType.EMOTION, periodStart.plusDays(2));
		draftEntry(member, EntryType.FREE, periodStart.plusDays(3));

		List<Entry> evidence = eligibilityService.selectEvidenceEntriesOrThrow(
				member.getId(), AiFeatureType.WEEKLY_REFLECTION, periodStart, periodEnd);

		assertEquals(3, evidence.size());
	}

	@Test
	void selectEvidenceEntriesOrThrow_weeklyWithTooFewRecords_throwsInsufficientRecords() {
		Member member = memberRepository.save(Member.create("지연"));
		LocalDate periodStart = LocalDate.of(2026, 7, 6);
		LocalDate periodEnd = LocalDate.of(2026, 7, 12);
		publishedEntry(member, EntryType.FREE, periodStart);
		publishedEntry(member, EntryType.GRATITUDE, periodStart.plusDays(1));

		BusinessException exception = assertThrows(
				BusinessException.class,
				() -> eligibilityService.selectEvidenceEntriesOrThrow(
						member.getId(), AiFeatureType.WEEKLY_REFLECTION, periodStart, periodEnd));
		assertEquals(LifetimeErrorCode.PERIOD_REFLECTION_INSUFFICIENT_RECORDS, exception.errorCode());
	}

	@Test
	void selectEvidenceEntriesOrThrow_threeDayWithOneRecord_succeeds() {
		Member member = memberRepository.save(Member.create("지연"));
		LocalDate periodStart = LocalDate.of(2026, 7, 10);
		LocalDate periodEnd = LocalDate.of(2026, 7, 12);
		publishedEntry(member, EntryType.FREE, periodEnd);

		List<Entry> evidence = eligibilityService.selectEvidenceEntriesOrThrow(
				member.getId(), AiFeatureType.THREE_DAY_REFLECTION, periodStart, periodEnd);

		assertEquals(1, evidence.size());
	}

	@Test
	void selectEvidenceEntriesOrThrow_excludesReflectionAndSummaryEnvelopeTypes() {
		Member member = memberRepository.save(Member.create("지연"));
		LocalDate periodStart = LocalDate.of(2026, 7, 6);
		LocalDate periodEnd = LocalDate.of(2026, 7, 12);
		publishedEntry(member, EntryType.FREE, periodStart);
		publishedEntry(member, EntryType.GRATITUDE, periodStart.plusDays(1));
		publishedEntry(member, EntryType.EMOTION, periodStart.plusDays(2));
		publishedEntry(member, EntryType.WEEKLY_REFLECTION, periodStart.plusDays(3));
		publishedEntry(member, EntryType.THREE_DAY_REFLECTION, periodStart.plusDays(3));
		publishedEntry(member, EntryType.SELF_SUMMARY, periodStart.plusDays(3));

		List<Entry> evidence = eligibilityService.selectEvidenceEntriesOrThrow(
				member.getId(), AiFeatureType.WEEKLY_REFLECTION, periodStart, periodEnd);

		assertEquals(3, evidence.size());
		assertTrue(evidence.stream().noneMatch(entry -> entry.getEntryType() == EntryType.WEEKLY_REFLECTION
				|| entry.getEntryType() == EntryType.THREE_DAY_REFLECTION
				|| entry.getEntryType() == EntryType.SELF_SUMMARY));
	}

	private void publishedEntry(Member member, EntryType entryType, LocalDate recordDate) {
		Entry entry = Entry.create(member, entryType, null, "본문", recordDate, null, null, null);
		entry.publish();
		entryRepository.save(entry);
	}

	private void draftEntry(Member member, EntryType entryType, LocalDate recordDate) {
		entryRepository.save(Entry.create(member, entryType, null, "본문", recordDate, null, null, null));
	}

}
