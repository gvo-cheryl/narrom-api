package com.naroom.api.checkin.domain.entity;

import com.naroom.api.record.domain.entity.Tag;
import jakarta.persistence.Column;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.MapsId;
import jakarta.persistence.Table;

import java.time.Instant;

@Entity
@Table(name = "check_in_emotions")
public class CheckInEmotion {

	@EmbeddedId
	private CheckInEmotionId id;

	@ManyToOne(fetch = FetchType.LAZY)
	@MapsId("checkInId")
	@JoinColumn(name = "check_in_id", nullable = false, updatable = false)
	private CheckIn checkIn;

	@ManyToOne(fetch = FetchType.LAZY)
	@MapsId("tagId")
	@JoinColumn(name = "tag_id", nullable = false, updatable = false)
	private Tag tag;

	@Column(name = "selected_at", nullable = false, updatable = false)
	private Instant selectedAt;

	protected CheckInEmotion() {
	}

	private CheckInEmotion(CheckIn checkIn, Tag tag) {
		this.checkIn = checkIn;
		this.tag = tag;
		this.id = new CheckInEmotionId(checkIn.getId(), tag.getId());
		this.selectedAt = Instant.now();
	}

	public static CheckInEmotion select(CheckIn checkIn, Tag tag) {
		return new CheckInEmotion(checkIn, tag);
	}

	public CheckInEmotionId getId() {
		return id;
	}

	public CheckIn getCheckIn() {
		return checkIn;
	}

	public Tag getTag() {
		return tag;
	}

	public Instant getSelectedAt() {
		return selectedAt;
	}

}
