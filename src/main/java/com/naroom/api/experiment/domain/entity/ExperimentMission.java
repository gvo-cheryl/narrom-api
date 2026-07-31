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

// 재사용 가능한 하루 미션 원본. 코스 시작 시 UserProgramMission에 문구가 스냅샷으로 복사되므로,
// 여기 내용이 나중에 수정되어도 이미 진행 중인 사용자의 기록은 바뀌지 않는다(§12.2).
@Entity
@Table(name = "experiment_missions")
public class ExperimentMission {

	@Id
	@GeneratedValue(strategy = GenerationType.UUID)
	@Column(name = "id", nullable = false, updatable = false)
	private UUID id;

	@Column(name = "code", nullable = false, updatable = false, length = 80)
	private String code;

	@Column(name = "content_version", nullable = false)
	private int contentVersion;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "topic_id", nullable = false)
	private ExperimentTopic topic;

	@Column(name = "title", nullable = false, length = 120)
	private String title;

	@Column(name = "description", nullable = false)
	private String description;

	@Column(name = "instruction", nullable = false)
	private String instruction;

	@Enumerated(EnumType.STRING)
	@JdbcTypeCode(SqlTypes.NAMED_ENUM)
	@Column(name = "mission_type", nullable = false)
	private ExperimentMissionType missionType;

	// L03/L04 등 프론트 렌더링 변형을 가리키는 개방형 값이다(TEXT, ACTION_REFLECTION,
	// FACT_AND_INTERPRETATION, BULLET_LIST, COURSE_REVIEW 등) - 아직 닫힌 집합으로 확정되지 않아
	// 네이티브 ENUM으로 만들지 않았다.
	@Column(name = "response_type", nullable = false, length = 40)
	private String responseType;

	@Column(name = "estimated_minutes", nullable = false)
	private short estimatedMinutes;

	@Enumerated(EnumType.STRING)
	@JdbcTypeCode(SqlTypes.NAMED_ENUM)
	@Column(name = "emotional_load", nullable = false)
	private ExperimentEmotionalLoad emotionalLoad;

	@Column(name = "reflection_questions", columnDefinition = "jsonb", nullable = false)
	@JdbcTypeCode(SqlTypes.JSON)
	private String reflectionQuestions;

	@Column(name = "examples", columnDefinition = "jsonb", nullable = false)
	@JdbcTypeCode(SqlTypes.JSON)
	private String examples;

	@Column(name = "response_schema", columnDefinition = "jsonb", nullable = false)
	@JdbcTypeCode(SqlTypes.JSON)
	private String responseSchema;

	@Column(name = "safety_note")
	private String safetyNote;

	@Column(name = "active", nullable = false)
	private boolean active;

	@CreationTimestamp
	@Column(name = "created_at", nullable = false, updatable = false)
	private Instant createdAt;

	@UpdateTimestamp
	@Column(name = "updated_at", nullable = false)
	private Instant updatedAt;

	protected ExperimentMission() {
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

	public ExperimentTopic getTopic() {
		return topic;
	}

	public String getTitle() {
		return title;
	}

	public String getDescription() {
		return description;
	}

	public String getInstruction() {
		return instruction;
	}

	public ExperimentMissionType getMissionType() {
		return missionType;
	}

	public String getResponseType() {
		return responseType;
	}

	public short getEstimatedMinutes() {
		return estimatedMinutes;
	}

	public ExperimentEmotionalLoad getEmotionalLoad() {
		return emotionalLoad;
	}

	public String getReflectionQuestions() {
		return reflectionQuestions;
	}

	public String getExamples() {
		return examples;
	}

	public String getResponseSchema() {
		return responseSchema;
	}

	public String getSafetyNote() {
		return safetyNote;
	}

	public boolean isActive() {
		return active;
	}

	public Instant getCreatedAt() {
		return createdAt;
	}

	public Instant getUpdatedAt() {
		return updatedAt;
	}

}
