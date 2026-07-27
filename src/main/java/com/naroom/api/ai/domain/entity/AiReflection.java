package com.naroom.api.ai.domain.entity;

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
@Table(name = "ai_reflections")
public class AiReflection {

	@Id
	@GeneratedValue(strategy = GenerationType.UUID)
	@Column(name = "id", nullable = false, updatable = false)
	private UUID id;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "entry_id", nullable = false, updatable = false)
	private Entry entry;

	// PENDING 상태로 생성된 뒤 complete()에서 결과와 함께 연결되므로 다른 FK와 달리 updatable이어야 한다.
	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "generation_run_id")
	private AiGenerationRun generationRun;

	@Column(name = "version_no", nullable = false, updatable = false)
	private int versionNo;

	@Enumerated(EnumType.STRING)
	@JdbcTypeCode(SqlTypes.NAMED_ENUM)
	@Column(name = "status", nullable = false)
	private AiJobStatus status;

	@Column(name = "reflection_text")
	private String reflectionText;

	@Column(name = "question_text")
	private String questionText;

	@Column(name = "structured_result", columnDefinition = "jsonb")
	@JdbcTypeCode(SqlTypes.JSON)
	private String structuredResult;

	@Column(name = "safety_code", length = 50)
	private String safetyCode;

	@Column(name = "hidden_at")
	private Instant hiddenAt;

	@Column(name = "error_code", length = 80)
	private String errorCode;

	@CreationTimestamp
	@Column(name = "requested_at", nullable = false, updatable = false)
	private Instant requestedAt;

	@Column(name = "completed_at")
	private Instant completedAt;

	protected AiReflection() {
	}

	private AiReflection(Entry entry, int versionNo) {
		this.entry = entry;
		this.versionNo = versionNo;
		this.status = AiJobStatus.PENDING;
	}

	public static AiReflection request(Entry entry, int versionNo) {
		return new AiReflection(entry, versionNo);
	}

	public void complete(
			AiGenerationRun generationRun,
			String reflectionText,
			String questionText,
			String structuredResult,
			String safetyCode,
			Instant completedAt) {
		this.generationRun = generationRun;
		this.reflectionText = reflectionText;
		this.questionText = questionText;
		this.structuredResult = structuredResult;
		this.safetyCode = safetyCode;
		this.status = AiJobStatus.COMPLETED;
		this.completedAt = completedAt;
	}

	public void fail(String errorCode, Instant completedAt) {
		this.errorCode = errorCode;
		this.status = AiJobStatus.FAILED;
		this.completedAt = completedAt;
	}

	// 8.3절: 출력 Moderation에 걸린 경우에도 실제로 생성은 일어났으므로 generationRun은 연결해 감사 이력을 남기되,
	// 안전하지 않다고 판단된 내용 자체(reflectionText/questionText)는 저장하지 않는다.
	public void blockAsUnsafe(AiGenerationRun generationRun, String safetyCode, Instant completedAt) {
		this.generationRun = generationRun;
		this.safetyCode = safetyCode;
		this.status = AiJobStatus.BLOCKED;
		this.completedAt = completedAt;
	}

	public void markSafetySupport(AiGenerationRun generationRun, String safetyCode, Instant completedAt) {
		this.generationRun = generationRun;
		this.safetyCode = safetyCode;
		this.status = AiJobStatus.SAFETY_SUPPORT;
		this.completedAt = completedAt;
	}

	public void hide(Instant hiddenAt) {
		this.hiddenAt = hiddenAt;
	}

	public UUID getId() {
		return id;
	}

	public Entry getEntry() {
		return entry;
	}

	public AiGenerationRun getGenerationRun() {
		return generationRun;
	}

	public int getVersionNo() {
		return versionNo;
	}

	public AiJobStatus getStatus() {
		return status;
	}

	public String getReflectionText() {
		return reflectionText;
	}

	public String getQuestionText() {
		return questionText;
	}

	public String getStructuredResult() {
		return structuredResult;
	}

	public String getSafetyCode() {
		return safetyCode;
	}

	public Instant getHiddenAt() {
		return hiddenAt;
	}

	public String getErrorCode() {
		return errorCode;
	}

	public Instant getRequestedAt() {
		return requestedAt;
	}

	public Instant getCompletedAt() {
		return completedAt;
	}

}
