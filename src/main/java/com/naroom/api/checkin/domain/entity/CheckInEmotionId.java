package com.naroom.api.checkin.domain.entity;

import jakarta.persistence.Embeddable;

import java.io.Serializable;
import java.util.Objects;
import java.util.UUID;

@Embeddable
public class CheckInEmotionId implements Serializable {

	private UUID checkInId;
	private UUID tagId;

	protected CheckInEmotionId() {
	}

	public CheckInEmotionId(UUID checkInId, UUID tagId) {
		this.checkInId = checkInId;
		this.tagId = tagId;
	}

	public UUID getCheckInId() {
		return checkInId;
	}

	public UUID getTagId() {
		return tagId;
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) {
			return true;
		}
		if (!(o instanceof CheckInEmotionId that)) {
			return false;
		}
		return Objects.equals(checkInId, that.checkInId) && Objects.equals(tagId, that.tagId);
	}

	@Override
	public int hashCode() {
		return Objects.hash(checkInId, tagId);
	}

}
