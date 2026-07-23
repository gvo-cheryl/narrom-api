package com.naroom.api.account.domain.entity;

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
import jakarta.persistence.UniqueConstraint;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.UpdateTimestamp;
import org.hibernate.type.SqlTypes;

import java.time.Instant;
import java.time.LocalTime;
import java.util.UUID;

@Entity
@Table(
		name = "notification_preferences",
		uniqueConstraints = @UniqueConstraint(columnNames = {"member_id", "notification_type"}))
public class NotificationPreference {

	@Id
	@GeneratedValue(strategy = GenerationType.UUID)
	@Column(name = "id", nullable = false, updatable = false)
	private UUID id;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "member_id", nullable = false, updatable = false)
	private Member member;

	@Enumerated(EnumType.STRING)
	@JdbcTypeCode(SqlTypes.NAMED_ENUM)
	@Column(name = "notification_type", nullable = false, updatable = false)
	private NotificationType notificationType;

	@Column(name = "enabled", nullable = false)
	private boolean enabled;

	// DAILY_QUOTE처럼 요일 지정이 필요 없는 알림 유형에서는 null.
	@Column(name = "local_time")
	private LocalTime localTime;

	// smallint(1=월~7=일, DB CHECK) 컬럼이라 ddl-auto=validate를 통과하려면 Integer가 아니라 Short로 맞춰야 한다.
	@Column(name = "day_of_week")
	private Short dayOfWeek;

	@CreationTimestamp
	@Column(name = "created_at", nullable = false, updatable = false)
	private Instant createdAt;

	@UpdateTimestamp
	@Column(name = "updated_at", nullable = false)
	private Instant updatedAt;

	protected NotificationPreference() {
	}

	private NotificationPreference(Member member, NotificationType notificationType) {
		this.member = member;
		this.notificationType = notificationType;
		this.enabled = false;
	}

	public static NotificationPreference createDisabled(Member member, NotificationType notificationType) {
		return new NotificationPreference(member, notificationType);
	}

	public void update(boolean enabled, LocalTime localTime, Short dayOfWeek) {
		this.enabled = enabled;
		this.localTime = localTime;
		this.dayOfWeek = dayOfWeek;
	}

	public UUID getId() {
		return id;
	}

	public Member getMember() {
		return member;
	}

	public NotificationType getNotificationType() {
		return notificationType;
	}

	public boolean isEnabled() {
		return enabled;
	}

	public LocalTime getLocalTime() {
		return localTime;
	}

	public Short getDayOfWeek() {
		return dayOfWeek;
	}

	public Instant getCreatedAt() {
		return createdAt;
	}

	public Instant getUpdatedAt() {
		return updatedAt;
	}

}
