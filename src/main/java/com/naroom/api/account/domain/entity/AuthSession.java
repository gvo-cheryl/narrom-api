package com.naroom.api.account.domain.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "auth_sessions")
public class AuthSession {

	@Id
	@GeneratedValue(strategy = GenerationType.UUID)
	@Column(name = "id", nullable = false, updatable = false)
	private UUID id;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "member_id", nullable = false, updatable = false)
	private Member member;

	// ON DELETE SET NULL: 기기 레코드가 삭제돼도 세션 자체는 남는다.
	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "device_installation_id")
	private DeviceInstallation deviceInstallation;

	@Column(name = "refresh_token_hash", nullable = false, length = 128, unique = true)
	private String refreshTokenHash;

	@Column(name = "issued_at", nullable = false, updatable = false)
	private Instant issuedAt;

	@Column(name = "expires_at", nullable = false)
	private Instant expiresAt;

	@Column(name = "last_used_at")
	private Instant lastUsedAt;

	@Column(name = "revoked_at")
	private Instant revokedAt;

	@Column(name = "revoke_reason", length = 50)
	private String revokeReason;

	protected AuthSession() {
	}

	private AuthSession(
			Member member,
			DeviceInstallation deviceInstallation,
			String refreshTokenHash,
			Instant expiresAt) {
		this.member = member;
		this.deviceInstallation = deviceInstallation;
		this.refreshTokenHash = refreshTokenHash;
		this.issuedAt = Instant.now();
		this.expiresAt = expiresAt;
	}

	public static AuthSession issue(
			Member member,
			DeviceInstallation deviceInstallation,
			String refreshTokenHash,
			Instant expiresAt) {
		return new AuthSession(member, deviceInstallation, refreshTokenHash, expiresAt);
	}

	/** 재발급 시 같은 세션 행을 유지하고 refresh_token_hash만 교체한다(authentication.md 토큰 정책). */
	public void rotate(String newRefreshTokenHash, Instant newExpiresAt) {
		this.refreshTokenHash = newRefreshTokenHash;
		this.expiresAt = newExpiresAt;
		this.lastUsedAt = Instant.now();
	}

	// reason은 자유 varchar(50)이지만 실제로는 LOGOUT/WITHDRAWAL/SECURITY 등 정해진 값만 쓴다(Java enum 아님, ERD 원본 그대로).
	public void revoke(String reason) {
		this.revokedAt = Instant.now();
		this.revokeReason = reason;
	}

	public UUID getId() {
		return id;
	}

	public Member getMember() {
		return member;
	}

	public DeviceInstallation getDeviceInstallation() {
		return deviceInstallation;
	}

	public String getRefreshTokenHash() {
		return refreshTokenHash;
	}

	public Instant getIssuedAt() {
		return issuedAt;
	}

	public Instant getExpiresAt() {
		return expiresAt;
	}

	public Instant getLastUsedAt() {
		return lastUsedAt;
	}

	public Instant getRevokedAt() {
		return revokedAt;
	}

	public String getRevokeReason() {
		return revokeReason;
	}

}
