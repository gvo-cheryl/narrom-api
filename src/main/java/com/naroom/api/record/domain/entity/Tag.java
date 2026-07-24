package com.naroom.api.record.domain.entity;

import com.naroom.api.account.domain.entity.Member;
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
import java.util.UUID;

// scope와 ownerMember는 DB CHECK 제약(ck_tags_1)으로 묶여 있다: SYSTEM 태그는 ownerMember가 없고,
// USER 태그는 반드시 소유 회원이 있다. createSystemTag/createUserTag로만 생성해 이 규칙을 강제한다.
@Entity
@Table(name = "tags")
public class Tag {

	@Id
	@GeneratedValue(strategy = GenerationType.UUID)
	@Column(name = "id", nullable = false, updatable = false)
	private UUID id;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "owner_member_id", updatable = false)
	private Member ownerMember;

	@Enumerated(EnumType.STRING)
	@JdbcTypeCode(SqlTypes.NAMED_ENUM)
	@Column(name = "scope", nullable = false, updatable = false)
	private TagScope scope;

	@Enumerated(EnumType.STRING)
	@JdbcTypeCode(SqlTypes.NAMED_ENUM)
	@Column(name = "category", nullable = false)
	private TagCategory category;

	@Column(name = "name", nullable = false, length = 80)
	private String name;

	@Column(name = "normalized_name", nullable = false, length = 80)
	private String normalizedName;

	@Column(name = "active", nullable = false)
	private boolean active;

	@CreationTimestamp
	@Column(name = "created_at", nullable = false, updatable = false)
	private Instant createdAt;

	@UpdateTimestamp
	@Column(name = "updated_at", nullable = false)
	private Instant updatedAt;

	protected Tag() {
	}

	private Tag(Member ownerMember, TagScope scope, TagCategory category, String name, String normalizedName) {
		this.ownerMember = ownerMember;
		this.scope = scope;
		this.category = category;
		this.name = name;
		this.normalizedName = normalizedName;
		this.active = true;
	}

	public static Tag createSystemTag(TagCategory category, String name, String normalizedName) {
		return new Tag(null, TagScope.SYSTEM, category, name, normalizedName);
	}

	public static Tag createUserTag(Member ownerMember, TagCategory category, String name, String normalizedName) {
		return new Tag(ownerMember, TagScope.USER, category, name, normalizedName);
	}

	public void deactivate() {
		this.active = false;
	}

	public UUID getId() {
		return id;
	}

	public Member getOwnerMember() {
		return ownerMember;
	}

	public TagScope getScope() {
		return scope;
	}

	public TagCategory getCategory() {
		return category;
	}

	public String getName() {
		return name;
	}

	public String getNormalizedName() {
		return normalizedName;
	}

	public boolean isActive() {
		return active;
	}

	public Instant getCreatedAt() {
		return createdAt;
	}

	public Instant getUpdatedAt() {
		return updatedAt;
	}

}
