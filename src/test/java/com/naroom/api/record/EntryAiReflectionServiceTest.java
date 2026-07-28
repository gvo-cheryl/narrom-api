package com.naroom.api.record;

import com.naroom.api.account.domain.entity.Member;
import com.naroom.api.account.domain.repository.MemberRepository;
import com.naroom.api.ai.AiJobService;
import com.naroom.api.ai.domain.entity.AiFeatureType;
import com.naroom.api.ai.domain.entity.AiJobStatus;
import com.naroom.api.ai.domain.entity.AiReflection;
import com.naroom.api.ai.domain.repository.AiReflectionRepository;
import com.naroom.api.ai.dto.AiJobResponse;
import com.naroom.api.global.error.exception.BusinessException;
import com.naroom.api.record.domain.entity.Entry;
import com.naroom.api.record.domain.entity.EntryType;
import com.naroom.api.record.domain.error.RecordErrorCode;
import com.naroom.api.record.domain.repository.EntryRepository;
import com.naroom.api.record.dto.EntryAiReflectionResponse;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

@SpringBootTest
@Transactional
@DirtiesContext
class EntryAiReflectionServiceTest {

	@Autowired
	private EntryAiReflectionService entryAiReflectionService;

	@Autowired
	private EntryRepository entryRepository;

	@Autowired
	private MemberRepository memberRepository;

	@Autowired
	private AiJobService aiJobService;

	@Autowired
	private AiReflectionRepository aiReflectionRepository;

	@Test
	void getStatus_noAiJobYet_returnsNotRequested() {
		Member member = memberRepository.save(Member.create("지연"));
		Entry entry = entryRepository.save(
				Entry.create(member, EntryType.FREE, null, "본문", LocalDate.now(), null, null, null));

		EntryAiReflectionResponse response = entryAiReflectionService.getStatus(member.getId(), entry.getId());

		assertNull(response.status());
		assertNull(response.reflectionText());
	}

	@Test
	void getStatus_jobPending_returnsPendingWithoutReflectionContent() {
		Member member = memberRepository.save(Member.create("지연"));
		Entry entry = entryRepository.save(
				Entry.create(member, EntryType.FREE, null, "본문", LocalDate.now(), null, null, null));
		aiJobService.createForEntry(member.getId(), AiFeatureType.ENTRY_REFLECTION, entry.getId(), "key-" + System.nanoTime());

		EntryAiReflectionResponse response = entryAiReflectionService.getStatus(member.getId(), entry.getId());

		assertEquals(AiJobStatus.PENDING, response.status());
		assertNull(response.reflectionText());
	}

	@Test
	void getStatus_jobProcessing_returnsProcessingStatus() {
		Member member = memberRepository.save(Member.create("지연"));
		Entry entry = entryRepository.save(
				Entry.create(member, EntryType.FREE, null, "본문", LocalDate.now(), null, null, null));
		aiJobService.createForEntry(member.getId(), AiFeatureType.ENTRY_REFLECTION, entry.getId(), "key-" + System.nanoTime());
		aiJobService.claimNextBatch(10);

		EntryAiReflectionResponse response = entryAiReflectionService.getStatus(member.getId(), entry.getId());

		assertEquals(AiJobStatus.PROCESSING, response.status());
	}

	@Test
	void getStatus_reflectionCompleted_returnsReflectionContent() {
		Member member = memberRepository.save(Member.create("지연"));
		Entry entry = entryRepository.save(
				Entry.create(member, EntryType.FREE, null, "본문", LocalDate.now(), null, null, null));
		AiJobResponse claimed = createAndClaimJob(member, entry);
		AiReflection reflection = aiReflectionRepository.save(AiReflection.request(entry, 1));
		reflection.complete(null, "정리 결과", "질문", null, null, Instant.now());
		aiJobService.completeJob(claimed.id(), claimed.startedAt());

		EntryAiReflectionResponse response = entryAiReflectionService.getStatus(member.getId(), entry.getId());

		assertEquals(AiJobStatus.COMPLETED, response.status());
		assertEquals("정리 결과", response.reflectionText());
		assertEquals("질문", response.reflectionQuestion());
	}

	@Test
	void getStatus_entryNotOwnedByMember_throwsEntryNotFound() {
		Member owner = memberRepository.save(Member.create("소유자"));
		Member intruder = memberRepository.save(Member.create("다른회원"));
		Entry entry = entryRepository.save(
				Entry.create(owner, EntryType.FREE, null, "본문", LocalDate.now(), null, null, null));

		BusinessException exception = assertThrows(
				BusinessException.class,
				() -> entryAiReflectionService.getStatus(intruder.getId(), entry.getId()));
		assertEquals(RecordErrorCode.ENTRY_NOT_FOUND, exception.errorCode());
	}

	private AiJobResponse createAndClaimJob(Member member, Entry entry) {
		aiJobService.createForEntry(member.getId(), AiFeatureType.ENTRY_REFLECTION, entry.getId(), "key-" + System.nanoTime());
		return aiJobService.claimNextBatch(10).get(0);
	}

}
