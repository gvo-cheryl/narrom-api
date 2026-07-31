package com.naroom.api.experiment.domain.entity;

import jakarta.persistence.Column;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.MapsId;
import jakarta.persistence.Table;
import org.hibernate.annotations.CreationTimestamp;

import java.time.Instant;

// 코스 원본의 일차별 미션 구성. 복합키(program_id, day_number)라 서로게이트 id 없이
// ExperimentProgramMissionId를 그대로 쓴다(PeriodReflectionEntry와 동일한 패턴).
@Entity
@Table(name = "experiment_program_missions")
public class ExperimentProgramMission {

	@EmbeddedId
	private ExperimentProgramMissionId id;

	@ManyToOne(fetch = FetchType.LAZY)
	@MapsId("programId")
	@JoinColumn(name = "program_id", nullable = false, updatable = false)
	private ExperimentProgram program;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "mission_id", nullable = false, updatable = false)
	private ExperimentMission mission;

	@Column(name = "is_replaceable", nullable = false)
	private boolean replaceable;

	@Column(name = "replacement_group", length = 50)
	private String replacementGroup;

	@CreationTimestamp
	@Column(name = "created_at", nullable = false, updatable = false)
	private Instant createdAt;

	protected ExperimentProgramMission() {
	}

	private ExperimentProgramMission(
			ExperimentProgram program, ExperimentMission mission, short dayNumber,
			boolean replaceable, String replacementGroup) {
		this.program = program;
		this.mission = mission;
		this.replaceable = replaceable;
		this.replacementGroup = replacementGroup;
		this.id = new ExperimentProgramMissionId(program.getId(), dayNumber);
	}

	public static ExperimentProgramMission of(
			ExperimentProgram program, ExperimentMission mission, short dayNumber,
			boolean replaceable, String replacementGroup) {
		return new ExperimentProgramMission(program, mission, dayNumber, replaceable, replacementGroup);
	}

	public ExperimentProgramMissionId getId() {
		return id;
	}

	public ExperimentProgram getProgram() {
		return program;
	}

	public ExperimentMission getMission() {
		return mission;
	}

	public short getDayNumber() {
		return id.getDayNumber();
	}

	public boolean isReplaceable() {
		return replaceable;
	}

	public String getReplacementGroup() {
		return replacementGroup;
	}

	public Instant getCreatedAt() {
		return createdAt;
	}

}
