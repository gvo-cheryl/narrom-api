package com.naroom.api.lifetime;

import com.naroom.api.account.domain.entity.Member;
import com.naroom.api.account.domain.repository.MemberRepository;
import com.naroom.api.ai.AiJobService;
import com.naroom.api.ai.domain.entity.AiFeatureType;
import com.naroom.api.ai.domain.entity.AiJobStatus;
import com.naroom.api.ai.dto.AiJobResponse;
import com.naroom.api.global.error.exception.BusinessException;
import com.naroom.api.lifetime.domain.entity.PeriodReflection;
import com.naroom.api.lifetime.domain.entity.PeriodReflectionEntry;
import com.naroom.api.lifetime.domain.error.LifetimeErrorCode;
import com.naroom.api.lifetime.domain.repository.PeriodReflectionEntryRepository;
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

@SpringBootTest
@Transactional
@DirtiesContext
class PeriodReflectionServiceTest {

	@Autowired
	private PeriodReflectionService periodReflectionService;

	@Autowired
	private EntryRepository entryRepository;

	@Autowired
	private MemberRepository memberRepository;

	@Autowired
	private PeriodReflectionEntryRepository periodReflectionEntryRepository;

	@Autowired
	private AiJobService aiJobService;

	@Test
	void generate_threeDayWithOneRecord_createsEnvelopeAndLinksEvidenceAndSchedulesJob() {
		Member member = memberRepository.save(Member.create("지연"));
		LocalDate today = LocalDate.now();
		publishedEntry(member, today);

		PeriodReflection reflection = periodReflectionService.generate(member.getId(), AiFeatureType.THREE_DAY_REFLECTION);

		assertEquals(AiJobStatus.PENDING, reflection.getStatus());
		assertEquals(EntryType.THREE_DAY_REFLECTION, reflection.getEntry().getEntryType());
		List<PeriodReflectionEntry> links = periodReflectionEntryRepository.findByPeriodReflection_Id(reflection.getId());
		assertEquals(1, links.size());
		List<AiJobResponse> claimed = aiJobService.claimNextBatch(10);
		assertEquals(1, claimed.size());
		assertEquals(AiFeatureType.THREE_DAY_REFLECTION, claimed.get(0).featureType());
		assertEquals(reflection.getEntry().getId(), claimed.get(0).entryId());
	}

	@Test
	void generate_calledTwiceForSamePeriod_returnsSameReflectionInstead() {
		Member member = memberRepository.save(Member.create("지연"));
		publishedEntry(member, LocalDate.now());

		PeriodReflection first = periodReflectionService.generate(member.getId(), AiFeatureType.THREE_DAY_REFLECTION);
		PeriodReflection second = periodReflectionService.generate(member.getId(), AiFeatureType.THREE_DAY_REFLECTION);

		assertEquals(first.getId(), second.getId());
		assertEquals(1, aiJobService.claimNextBatch(10).size());
	}

	@Test
	void generate_insufficientRecords_throwsBusinessException() {
		Member member = memberRepository.save(Member.create("지연"));

		BusinessException exception = assertThrows(
				BusinessException.class,
				() -> periodReflectionService.generate(member.getId(), AiFeatureType.THREE_DAY_REFLECTION));
		assertEquals(LifetimeErrorCode.PERIOD_REFLECTION_INSUFFICIENT_RECORDS, exception.errorCode());
	}

	private void publishedEntry(Member member, LocalDate recordDate) {
		Entry entry = Entry.create(member, EntryType.FREE, null, "본문", recordDate, null, null, null);
		entry.publish();
		entryRepository.save(entry);
	}

}
