package com.naroom.api.ai;

import com.naroom.api.account.domain.entity.Member;
import com.naroom.api.account.domain.repository.MemberRepository;
import com.naroom.api.ai.domain.entity.AiConversation;
import com.naroom.api.ai.domain.entity.AiFeatureType;
import com.naroom.api.ai.domain.entity.AiJob;
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

import java.time.Instant;
import java.util.UUID;

@Service
@Transactional(readOnly = true)
public class AiJobService {

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

	// 상태 전이는 실행 주체가 사용자 요청이 아니라 내부 AI 파이프라인/워커이므로 memberId 소유권 검증 없이 jobId만으로 처리한다.
	@Transactional
	public AiJobResponse markProcessing(UUID jobId) {
		AiJob job = getJobOrThrow(jobId);
		job.markProcessing(Instant.now());
		return AiJobResponse.from(job);
	}

	@Transactional
	public AiJobResponse markCompleted(UUID jobId) {
		AiJob job = getJobOrThrow(jobId);
		job.markCompleted(Instant.now());
		return AiJobResponse.from(job);
	}

	@Transactional
	public AiJobResponse markFailed(UUID jobId, String errorCode, Instant nextRetryAt) {
		AiJob job = getJobOrThrow(jobId);
		job.markFailed(errorCode, nextRetryAt);
		return AiJobResponse.from(job);
	}

	@Transactional
	public AiJobResponse markBlocked(UUID jobId) {
		AiJob job = getJobOrThrow(jobId);
		job.markBlocked(Instant.now());
		return AiJobResponse.from(job);
	}

	@Transactional
	public AiJobResponse markSafetySupport(UUID jobId) {
		AiJob job = getJobOrThrow(jobId);
		job.markSafetySupport(Instant.now());
		return AiJobResponse.from(job);
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
