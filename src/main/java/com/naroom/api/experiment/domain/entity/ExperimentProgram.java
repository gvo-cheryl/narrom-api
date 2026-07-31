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

	@Column(name = "content_version", nullable = false)
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

	@CreationTimestamp
	@Column(name = "created_at", nullable = false, updatable = false)
	private Instant createdAt;

	@UpdateTimestamp
	@Column(name = "updated_at", nullable = false)
	private Instant updatedAt;

	protected ExperimentProgram() {
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

	public Instant getCreatedAt() {
		return createdAt;
	}

	public Instant getUpdatedAt() {
		return updatedAt;
	}

}
