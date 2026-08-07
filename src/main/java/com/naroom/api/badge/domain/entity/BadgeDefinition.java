package com.naroom.api.badge.domain.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.UpdateTimestamp;
import org.hibernate.type.SqlTypes;

import java.time.Instant;
import java.util.UUID;

// Beta 1은 애플리케이션 시드 데이터(V19)로만 채운다 - 관리용 CRUD API는 두지 않는다.
@Entity
@Table(name = "badge_definitions")
public class BadgeDefinition {

	@Id
	@GeneratedValue(strategy = GenerationType.UUID)
	@Column(name = "id", nullable = false, updatable = false)
	private UUID id;

	@Enumerated(EnumType.STRING)
	@JdbcTypeCode(SqlTypes.NAMED_ENUM)
	@Column(name = "code", nullable = false, updatable = false)
	private BadgeCode code;

	@Enumerated(EnumType.STRING)
	@JdbcTypeCode(SqlTypes.NAMED_ENUM)
	@Column(name = "category", nullable = false, updatable = false)
	private BadgeCategory category;

	@Column(name = "title", nullable = false, length = 80, updatable = false)
	private String title;

	@Column(name = "description", nullable = false, length = 200, updatable = false)
	private String description;

	@Column(name = "display_order", nullable = false, updatable = false)
	private short displayOrder;

	@CreationTimestamp
	@Column(name = "created_at", nullable = false, updatable = false)
	private Instant createdAt;

	@UpdateTimestamp
	@Column(name = "updated_at", nullable = false)
	private Instant updatedAt;

	protected BadgeDefinition() {
	}

	public UUID getId() {
		return id;
	}

	public BadgeCode getCode() {
		return code;
	}

	public BadgeCategory getCategory() {
		return category;
	}

	public String getTitle() {
		return title;
	}

	public String getDescription() {
		return description;
	}

	public short getDisplayOrder() {
		return displayOrder;
	}

	public Instant getCreatedAt() {
		return createdAt;
	}

	public Instant getUpdatedAt() {
		return updatedAt;
	}

}
