package com.naroom.api.checkin.domain.entity;

import com.naroom.api.account.domain.entity.Member;
import com.naroom.api.record.domain.entity.Entry;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

// entry는 이 체크인을 LifeTime 타임라인에 CHECK_IN 기록으로 보여주기 위한 봉투다(reference 스키마 기준
// entries.id NOT NULL UNIQUE). CheckInService가 Entry.create(CHECK_IN)와 함께 생성해서만 존재한다.
// UNIQUE(member_id, check_in_date)는 DB 제약: 같은 날 재요청은 새 행이 아니라 upsert로 처리해야 한다.
@Entity
@Table(name = "check_ins")
public class CheckIn {

	@Id
	@GeneratedValue(strategy = GenerationType.UUID)
	@Column(name = "id", nullable = false, updatable = false)
	private UUID id;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "member_id", nullable = false, updatable = false)
	private Member member;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "entry_id", nullable = false, updatable = false)
	private Entry entry;

	@Column(name = "check_in_date", nullable = false, updatable = false)
	private LocalDate checkInDate;

	@Column(name = "emotion_intensity")
	private Short emotionIntensity;

	@Column(name = "energy_level")
	private Short energyLevel;

	@Column(name = "memorable_event")
	private String memorableEvent;

	@Column(name = "gratitude_note")
	private String gratitudeNote;

	@Column(name = "current_need")
	private String currentNeed;

	@Column(name = "free_note")
	private String freeNote;

	@CreationTimestamp
	@Column(name = "created_at", nullable = false, updatable = false)
	private Instant createdAt;

	@UpdateTimestamp
	@Column(name = "updated_at", nullable = false)
	private Instant updatedAt;

	@Version
	@Column(name = "version", nullable = false)
	private Long version;

	protected CheckIn() {
	}

	private CheckIn(Member member, Entry entry, LocalDate checkInDate) {
		this.member = member;
		this.entry = entry;
		this.checkInDate = checkInDate;
	}

	public static CheckIn create(Member member, Entry entry, LocalDate checkInDate) {
		return new CheckIn(member, entry, checkInDate);
	}

	public void update(
			Short emotionIntensity, Short energyLevel, String memorableEvent, String gratitudeNote,
			String currentNeed, String freeNote) {
		this.emotionIntensity = emotionIntensity;
		this.energyLevel = energyLevel;
		this.memorableEvent = memorableEvent;
		this.gratitudeNote = gratitudeNote;
		this.currentNeed = currentNeed;
		this.freeNote = freeNote;
	}

	public UUID getId() {
		return id;
	}

	public Member getMember() {
		return member;
	}

	public Entry getEntry() {
		return entry;
	}

	public LocalDate getCheckInDate() {
		return checkInDate;
	}

	public Short getEmotionIntensity() {
		return emotionIntensity;
	}

	public Short getEnergyLevel() {
		return energyLevel;
	}

	public String getMemorableEvent() {
		return memorableEvent;
	}

	public String getGratitudeNote() {
		return gratitudeNote;
	}

	public String getCurrentNeed() {
		return currentNeed;
	}

	public String getFreeNote() {
		return freeNote;
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
