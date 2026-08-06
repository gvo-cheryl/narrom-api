package com.naroom.api.lifetime.domain.entity;

import com.naroom.api.account.domain.entity.Member;
import com.naroom.api.ai.domain.entity.AiFeatureType;
import com.naroom.api.ai.domain.entity.AiGenerationRun;
import com.naroom.api.ai.domain.entity.AiJobStatus;
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
import java.time.LocalDate;
import java.util.UUID;

// ai_reflections와 같은 패턴이다: 실제 회고 텍스트는 여기에 저장하고, 모델·토큰·프롬프트 버전·안전 분류
// 이력은 AiGenerationRun이 전담한다. status는 새 enum을 만들지 않고 기존 AiJobStatus를 재사용해
// 개별 기록 회고(AiReflection)와 동일한 상태 전이 규칙(PENDING→PROCESSING→COMPLETED/BLOCKED/
// SAFETY_SUPPORT/FAILED)을 따른다.
@Entity
@Table(name = "period_reflections")
public class PeriodReflection {

	@Id
	@GeneratedValue(strategy = GenerationType.UUID)
	@Column(name = "id", nullable = false, updatable = false)
	private UUID id;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "member_id", nullable = false, updatable = false)
	private Member member;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "entry_id", nullable = false, updatable = false)
	private Entry entry;

	@Enumerated(EnumType.STRING)
	@JdbcTypeCode(SqlTypes.NAMED_ENUM)
	@Column(name = "feature_type", nullable = false, updatable = false)
	private AiFeatureType featureType;

	@Column(name = "period_start", nullable = false, updatable = false)
	private LocalDate periodStart;

	@Column(name = "period_end", nullable = false, updatable = false)
	private LocalDate periodEnd;

	@Column(name = "version_no", nullable = false, updatable = false)
	private int versionNo;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "previous_reflection_id", updatable = false)
	private PeriodReflection previousReflection;

	@Enumerated(EnumType.STRING)
	@JdbcTypeCode(SqlTypes.NAMED_ENUM)
	@Column(name = "status", nullable = false)
	private AiJobStatus status;

	// PENDING 상태로 생성된 뒤 complete()에서 결과와 함께 연결되므로 다른 FK와 달리 updatable이어야 한다.
	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "generation_run_id")
	private AiGenerationRun generationRun;

	@Column(name = "summary_text")
	private String summaryText;

	@Column(name = "insights", columnDefinition = "jsonb")
	@JdbcTypeCode(SqlTypes.JSON)
	private String insights;

	@Column(name = "question_text")
	private String questionText;

	@Column(name = "safety_code", length = 50)
	private String safetyCode;

	@Column(name = "error_code", length = 80)
	private String errorCode;

	// 3일 코스 종료 시 요청한 코스 전용 AI 회고를 연결한다(§11.3). experiment 도메인 엔티티를 직접
	// 참조하지 않고 단일 컬럼으로만 매핑한다 - lifetime↔experiment 패키지 순환 의존을 만들지 않기 위함이다
	// (ExperimentMissionRecord가 복합 FK를 단일 컬럼으로 단순화한 것과 같은 이유).
	@Column(name = "user_experiment_program_id")
	private UUID userExperimentProgramId;

	@CreationTimestamp
	@Column(name = "requested_at", nullable = false, updatable = false)
	private Instant requestedAt;

	@Column(name = "completed_at")
	private Instant completedAt;

	protected PeriodReflection() {
	}

	private PeriodReflection(
			Member member,
			Entry entry,
			AiFeatureType featureType,
			LocalDate periodStart,
			LocalDate periodEnd,
			int versionNo,
			PeriodReflection previousReflection) {
		this.member = member;
		this.entry = entry;
		this.featureType = featureType;
		this.periodStart = periodStart;
		this.periodEnd = periodEnd;
		this.versionNo = versionNo;
		this.previousReflection = previousReflection;
		this.status = AiJobStatus.PENDING;
	}

	public static PeriodReflection request(
			Member member, Entry entry, AiFeatureType featureType, LocalDate periodStart, LocalDate periodEnd) {
		return new PeriodReflection(member, entry, featureType, periodStart, periodEnd, 1, null);
	}

	// §11.3: 3일 코스 종료 시 사용자가 선택한 코스 전용 AI 회고다. featureType은 항상
	// THREE_DAY_REFLECTION이고, 근거 기록은 이 코스에서 만들어진 기록으로 한정한다(호출자가 선정해서 넘김).
	public static PeriodReflection requestForExperimentCourse(
			Member member, Entry entry, LocalDate periodStart, LocalDate periodEnd, UUID userExperimentProgramId) {
		PeriodReflection reflection = new PeriodReflection(
				member, entry, AiFeatureType.THREE_DAY_REFLECTION, periodStart, periodEnd, 1, null);
		reflection.userExperimentProgramId = userExperimentProgramId;
		return reflection;
	}

	// 잘못된 과거 해석이 연쇄적으로 누적되는 것을 막기 위해 이전 대화를 이어가지 않고 기간 데이터로
	// 새로 생성한다(§12.3) - previousReflection은 이력 추적용 참조일 뿐 내용을 상속하지 않는다.
	public static PeriodReflection regenerate(Member member, Entry entry, PeriodReflection previousReflection) {
		PeriodReflection reflection = new PeriodReflection(
				member,
				entry,
				previousReflection.featureType,
				previousReflection.periodStart,
				previousReflection.periodEnd,
				previousReflection.versionNo + 1,
				previousReflection);
		reflection.userExperimentProgramId = previousReflection.userExperimentProgramId;
		return reflection;
	}

	public void complete(
			AiGenerationRun generationRun, String summaryText, String insights, String questionText, Instant completedAt) {
		this.generationRun = generationRun;
		this.summaryText = summaryText;
		this.insights = insights;
		this.questionText = questionText;
		this.status = AiJobStatus.COMPLETED;
		this.completedAt = completedAt;
	}

	public void fail(String errorCode, Instant completedAt) {
		this.errorCode = errorCode;
		this.status = AiJobStatus.FAILED;
		this.completedAt = completedAt;
	}

	// 8.3절: 출력 Moderation에 걸린 경우에도 실제로 생성은 일어났으므로 generationRun은 연결해 감사 이력을
	// 남기되, 안전하지 않다고 판단된 내용 자체(summaryText/questionText/insights)는 저장하지 않는다.
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

	public UUID getId() {
		return id;
	}

	public Member getMember() {
		return member;
	}

	public Entry getEntry() {
		return entry;
	}

	public AiFeatureType getFeatureType() {
		return featureType;
	}

	public LocalDate getPeriodStart() {
		return periodStart;
	}

	public LocalDate getPeriodEnd() {
		return periodEnd;
	}

	public int getVersionNo() {
		return versionNo;
	}

	public PeriodReflection getPreviousReflection() {
		return previousReflection;
	}

	public AiJobStatus getStatus() {
		return status;
	}

	public AiGenerationRun getGenerationRun() {
		return generationRun;
	}

	public String getSummaryText() {
		return summaryText;
	}

	public String getInsights() {
		return insights;
	}

	public String getQuestionText() {
		return questionText;
	}

	public String getSafetyCode() {
		return safetyCode;
	}

	public String getErrorCode() {
		return errorCode;
	}

	public UUID getUserExperimentProgramId() {
		return userExperimentProgramId;
	}

	public Instant getRequestedAt() {
		return requestedAt;
	}

	public Instant getCompletedAt() {
		return completedAt;
	}

}
