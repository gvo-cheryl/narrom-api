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
import org.hibernate.type.SqlTypes;

import java.time.Instant;
import java.util.UUID;

// 미션 교체 이력. from/to가 nullable인 이유는 참조된 원본 미션이 삭제돼도(ON DELETE SET NULL)
// 이력 자체는 남기기 위함이다.
@Entity
@Table(name = "user_program_mission_replacements")
public class UserProgramMissionReplacement {

	@Id
	@GeneratedValue(strategy = GenerationType.UUID)
	@Column(name = "id", nullable = false, updatable = false)
	private UUID id;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "user_program_mission_id", nullable = false, updatable = false)
	private UserProgramMission userProgramMission;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "from_mission_id")
	private ExperimentMission fromMission;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "to_mission_id")
	private ExperimentMission toMission;

	@Enumerated(EnumType.STRING)
	@JdbcTypeCode(SqlTypes.NAMED_ENUM)
	@Column(name = "reason_code")
	private MissionReplacementReasonCode reasonCode;

	@Column(name = "reason_note")
	private String reasonNote;

	@CreationTimestamp
	@Column(name = "replaced_at", nullable = false, updatable = false)
	private Instant replacedAt;

	protected UserProgramMissionReplacement() {
	}

	public UUID getId() {
		return id;
	}

	public UserProgramMission getUserProgramMission() {
		return userProgramMission;
	}

	public ExperimentMission getFromMission() {
		return fromMission;
	}

	public ExperimentMission getToMission() {
		return toMission;
	}

	public MissionReplacementReasonCode getReasonCode() {
		return reasonCode;
	}

	public String getReasonNote() {
		return reasonNote;
	}

	public Instant getReplacedAt() {
		return replacedAt;
	}

}
