package com.naroom.api.experiment.domain.entity;

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
import jakarta.persistence.Version;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.UpdateTimestamp;
import org.hibernate.type.SqlTypes;

import java.time.Instant;
import java.util.UUID;

// 3일·7일 코스 원본. 사용자가 시작하면 UserExperimentProgram/UserProgramMission에 문구가
// 스냅샷으로 복사되므로, 여기 내용이 나중에 수정돼도 이미 시작한 사용자의 코스는 바뀌지 않는다(§12.2).
@Entity
@Table(name = "experiment_programs")
public class ExperimentProgram {

	@Id
	@GeneratedValue(strategy = GenerationType.UUID)
	@Column(name = "id", nullable = false, updatable = false)
	private UUID id;

	@Column(name = "code", nullable = false, updatable = false, length = 80)
	private String code;

	// 사람이 보는 리비전 번호. row 생성 시점에 고정되고 DRAFT 내용 수정으로는 바뀌지 않는다 - 새 리비전은
	// createRevision이 새 row를 만들며 +1한 값을 넣는다(quotes/record_prompts의 versionNo와 동일한 패턴,
	// V27: 낙관적 잠금과는 분리 - 자세한 경위는 V27 마이그레이션 주석 참고).
	@Column(name = "content_version", nullable = false, updatable = false)
	private int contentVersion;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "primary_topic_id", nullable = false)
	private ExperimentTopic primaryTopic;

	@Column(name = "title", nullable = false, length = 120)
	private String title;

	@Column(name = "description", nullable = false)
	private String description;

	// Beta 1은 3·7일만 노출한다(§4). DB는 14·28일도 허용하지만 API 계층에서 3·7만 검증한다.
	@Column(name = "duration_days", nullable = false)
	private short durationDays;

	@Enumerated(EnumType.STRING)
	@JdbcTypeCode(SqlTypes.NAMED_ENUM)
	@Column(name = "source_type", nullable = false)
	private ExperimentSourceType sourceType;

	@Enumerated(EnumType.STRING)
	@JdbcTypeCode(SqlTypes.NAMED_ENUM)
	@Column(name = "status", nullable = false)
	private ExperimentProgramStatus status;

	@Column(name = "estimated_minutes_min", nullable = false)
	private short estimatedMinutesMin;

	@Column(name = "estimated_minutes_max", nullable = false)
	private short estimatedMinutesMax;

	@Column(name = "is_featured", nullable = false)
	private boolean featured;

	@Column(name = "is_beginner", nullable = false)
	private boolean beginner;

	@Column(name = "display_order", nullable = false)
	private int displayOrder;

	// 다른 experiment_programs row를 가리키는 이력 정보라 연관관계 대신 원문 UUID만 보관한다(Quote와 동일한 이유).
	@Column(name = "supersedes_program_id", updatable = false)
	private UUID supersedesProgramId;

	// admin_users는 별도 신원 체계(com.naroom.api.admin)라 엔티티 연관관계 대신 원문 UUID만 보관한다.
	// 초기 시드 데이터는 관리자가 만든 게 아니라 NULL일 수 있다.
	@Column(name = "created_by_admin_id", updatable = false)
	private UUID createdByAdminId;

	@CreationTimestamp
	@Column(name = "created_at", nullable = false, updatable = false)
	private Instant createdAt;

	@UpdateTimestamp
	@Column(name = "updated_at", nullable = false)
	private Instant updatedAt;

	@Version
	@Column(name = "version", nullable = false)
	private Long version;

	protected ExperimentProgram() {
	}

	private ExperimentProgram(
			String code,
			int contentVersion,
			ExperimentTopic primaryTopic,
			String title,
			String description,
			short durationDays,
			ExperimentSourceType sourceType,
			short estimatedMinutesMin,
			short estimatedMinutesMax,
			boolean featured,
			boolean beginner,
			int displayOrder,
			UUID supersedesProgramId,
			UUID createdByAdminId) {
		this.code = code;
		this.contentVersion = contentVersion;
		this.primaryTopic = primaryTopic;
		this.title = title;
		this.description = description;
		this.durationDays = durationDays;
		this.sourceType = sourceType;
		this.status = ExperimentProgramStatus.DRAFT;
		this.estimatedMinutesMin = estimatedMinutesMin;
		this.estimatedMinutesMax = estimatedMinutesMax;
		this.featured = featured;
		this.beginner = beginner;
		this.displayOrder = displayOrder;
		this.supersedesProgramId = supersedesProgramId;
		this.createdByAdminId = createdByAdminId;
	}

	public static ExperimentProgram create(
			String code,
			int contentVersion,
			ExperimentTopic primaryTopic,
			String title,
			String description,
			short durationDays,
			ExperimentSourceType sourceType,
			short estimatedMinutesMin,
			short estimatedMinutesMax,
			boolean featured,
			boolean beginner,
			int displayOrder,
			UUID supersedesProgramId,
			UUID createdByAdminId) {
		return new ExperimentProgram(
				code, contentVersion, primaryTopic, title, description, durationDays, sourceType,
				estimatedMinutesMin, estimatedMinutesMax, featured, beginner, displayOrder,
				supersedesProgramId, createdByAdminId);
	}

	// DRAFT 상태에서만 그대로 수정한다 - 이미 PUBLISHED된 버전은 절대 UPDATE하지 않는다(§8.5).
	public void updateDraft(
			ExperimentTopic primaryTopic,
			String title,
			String description,
			short durationDays,
			ExperimentSourceType sourceType,
			short estimatedMinutesMin,
			short estimatedMinutesMax,
			boolean featured,
			boolean beginner,
			int displayOrder) {
		this.primaryTopic = primaryTopic;
		this.title = title;
		this.description = description;
		this.durationDays = durationDays;
		this.sourceType = sourceType;
		this.estimatedMinutesMin = estimatedMinutesMin;
		this.estimatedMinutesMax = estimatedMinutesMax;
		this.featured = featured;
		this.beginner = beginner;
		this.displayOrder = displayOrder;
	}

	public void publish() {
		this.status = ExperimentProgramStatus.PUBLISHED;
	}

	public void archive() {
		this.status = ExperimentProgramStatus.ARCHIVED;
	}

	public UUID getId() {
		return id;
	}

	public String getCode() {
		return code;
	}

	public int getContentVersion() {
		return contentVersion;
	}

	public ExperimentTopic getPrimaryTopic() {
		return primaryTopic;
	}

	public String getTitle() {
		return title;
	}

	public String getDescription() {
		return description;
	}

	public short getDurationDays() {
		return durationDays;
	}

	public ExperimentSourceType getSourceType() {
		return sourceType;
	}

	public ExperimentProgramStatus getStatus() {
		return status;
	}

	public short getEstimatedMinutesMin() {
		return estimatedMinutesMin;
	}

	public short getEstimatedMinutesMax() {
		return estimatedMinutesMax;
	}

	public boolean isFeatured() {
		return featured;
	}

	public boolean isBeginner() {
		return beginner;
	}

	public int getDisplayOrder() {
		return displayOrder;
	}

	public UUID getSupersedesProgramId() {
		return supersedesProgramId;
	}

	public UUID getCreatedByAdminId() {
		return createdByAdminId;
	}

	public Instant getCreatedAt() {
		return createdAt;
	}

	public Instant getUpdatedAt() {
		return updatedAt;
	}

	public Long getVersion() {
		return version;
	}

}
