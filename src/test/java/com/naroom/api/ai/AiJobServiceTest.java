package com.naroom.api.ai;

import com.naroom.api.account.domain.entity.Member;
import com.naroom.api.account.domain.repository.MemberRepository;
import com.naroom.api.ai.domain.entity.AiConversation;
import com.naroom.api.ai.domain.entity.AiFeatureType;
import com.naroom.api.ai.domain.entity.AiJobStatus;
import com.naroom.api.ai.domain.error.AiErrorCode;
import com.naroom.api.ai.domain.repository.AiConversationRepository;
import com.naroom.api.ai.domain.repository.AiJobRepository;
import com.naroom.api.ai.dto.AiJobResponse;
import com.naroom.api.global.error.exception.BusinessException;
import com.naroom.api.record.domain.entity.Entry;
import com.naroom.api.record.domain.entity.EntryType;
import com.naroom.api.record.domain.error.RecordErrorCode;
import com.naroom.api.record.domain.repository.EntryRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

@SpringBootTest
@Transactional
@DirtiesContext
class AiJobServiceTest {

	@Autowired
	private AiJobService aiJobService;

	@Autowired
	private AiJobRepository aiJobRepository;

	@Autowired
	private EntryRepository entryRepository;

	@Autowired
	private AiConversationRepository aiConversationRepository;

	@Autowired
	private MemberRepository memberRepository;

	@Test
	void createForEntry_calledTwiceWithSameIdempotencyKey_doesNotDuplicate() {
		Member member = memberRepository.save(Member.create("지연"));
		Entry entry = entryRepository.save(
				Entry.create(member, EntryType.FREE, null, "본문", LocalDate.now(), null, null, null));
		String idempotencyKey = "key-" + System.nanoTime();

		AiJobResponse first = aiJobService.createForEntry(member.getId(), AiFeatureType.ENTRY_REFLECTION, entry.getId(), idempotencyKey);
		AiJobResponse second = aiJobService.createForEntry(member.getId(), AiFeatureType.ENTRY_REFLECTION, entry.getId(), idempotencyKey);

		assertEquals(first.id(), second.id());
		assertEquals(1, aiJobRepository.findAll().stream().filter(job -> job.getIdempotencyKey().equals(idempotencyKey)).count());
	}

	@Test
	void createForEntry_setsInitialStatusPending() {
		Member member = memberRepository.save(Member.create("지연"));
		Entry entry = entryRepository.save(
				Entry.create(member, EntryType.FREE, null, "본문", LocalDate.now(), null, null, null));

		AiJobResponse job = aiJobService.createForEntry(
				member.getId(), AiFeatureType.ENTRY_REFLECTION, entry.getId(), "key-" + System.nanoTime());

		assertEquals(AiJobStatus.PENDING, job.status());
		assertEquals(entry.getId(), job.entryId());
	}

	@Test
	void createForEntry_otherMembersEntry_throwsEntryNotFound() {
		Member owner = memberRepository.save(Member.create("소유자"));
		Member intruder = memberRepository.save(Member.create("다른회원"));
		Entry entry = entryRepository.save(
				Entry.create(owner, EntryType.FREE, null, "본문", LocalDate.now(), null, null, null));

		BusinessException exception = assertThrows(
				BusinessException.class,
				() -> aiJobService.createForEntry(
						intruder.getId(), AiFeatureType.ENTRY_REFLECTION, entry.getId(), "key-" + System.nanoTime()));
		assertEquals(RecordErrorCode.ENTRY_NOT_FOUND, exception.errorCode());
	}

	@Test
	void createForConversation_setsConversationId() {
		Member member = memberRepository.save(Member.create("지연"));
		AiConversation conversation = aiConversationRepository.save(
				AiConversation.start(member, AiFeatureType.CONVERSATION_REPLY, null));

		AiJobResponse job = aiJobService.createForConversation(
				member.getId(), AiFeatureType.CONVERSATION_REPLY, conversation.getId(), "key-" + System.nanoTime());

		assertEquals(conversation.getId(), job.conversationId());
	}

