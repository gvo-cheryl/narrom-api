package com.naroom.api.experiment.domain.entity;

import jakarta.persistence.Embeddable;

import java.io.Serializable;
import java.util.Objects;
import java.util.UUID;

@Embeddable
public class ExperimentProgramMissionId implements Serializable {

	private UUID programId;
	private short dayNumber;

	protected ExperimentProgramMissionId() {
	}

	public ExperimentProgramMissionId(UUID programId, short dayNumber) {
		this.programId = programId;
		this.dayNumber = dayNumber;
	}

	public UUID getProgramId() {
		return programId;
	}

	public short getDayNumber() {
		return dayNumber;
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) {
			return true;
		}
		if (!(o instanceof ExperimentProgramMissionId that)) {
			return false;
		}
		return dayNumber == that.dayNumber && Objects.equals(programId, that.programId);
	}

	@Override
	public int hashCode() {
		return Objects.hash(programId, dayNumber);
	}

}
