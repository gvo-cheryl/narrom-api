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
import java.util.UUID;

// (provider, provider_user_id) unique: 한 소셜 계정은 하나의 Member에만 연결된다.
@Entity
@Table(
		name = "social_identities",
		uniqueConstraints = @UniqueConstraint(columnNames = {"provider", "provider_user_id"}))
public class SocialIdentity {

	@Id
	@GeneratedValue(strategy = GenerationType.UUID)
	@Column(name = "id", nullable = false, updatable = false)
	private UUID id;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "member_id", nullable = false, updatable = false)
	private Member member;

	@Enumerated(EnumType.STRING)
	@JdbcTypeCode(SqlTypes.NAMED_ENUM)
	@Column(name = "provider", nullable = false, updatable = false)
	private SocialProvider provider;

	@Column(name = "provider_user_id", nullable = false, length = 255, updatable = false)
	private String providerUserId;

	// 카카오가 이메일을 안 줄 수 있어 nullable. 동일 이메일만으로 기존 회원과 자동 병합하지 않는다.
	@Column(name = "email", length = 320)
	private String email;

	@Column(name = "email_verified", nullable = false)
	private boolean emailVerified;

	@Column(name = "profile_name", length = 100)
	private String profileName;

	@Column(name = "profile_image_url")
	private String profileImageUrl;

	@Enumerated(EnumType.STRING)
	@JdbcTypeCode(SqlTypes.NAMED_ENUM)
	@Column(name = "status", nullable = false)
	private IdentityStatus status;

	// createdAt(행 생성 시각)과 별개로 "최초 연결 시각"이라는 의미를 갖는다. 지금은 항상 같은 값이지만 개념이 다르다.
	@Column(name = "connected_at", nullable = false, updatable = false)
	private Instant connectedAt;

	@Column(name = "last_login_at")
	private Instant lastLoginAt;

	@Column(name = "disconnected_at")
	private Instant disconnectedAt;

	@CreationTimestamp
	@Column(name = "created_at", nullable = false, updatable = false)
	private Instant createdAt;

	@UpdateTimestamp
	@Column(name = "updated_at", nullable = false)
	private Instant updatedAt;

	protected SocialIdentity() {
	}

	private SocialIdentity(Member member, SocialProvider provider, String providerUserId) {
		this.member = member;
		this.provider = provider;
		this.providerUserId = providerUserId;
		this.status = IdentityStatus.ACTIVE;
		this.emailVerified = false;
		this.connectedAt = Instant.now();
	}

	public static SocialIdentity connect(Member member, SocialProvider provider, String providerUserId) {
		return new SocialIdentity(member, provider, providerUserId);
	}

	public UUID getId() {
		return id;
	}

	public Member getMember() {
		return member;
	}

	public SocialProvider getProvider() {
		return provider;
	}

	public String getProviderUserId() {
		return providerUserId;
	}

	public String getEmail() {
		return email;
	}

	public boolean isEmailVerified() {
		return emailVerified;
	}

	public String getProfileName() {
		return profileName;
	}

	public String getProfileImageUrl() {
		return profileImageUrl;
	}

	public IdentityStatus getStatus() {
		return status;
	}

	public Instant getConnectedAt() {
		return connectedAt;
	}

	public Instant getLastLoginAt() {
		return lastLoginAt;
	}

	public Instant getDisconnectedAt() {
		return disconnectedAt;
	}

	public Instant getCreatedAt() {
		return createdAt;
	}

	public Instant getUpdatedAt() {
		return updatedAt;
	}

}