	@Test
	void markProcessing_thenCompleted_updatesStatusAndTimestamps() {
		Member member = memberRepository.save(Member.create("지연"));
		Entry entry = entryRepository.save(
				Entry.create(member, EntryType.FREE, null, "본문", LocalDate.now(), null, null, null));
		AiJobResponse created = aiJobService.createForEntry(
				member.getId(), AiFeatureType.ENTRY_REFLECTION, entry.getId(), "key-" + System.nanoTime());

		AiJobResponse processing = aiJobService.markProcessing(created.id());
		assertEquals(AiJobStatus.PROCESSING, processing.status());
		assertNotNull(processing.startedAt());

		AiJobResponse completed = aiJobService.markCompleted(created.id());
		assertEquals(AiJobStatus.COMPLETED, completed.status());
		assertNotNull(completed.completedAt());
	}

	@Test
	void markFailed_incrementsAttemptCountAndSetsErrorCode() {
		Member member = memberRepository.save(Member.create("지연"));
		Entry entry = entryRepository.save(
				Entry.create(member, EntryType.FREE, null, "본문", LocalDate.now(), null, null, null));
		AiJobResponse created = aiJobService.createForEntry(
				member.getId(), AiFeatureType.ENTRY_REFLECTION, entry.getId(), "key-" + System.nanoTime());
		Instant nextRetryAt = Instant.now().plusSeconds(30);

		AiJobResponse failed = aiJobService.markFailed(created.id(), "GENERATION_TIMEOUT", nextRetryAt);

		assertEquals(AiJobStatus.FAILED, failed.status());
		assertEquals(1, failed.attemptCount());
		assertEquals("GENERATION_TIMEOUT", failed.errorCode());
		assertEquals(nextRetryAt, failed.nextRetryAt());
	}

	@Test
	void markBlocked_and_markSafetySupport_updateStatus() {
		Member member = memberRepository.save(Member.create("지연"));
		Entry entry = entryRepository.save(
				Entry.create(member, EntryType.FREE, null, "본문", LocalDate.now(), null, null, null));
		AiJobResponse blockedTarget = aiJobService.createForEntry(
				member.getId(), AiFeatureType.ENTRY_REFLECTION, entry.getId(), "key-" + System.nanoTime());
		AiJobResponse safetyTarget = aiJobService.createForEntry(
				member.getId(), AiFeatureType.ENTRY_REFLECTION, entry.getId(), "key-" + System.nanoTime());

		assertEquals(AiJobStatus.BLOCKED, aiJobService.markBlocked(blockedTarget.id()).status());
		assertEquals(AiJobStatus.SAFETY_SUPPORT, aiJobService.markSafetySupport(safetyTarget.id()).status());
	}

	// 근거(AI 작업)는 회원 소유 자원이므로, 다른 회원의 작업 조회는 존재 여부를 드러내지 않고 JOB_NOT_FOUND로만 응답해야 한다.
	@Test
	void getJob_otherMembersJob_throwsJobNotFound() {
		Member owner = memberRepository.save(Member.create("소유자"));
		Member intruder = memberRepository.save(Member.create("다른회원"));
		Entry entry = entryRepository.save(
				Entry.create(owner, EntryType.FREE, null, "본문", LocalDate.now(), null, null, null));
		AiJobResponse job = aiJobService.createForEntry(
				owner.getId(), AiFeatureType.ENTRY_REFLECTION, entry.getId(), "key-" + System.nanoTime());

		BusinessException exception = assertThrows(
				BusinessException.class,
				() -> aiJobService.getJob(intruder.getId(), job.id()));
		assertEquals(AiErrorCode.JOB_NOT_FOUND, exception.errorCode());
	}

	@Test
	void getJob_unknownId_throwsJobNotFound() {
		Member member = memberRepository.save(Member.create("지연"));

		BusinessException exception = assertThrows(
				BusinessException.class,
				() -> aiJobService.getJob(member.getId(), UUID.randomUUID()));
		assertEquals(AiErrorCode.JOB_NOT_FOUND, exception.errorCode());
	}

}
