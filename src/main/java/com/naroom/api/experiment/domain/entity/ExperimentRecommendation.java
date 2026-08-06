package com.naroom.api.experiment.domain.entity;

import com.naroom.api.account.domain.entity.Member;
import com.naroom.api.lifetime.domain.entity.PeriodReflection;
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

// 기록·체크인·주간 회고·규칙·랜덤·AI에서 나온 코스 추천과 사용자 반응(살펴봄/수락/넘김) 이력.
// 추천은 자동 시작하지 않는다(§5.1) - startedUserProgram은 사용자가 실제로 시작을 선택했을
// 때만 연결된다.
@Entity
@Table(name = "experiment_recommendations")
public class ExperimentRecommendation {

	@Id
	@GeneratedValue(strategy = GenerationType.UUID)
	@Column(name = "id", nullable = false, updatable = false)
	private UUID id;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "member_id", nullable = false, updatable = false)
	private Member member;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "program_id", nullable = false, updatable = false)
	private ExperimentProgram program;

	@Enumerated(EnumType.STRING)
	@JdbcTypeCode(SqlTypes.NAMED_ENUM)
	@Column(name = "source_type", nullable = false, updatable = false)
	private ExperimentRecommendationSourceType sourceType;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "source_entry_id")
	private Entry sourceEntry;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "source_period_reflection_id")
	private PeriodReflection sourcePeriodReflection;

	@Column(name = "reason_code", length = 80)
	private String reasonCode;

	@Column(name = "reason_text")
	private String reasonText;

	@Column(name = "evidence", columnDefinition = "jsonb")
	@JdbcTypeCode(SqlTypes.JSON)
	private String evidence;

	@Enumerated(EnumType.STRING)
	@JdbcTypeCode(SqlTypes.NAMED_ENUM)
	@Column(name = "status", nullable = false)
	private ExperimentRecommendationStatus status;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "started_user_program_id")
	private UserExperimentProgram startedUserProgram;

	@CreationTimestamp
	@Column(name = "created_at", nullable = false, updatable = false)
	private Instant createdAt;

	@Column(name = "viewed_at")
	private Instant viewedAt;

	@Column(name = "responded_at")
	private Instant respondedAt;

	@Column(name = "expires_at")
	private Instant expiresAt;

	protected ExperimentRecommendation() {
	}

	// §14 추천 생성. 규칙 기반 추천은 항상 SHOWN으로 시작하고, 실제로 시작하지 않는 한 자동으로 상태가
	// 바뀌지 않는다(§5.1 "추천은 자동 시작하지 않는다").
	public static ExperimentRecommendation create(
			Member member, ExperimentProgram program, ExperimentRecommendationSourceType sourceType,
			Entry sourceEntry, String reasonCode, String reasonText, String evidence, Instant expiresAt) {
		ExperimentRecommendation recommendation = new ExperimentRecommendation();
		recommendation.member = member;
		recommendation.program = program;
		recommendation.sourceType = sourceType;
		recommendation.sourceEntry = sourceEntry;
		recommendation.reasonCode = reasonCode;
		recommendation.reasonText = reasonText;
		recommendation.evidence = evidence;
		recommendation.status = ExperimentRecommendationStatus.SHOWN;
		recommendation.expiresAt = expiresAt;
		return recommendation;
	}

	// §11.5 "살펴봄" 상태. 목록에 노출된 것만으로는 살펴본 것으로 보지 않으므로 SHOWN에서만 전이한다.
	public void view(Instant now) {
		if (this.status == ExperimentRecommendationStatus.SHOWN) {
			this.status = ExperimentRecommendationStatus.VIEWED;
			this.viewedAt = now;
		}
	}

	// "지금은 괜찮아요" - 추천을 넘긴다.
	public void dismiss(Instant now) {
		this.status = ExperimentRecommendationStatus.DISMISSED;
		this.respondedAt = now;
	}

	// §13 코스 시작 트랜잭션 6단계: 추천으로 시작했다면 상태를 ACCEPTED로 바꾸고 시작된 코스를 연결한다.
	public void accept(UserExperimentProgram startedUserProgram, Instant now) {
		this.status = ExperimentRecommendationStatus.ACCEPTED;
		this.startedUserProgram = startedUserProgram;
		this.respondedAt = now;
	}

	public UUID getId() {
		return id;
	}

	public Member getMember() {
		return member;
	}

	public ExperimentProgram getProgram() {
		return program;
	}

	public ExperimentRecommendationSourceType getSourceType() {
		return sourceType;
	}

	public Entry getSourceEntry() {
		return sourceEntry;
	}

	public PeriodReflection getSourcePeriodReflection() {
		return sourcePeriodReflection;
	}

	public String getReasonCode() {
		return reasonCode;
	}

	public String getReasonText() {
		return reasonText;
	}

	public String getEvidence() {
		return evidence;
	}

	public ExperimentRecommendationStatus getStatus() {
		return status;
	}

	public UserExperimentProgram getStartedUserProgram() {
		return startedUserProgram;
	}

	public Instant getCreatedAt() {
		return createdAt;
	}

	public Instant getViewedAt() {
		return viewedAt;
	}

	public Instant getRespondedAt() {
		return respondedAt;
	}

	public Instant getExpiresAt() {
		return expiresAt;
	}

}
