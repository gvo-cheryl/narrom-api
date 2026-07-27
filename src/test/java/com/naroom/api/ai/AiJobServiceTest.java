package com.naroom.api.ai;

import com.naroom.api.account.domain.entity.Member;
import com.naroom.api.account.domain.repository.MemberRepository;
import com.naroom.api.ai.domain.entity.AiConversation;
import com.naroom.api.ai.domain.entity.AiFeatureType;
import com.naroom.api.ai.domain.entity.AiJob;
import com.naroom.api.ai.domain.entity.AiJobStatus;
import com.naroom.api.ai.domain.entity.AiSafetyGrade;
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

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

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
	void claimNextBatch_claimsPendingJobsAndSetsProcessing() {
		Member member = memberRepository.save(Member.create("지연"));
		Entry entry = entryRepository.save(
				Entry.create(member, EntryType.FREE, null, "본문", LocalDate.now(), null, null, null));
		AiJobResponse created = aiJobService.createForEntry(
				member.getId(), AiFeatureType.ENTRY_REFLECTION, entry.getId(), "key-" + System.nanoTime());

		List<AiJobResponse> claimed = aiJobService.claimNextBatch(10);

		assertTrue(claimed.stream().anyMatch(job -> job.id().equals(created.id())));
		AiJobResponse claimedJob = claimed.stream().filter(job -> job.id().equals(created.id())).findFirst().orElseThrow();
		assertEquals(AiJobStatus.PROCESSING, claimedJob.status());
		assertNotNull(claimedJob.startedAt());
	}

	@Test
	void claimNextBatch_alreadyProcessingJob_isNotClaimedAgain() {
		Member member = memberRepository.save(Member.create("지연"));
		Entry entry = entryRepository.save(
				Entry.create(member, EntryType.FREE, null, "본문", LocalDate.now(), null, null, null));
		AiJobResponse created = aiJobService.createForEntry(
				member.getId(), AiFeatureType.ENTRY_REFLECTION, entry.getId(), "key-" + System.nanoTime());
		aiJobService.claimNextBatch(10);

		List<AiJobResponse> secondClaim = aiJobService.claimNextBatch(10);

		assertFalse(secondClaim.stream().anyMatch(job -> job.id().equals(created.id())));
	}

	@Test
	void claimNextBatch_respectsBatchSize() {
		Member member = memberRepository.save(Member.create("지연"));
		Entry entry = entryRepository.save(
				Entry.create(member, EntryType.FREE, null, "본문", LocalDate.now(), null, null, null));
		for (int i = 0; i < 5; i++) {
			aiJobService.createForEntry(member.getId(), AiFeatureType.ENTRY_REFLECTION, entry.getId(), "batch-key-" + i + "-" + System.nanoTime());
		}

		List<AiJobResponse> claimed = aiJobService.claimNextBatch(3);

		assertEquals(3, claimed.size());
	}

	@Test
	void completeJob_withValidLease_transitionsToCompleted() {
		Member member = memberRepository.save(Member.create("지연"));
		Entry entry = entryRepository.save(
				Entry.create(member, EntryType.FREE, null, "본문", LocalDate.now(), null, null, null));
		aiJobService.createForEntry(member.getId(), AiFeatureType.ENTRY_REFLECTION, entry.getId(), "key-" + System.nanoTime());
		AiJobResponse claimed = aiJobService.claimNextBatch(10).get(0);

		boolean applied = aiJobService.completeJob(claimed.id(), claimed.startedAt());

		assertTrue(applied);
		assertEquals(AiJobStatus.COMPLETED, aiJobService.getJob(member.getId(), claimed.id()).status());
	}

	@Test
	void completeJob_withStaleLease_doesNotApplyAndLeavesJobUnchanged() {
		Member member = memberRepository.save(Member.create("지연"));
		Entry entry = entryRepository.save(
				Entry.create(member, EntryType.FREE, null, "본문", LocalDate.now(), null, null, null));
		aiJobService.createForEntry(member.getId(), AiFeatureType.ENTRY_REFLECTION, entry.getId(), "key-" + System.nanoTime());
		AiJobResponse claimed = aiJobService.claimNextBatch(10).get(0);
		Instant staleLease = claimed.startedAt().minusSeconds(60);

		boolean applied = aiJobService.completeJob(claimed.id(), staleLease);

		assertFalse(applied);
		assertEquals(AiJobStatus.PROCESSING, aiJobService.getJob(member.getId(), claimed.id()).status());
	}

	@Test
	void failJob_withValidLease_incrementsAttemptCountAndSetsErrorCode() {
		Member member = memberRepository.save(Member.create("지연"));
		Entry entry = entryRepository.save(
				Entry.create(member, EntryType.FREE, null, "본문", LocalDate.now(), null, null, null));
		aiJobService.createForEntry(member.getId(), AiFeatureType.ENTRY_REFLECTION, entry.getId(), "key-" + System.nanoTime());
		AiJobResponse claimed = aiJobService.claimNextBatch(10).get(0);
		Instant nextRetryAt = Instant.now().plusSeconds(30);

		boolean applied = aiJobService.failJob(claimed.id(), claimed.startedAt(), "GENERATION_TIMEOUT", nextRetryAt);

		assertTrue(applied);
		AiJobResponse failed = aiJobService.getJob(member.getId(), claimed.id());
		assertEquals(AiJobStatus.FAILED, failed.status());
		assertEquals(1, failed.attemptCount());
		assertEquals("GENERATION_TIMEOUT", failed.errorCode());
	}

	@Test
	void blockJob_and_markJobSafetySupport_updateStatus() {
		Member member = memberRepository.save(Member.create("지연"));
		Entry entry = entryRepository.save(
				Entry.create(member, EntryType.FREE, null, "본문", LocalDate.now(), null, null, null));
		aiJobService.createForEntry(member.getId(), AiFeatureType.ENTRY_REFLECTION, entry.getId(), "block-key-" + System.nanoTime());
		aiJobService.createForEntry(member.getId(), AiFeatureType.ENTRY_REFLECTION, entry.getId(), "safety-key-" + System.nanoTime());
		List<AiJobResponse> claimed = aiJobService.claimNextBatch(10);
		AiJobResponse blockTarget = claimed.get(0);
		AiJobResponse safetyTarget = claimed.get(1);

		assertTrue(aiJobService.blockJob(blockTarget.id(), blockTarget.startedAt()));
		assertTrue(aiJobService.markJobSafetySupport(safetyTarget.id(), safetyTarget.startedAt()));
		assertEquals(AiJobStatus.BLOCKED, aiJobService.getJob(member.getId(), blockTarget.id()).status());
		assertEquals(AiJobStatus.SAFETY_SUPPORT, aiJobService.getJob(member.getId(), safetyTarget.id()).status());
	}

	@Test
	void applyInputSafetyGrade_normal_returnsTrueAndLeavesJobProcessing() {
		Member member = memberRepository.save(Member.create("지연"));
		Entry entry = entryRepository.save(
				Entry.create(member, EntryType.FREE, null, "본문", LocalDate.now(), null, null, null));
		aiJobService.createForEntry(member.getId(), AiFeatureType.ENTRY_REFLECTION, entry.getId(), "key-" + System.nanoTime());
		AiJobResponse claimed = aiJobService.claimNextBatch(10).get(0);

		boolean proceed = aiJobService.applyInputSafetyGrade(claimed.id(), claimed.startedAt(), AiSafetyGrade.NORMAL);

		assertTrue(proceed);
		assertEquals(AiJobStatus.PROCESSING, aiJobService.getJob(member.getId(), claimed.id()).status());
	}

	@Test
	void applyInputSafetyGrade_restricted_blocksJobAndReturnsFalse() {
		Member member = memberRepository.save(Member.create("지연"));
		Entry entry = entryRepository.save(
				Entry.create(member, EntryType.FREE, null, "본문", LocalDate.now(), null, null, null));
		aiJobService.createForEntry(member.getId(), AiFeatureType.ENTRY_REFLECTION, entry.getId(), "key-" + System.nanoTime());
		AiJobResponse claimed = aiJobService.claimNextBatch(10).get(0);

		boolean proceed = aiJobService.applyInputSafetyGrade(claimed.id(), claimed.startedAt(), AiSafetyGrade.RESTRICTED);

		assertFalse(proceed);
		assertEquals(AiJobStatus.BLOCKED, aiJobService.getJob(member.getId(), claimed.id()).status());
	}

	@Test
	void applyInputSafetyGrade_crisis_marksSafetySupportAndReturnsFalse() {
		Member member = memberRepository.save(Member.create("지연"));
		Entry entry = entryRepository.save(
				Entry.create(member, EntryType.FREE, null, "본문", LocalDate.now(), null, null, null));
		aiJobService.createForEntry(member.getId(), AiFeatureType.ENTRY_REFLECTION, entry.getId(), "key-" + System.nanoTime());
		AiJobResponse claimed = aiJobService.claimNextBatch(10).get(0);

		boolean proceed = aiJobService.applyInputSafetyGrade(claimed.id(), claimed.startedAt(), AiSafetyGrade.CRISIS);

		assertFalse(proceed);
		assertEquals(AiJobStatus.SAFETY_SUPPORT, aiJobService.getJob(member.getId(), claimed.id()).status());
	}

	@Test
	void reclaimExpiredLeases_reclaimsStaleProcessingJobsAsRetryableFailures() {
		Member member = memberRepository.save(Member.create("지연"));
		Entry entry = entryRepository.save(
				Entry.create(member, EntryType.FREE, null, "본문", LocalDate.now(), null, null, null));
		AiJobResponse created = aiJobService.createForEntry(
				member.getId(), AiFeatureType.ENTRY_REFLECTION, entry.getId(), "key-" + System.nanoTime());
		Instant staleStartedAt = Instant.now().minus(Duration.ofMinutes(10));
		aiJobRepository.markClaimed(List.of(created.id()), AiJobStatus.PROCESSING, staleStartedAt);

		int reclaimed = aiJobService.reclaimExpiredLeases(Duration.ofMinutes(5));

		assertEquals(1, reclaimed);
		AiJob reloaded = aiJobRepository.findById(created.id()).orElseThrow();
		assertEquals(AiJobStatus.FAILED, reloaded.getStatus());
		assertEquals(1, reloaded.getAttemptCount());
		assertEquals("LEASE_EXPIRED", reloaded.getErrorCode());
	}

	@Test
	void reclaimExpiredLeases_doesNotTouchFreshProcessingJobs() {
		Member member = memberRepository.save(Member.create("지연"));
		Entry entry = entryRepository.save(
				Entry.create(member, EntryType.FREE, null, "본문", LocalDate.now(), null, null, null));
		aiJobService.createForEntry(member.getId(), AiFeatureType.ENTRY_REFLECTION, entry.getId(), "key-" + System.nanoTime());
		aiJobService.claimNextBatch(10);

		int reclaimed = aiJobService.reclaimExpiredLeases(Duration.ofMinutes(5));

		assertEquals(0, reclaimed);
	}

	@Test
	void claimNextBatch_jobExhaustedRetries_isNotReclaimable() {
		Member member = memberRepository.save(Member.create("지연"));
		Entry entry = entryRepository.save(
				Entry.create(member, EntryType.FREE, null, "본문", LocalDate.now(), null, null, null));
		AiJobResponse created = aiJobService.createForEntry(
				member.getId(), AiFeatureType.ENTRY_REFLECTION, entry.getId(), "key-" + System.nanoTime());
		for (int i = 0; i < 3; i++) {
			AiJobResponse claimed = aiJobService.claimNextBatch(10).stream()
					.filter(job -> job.id().equals(created.id()))
					.findFirst()
					.orElseThrow();
			aiJobService.failJob(claimed.id(), claimed.startedAt(), "GENERATION_TIMEOUT", Instant.now());
		}

		List<AiJobResponse> claimedAfterExhausted = aiJobService.claimNextBatch(10);

		assertFalse(claimedAfterExhausted.stream().anyMatch(job -> job.id().equals(created.id())));
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
