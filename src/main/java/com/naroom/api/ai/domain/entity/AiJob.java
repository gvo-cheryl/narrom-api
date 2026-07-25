package com.naroom.api.ai.domain.entity;

import com.naroom.api.account.domain.entity.Member;
import com.naroom.api.record.domain.entity.Entry;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "ai_jobs")
public class AiJob {

	@Id
	@GeneratedValue(strategy = GenerationType.UUID)
	@Column(name = "id", nullable = false, updatable = false)
	private UUID id;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "member_id", nullable = false, updatable = false)
	private Member member;

	@Enumerated(EnumType.STRING)
	@JdbcTypeCode(SqlTypes.NAMED_ENUM)
	@Column(name = "feature_type", nullable = false, updatable = false)
	private AiFeatureType featureType;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "entry_id", updatable = false)
	private Entry entry;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "conversation_id", updatable = false)
	private AiConversation conversation;

	@Enumerated(EnumType.STRING)
	@JdbcTypeCode(SqlTypes.NAMED_ENUM)
	@Column(name = "status", nullable = false)
	private AiJobStatus status;

	@Column(name = "idempotency_key", nullable = false, updatable = false, length = 100)
	private String idempotencyKey;

	@Column(name = "attempt_count", nullable = false)
	private int attemptCount;

	@Column(name = "max_attempts", nullable = false, updatable = false)
	private int maxAttempts;

	@Column(name = "next_retry_at")
	private Instant nextRetryAt;

	@Column(name = "started_at")
	private Instant startedAt;

	@Column(name = "completed_at")
	private Instant completedAt;

	@Column(name = "error_code", length = 80)
	private String errorCode;

	@CreationTimestamp
	@Column(name = "created_at", nullable = false, updatable = false)
	private Instant createdAt;

	protected AiJob() {
	}

	private AiJob(Member member, AiFeatureType featureType, Entry entry, AiConversation conversation, String idempotencyKey) {
		this.member = member;
		this.featureType = featureType;
		this.entry = entry;
		this.conversation = conversation;
		this.status = AiJobStatus.PENDING;
		this.idempotencyKey = idempotencyKey;
		this.attemptCount = 0;
		this.maxAttempts = 3;
	}

	public static AiJob forEntry(Member member, AiFeatureType featureType, Entry entry, String idempotencyKey) {
		return new AiJob(member, featureType, entry, null, idempotencyKey);
	}

	public static AiJob forConversation(Member member, AiFeatureType featureType, AiConversation conversation, String idempotencyKey) {
		return new AiJob(member, featureType, null, conversation, idempotencyKey);
	}

	public void markProcessing(Instant startedAt) {
		this.status = AiJobStatus.PROCESSING;
		this.startedAt = startedAt;
	}

	public void markCompleted(Instant completedAt) {
		this.status = AiJobStatus.COMPLETED;
		this.completedAt = completedAt;
	}

	public void markFailed(String errorCode, Instant nextRetryAt) {
		this.status = AiJobStatus.FAILED;
		this.errorCode = errorCode;
		this.attemptCount++;
		this.nextRetryAt = nextRetryAt;
	}

	public void markBlocked(Instant completedAt) {
		this.status = AiJobStatus.BLOCKED;
		this.completedAt = completedAt;
	}

	public void markSafetySupport(Instant completedAt) {
		this.status = AiJobStatus.SAFETY_SUPPORT;
		this.completedAt = completedAt;
	}

	public UUID getId() {
		return id;
	}

	public Member getMember() {
		return member;
	}

	public AiFeatureType getFeatureType() {
		return featureType;
	}

	public Entry getEntry() {
		return entry;
	}

	public AiConversation getConversation() {
		return conversation;
	}

	public AiJobStatus getStatus() {
		return status;
	}

	public String getIdempotencyKey() {
		return idempotencyKey;
	}

	public int getAttemptCount() {
		return attemptCount;
	}

	public int getMaxAttempts() {
		return maxAttempts;
	}

	public Instant getNextRetryAt() {
		return nextRetryAt;
	}

	public Instant getStartedAt() {
		return startedAt;
	}

	public Instant getCompletedAt() {
		return completedAt;
	}

	public String getErrorCode() {
		return errorCode;
	}

	public Instant getCreatedAt() {
		return createdAt;
	}

}
