package com.naroom.api.experiment.domain.entity;

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
import org.hibernate.annotations.UpdateTimestamp;
import org.hibernate.type.SqlTypes;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

// 일별 시도·휴식·감정·에너지·회고 기록. DB의 실제 FK는 (user_program_mission_id,
// user_experiment_program_id) 복합키로 user_program_missions를 참조해 슬롯이 반드시 자기 코스를
// 통해서만 연결되도록 강제하지만, JPA 매핑은 두 연관을 단일 컬럼 조인으로 단순화했다 - Hibernate
// ddl-auto=validate는 컬럼 단위로만 검증하므로 복합 FK 제약 자체를 Java로 미러링할 필요는 없다.
@Entity
@Table(name = "experiment_mission_records")
public class ExperimentMissionRecord {

	@Id
	@GeneratedValue(strategy = GenerationType.UUID)
	@Column(name = "id", nullable = false, updatable = false)
	private UUID id;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "user_experiment_program_id", nullable = false, updatable = false)
	private UserExperimentProgram userExperimentProgram;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "user_program_mission_id", nullable = false, updatable = false)
	private UserProgramMission userProgramMission;

	@Column(name = "record_date", nullable = false, updatable = false)
	private LocalDate recordDate;

	@Enumerated(EnumType.STRING)
	@JdbcTypeCode(SqlTypes.NAMED_ENUM)
	@Column(name = "attempt_status", nullable = false, updatable = false)
	private ExperimentAttemptStatus attemptStatus;

	@Column(name = "response_text")
	private String responseText;

	@Column(name = "response_data", columnDefinition = "jsonb")
	@JdbcTypeCode(SqlTypes.JSON)
	private String responseData;

	@Column(name = "emotion_data", columnDefinition = "jsonb")
	@JdbcTypeCode(SqlTypes.JSON)
	private String emotionData;

	@Column(name = "energy_level")
	private Short energyLevel;

	@Column(name = "reflection")
	private String reflection;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "entry_id")
	private Entry entry;

	@CreationTimestamp
	@Column(name = "recorded_at", nullable = false, updatable = false)
	private Instant recordedAt;

	@CreationTimestamp
	@Column(name = "created_at", nullable = false, updatable = false)
	private Instant createdAt;

	@UpdateTimestamp
	@Column(name = "updated_at", nullable = false)
	private Instant updatedAt;

	protected ExperimentMissionRecord() {
	}

	public UUID getId() {
		return id;
	}

	public UserExperimentProgram getUserExperimentProgram() {
		return userExperimentProgram;
	}

	public UserProgramMission getUserProgramMission() {
		return userProgramMission;
	}

	public LocalDate getRecordDate() {
		return recordDate;
	}

	public ExperimentAttemptStatus getAttemptStatus() {
		return attemptStatus;
	}

	public String getResponseText() {
		return responseText;
	}

	public String getResponseData() {
		return responseData;
	}

	public String getEmotionData() {
		return emotionData;
	}

	public Short getEnergyLevel() {
		return energyLevel;
	}

	public String getReflection() {
		return reflection;
	}

	public Entry getEntry() {
		return entry;
	}

	public Instant getRecordedAt() {
		return recordedAt;
	}

	public Instant getCreatedAt() {
		return createdAt;
	}

	public Instant getUpdatedAt() {
		return updatedAt;
	}

}
