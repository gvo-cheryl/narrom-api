package com.naroom.api.lifetime.domain.entity;

import jakarta.persistence.Embeddable;

import java.io.Serializable;
import java.util.Objects;
import java.util.UUID;

@Embeddable
public class PeriodReflectionEntryId implements Serializable {

	private UUID periodReflectionId;
	private UUID entryId;

	protected PeriodReflectionEntryId() {
	}

	public PeriodReflectionEntryId(UUID periodReflectionId, UUID entryId) {
		this.periodReflectionId = periodReflectionId;
		this.entryId = entryId;
	}

	public UUID getPeriodReflectionId() {
		return periodReflectionId;
	}

	public UUID getEntryId() {
		return entryId;
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) {
			return true;
		}
		if (!(o instanceof PeriodReflectionEntryId that)) {
			return false;
		}
		return Objects.equals(periodReflectionId, that.periodReflectionId) && Objects.equals(entryId, that.entryId);
	}

	@Override
	public int hashCode() {
		return Objects.hash(periodReflectionId, entryId);
	}

}
