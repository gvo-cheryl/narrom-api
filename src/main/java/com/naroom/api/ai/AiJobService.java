package com.naroom.api.ai;

import com.naroom.api.account.domain.entity.Member;
import com.naroom.api.account.domain.repository.MemberRepository;
import com.naroom.api.ai.domain.entity.AiConversation;
import com.naroom.api.ai.domain.entity.AiFeatureType;
import com.naroom.api.ai.domain.entity.AiJob;
import com.naroom.api.ai.domain.entity.AiJobStatus;
import com.naroom.api.ai.domain.error.AiErrorCode;
import com.naroom.api.ai.domain.repository.AiConversationRepository;
import com.naroom.api.ai.domain.repository.AiJobRepository;
import com.naroom.api.ai.dto.AiJobResponse;
import com.naroom.api.global.error.exception.BusinessException;
import com.naroom.api.record.domain.entity.Entry;
import com.naroom.api.record.domain.error.RecordErrorCode;
import com.naroom.api.record.domain.repository.EntryRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import java.util.function.Consumer;
import java.util.stream.Collectors;

@Service
@Transactional(readOnly = true)
public class AiJobService {

	private static final String LEASE_EXPIRED_ERROR_CODE = "LEASE_EXPIRED";

	private final AiJobRepository aiJobRepository;
	private final MemberRepository memberRepository;
	private final EntryRepository entryRepository;
	private final AiConversationRepository aiConversationRepository;

	public AiJobService(
			AiJobRepository aiJobRepository,
			MemberRepository memberRepository,
			EntryRepository entryRepository,
			AiConversationRepository aiConversationRepository) {
		this.aiJobRepository = aiJobRepository;
		this.memberRepository = memberRepository;
		this.entryRepository = entryRepository;
		this.aiConversationRepository = aiConversationRepository;
	}

	// 6.1절: 동일 요청의 중복 전송은 멱등키로 차단한다. 이미 같은 (member, idempotencyKey) 작업이 있으면 새로 만들지 않고 그대로 반환한다.
	@Transactional
	public AiJobResponse createForEntry(UUID memberId, AiFeatureType featureType, UUID entryId, String idempotencyKey) {
		return aiJobRepository.findByMember_IdAndIdempotencyKey(memberId, idempotencyKey)
				.map(AiJobResponse::from)
				.orElseGet(() -> {
					Member member = memberRepository.getReferenceById(memberId);
					Entry entry = entryRepository.findByIdAndMember_Id(entryId, memberId)
							.orElseThrow(() -> new BusinessException(RecordErrorCode.ENTRY_NOT_FOUND));
					AiJob job = AiJob.forEntry(member, featureType, entry, idempotencyKey);
					return AiJobResponse.from(aiJobRepository.save(job));
				});
	}

	@Transactional
	public AiJobResponse createForConversation(
			UUID memberId, AiFeatureType featureType, UUID conversationId, String idempotencyKey) {
		return aiJobRepository.findByMember_IdAndIdempotencyKey(memberId, idempotencyKey)
				.map(AiJobResponse::from)
				.orElseGet(() -> {
					Member member = memberRepository.getReferenceById(memberId);
					AiConversation conversation = aiConversationRepository.findByIdAndMember_Id(conversationId, memberId)
							.orElseThrow(() -> new BusinessException(AiErrorCode.CONVERSATION_NOT_FOUND));
					AiJob job = AiJob.forConversation(member, featureType, conversation, idempotencyKey);
					return AiJobResponse.from(aiJobRepository.save(job));
				});
	}

	public AiJobResponse getJob(UUID memberId, UUID jobId) {
		return AiJobResponse.from(getOwnedJobOrThrow(memberId, jobId));
	}

	// 큐 선점(4-B): SKIP LOCKED로 고른 id를 같은 트랜잭션 안에서 PROCESSING으로 일괄 전환한다.
	// SELECT에서 잡은 행 잠금이 커밋까지 유지되므로 뒤이은 UPDATE·조회는 경쟁 없이 안전하다.
	@Transactional
	public List<AiJobResponse> claimNextBatch(int batchSize) {
		List<UUID> claimableIds = aiJobRepository.selectClaimableIds(batchSize);
		if (claimableIds.isEmpty()) {
			return List.of();
		}
		aiJobRepository.markClaimed(claimableIds, AiJobStatus.PROCESSING, Instant.now());
		return aiJobRepository.findAllById(claimableIds).stream()
				.map(AiJobResponse::from)
				.collect(Collectors.toList());
	}

	// leaseTimeout보다 오래 PROCESSING 상태인 작업(워커가 죽었거나 응답이 없는 경우)을 회수해 재시도 대상으로 되돌린다.
	@Transactional
	public int reclaimExpiredLeases(Duration leaseTimeout) {
		Instant expiredBefore = Instant.now().minus(leaseTimeout);
		return aiJobRepository.reclaimExpiredLeases(
				AiJobStatus.PROCESSING, AiJobStatus.FAILED, LEASE_EXPIRED_ERROR_CODE, Instant.now(), expiredBefore);
	}

	// 아래 completeJob/failJob/blockJob/markJobSafetySupport는 claimNextBatch가 돌려준 startedAt(lease)을
	// 그대로 넘겨받아야 한다. lease가 이미 만료되어 다른 워커에게 재할당된 작업이면 false를 반환하고 결과를 버린다.
	@Transactional
	public boolean completeJob(UUID jobId, Instant leaseStartedAt) {
		return applyIfLeaseValid(jobId, leaseStartedAt, job -> job.markCompleted(Instant.now()));
	}

	@Transactional
	public boolean failJob(UUID jobId, Instant leaseStartedAt, String errorCode, Instant nextRetryAt) {
		return applyIfLeaseValid(jobId, leaseStartedAt, job -> job.markFailed(errorCode, nextRetryAt));
	}

	@Transactional
	public boolean blockJob(UUID jobId, Instant leaseStartedAt) {
		return applyIfLeaseValid(jobId, leaseStartedAt, job -> job.markBlocked(Instant.now()));
	}

	@Transactional
	public boolean markJobSafetySupport(UUID jobId, Instant leaseStartedAt) {
		return applyIfLeaseValid(jobId, leaseStartedAt, job -> job.markSafetySupport(Instant.now()));
	}

	// PESSIMISTIC_WRITE로 행을 잠근 뒤 메모리에서 status+startedAt을 비교한다. SKIP LOCKED 특수 타임아웃 값 없이도
	// 같은 작업을 동시에 완료 처리하려는 시도끼리는 이 잠금으로 직렬화되어 안전하다.
	private boolean applyIfLeaseValid(UUID jobId, Instant leaseStartedAt, Consumer<AiJob> transition) {
		AiJob job = aiJobRepository.findByIdForUpdate(jobId)
				.orElseThrow(() -> new BusinessException(AiErrorCode.JOB_NOT_FOUND));
		if (job.getStatus() != AiJobStatus.PROCESSING || !Objects.equals(job.getStartedAt(), leaseStartedAt)) {
			return false;
		}
		transition.accept(job);
		return true;
	}

	private AiJob getOwnedJobOrThrow(UUID memberId, UUID jobId) {
		AiJob job = getJobOrThrow(jobId);
		if (!job.getMember().getId().equals(memberId)) {
			throw new BusinessException(AiErrorCode.JOB_NOT_FOUND);
		}
		return job;
	}

	private AiJob getJobOrThrow(UUID jobId) {
		return aiJobRepository.findById(jobId)
				.orElseThrow(() -> new BusinessException(AiErrorCode.JOB_NOT_FOUND));
	}

}
