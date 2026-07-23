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
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "device_installations")
public class DeviceInstallation {

	@Id
	@GeneratedValue(strategy = GenerationType.UUID)
	@Column(name = "id", nullable = false, updatable = false)
	private UUID id;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "member_id", nullable = false, updatable = false)
	private Member member;

	@Column(name = "installation_key", nullable = false, length = 255, updatable = false)
	private String installationKey;

	@Column(name = "platform", nullable = false, length = 30)
	private String platform;

	// pgcrypto가 아니라 애플리케이션 계층에서 암호화한 값을 저장한다.
	@Column(name = "push_token_ciphertext")
	private String pushTokenCiphertext;

	@Column(name = "app_version", length = 30)
	private String appVersion;

	@Column(name = "last_seen_at", nullable = false)
	private Instant lastSeenAt;

	@Column(name = "revoked_at")
	private Instant revokedAt;

	@CreationTimestamp
	@Column(name = "created_at", nullable = false, updatable = false)
	private Instant createdAt;

	@UpdateTimestamp
	@Column(name = "updated_at", nullable = false)
	private Instant updatedAt;

	protected DeviceInstallation() {
	}

	private DeviceInstallation(Member member, String installationKey, String platform, String appVersion) {
		this.member = member;
		this.installationKey = installationKey;
		this.platform = platform;
		this.appVersion = appVersion;
		this.lastSeenAt = Instant.now();
	}

	public static DeviceInstallation register(Member member, String installationKey, String platform, String appVersion) {
		return new DeviceInstallation(member, installationKey, platform, appVersion);
	}

	// 로그인/토큰 재발급 등 기기가 서버와 통신할 때마다 호출된다.
	public void markSeen(String appVersion) {
		this.appVersion = appVersion;
		this.lastSeenAt = Instant.now();
	}

	public UUID getId() {
		return id;
	}

	public Member getMember() {
		return member;
	}

	public String getInstallationKey() {
		return installationKey;
	}

	public String getPlatform() {
		return platform;
	}

	public String getPushTokenCiphertext() {
		return pushTokenCiphertext;
	}

	public String getAppVersion() {
		return appVersion;
	}

	public Instant getLastSeenAt() {
		return lastSeenAt;
	}

	public Instant getRevokedAt() {
		return revokedAt;
	}

	public Instant getCreatedAt() {
		return createdAt;
	}

	public Instant getUpdatedAt() {
		return updatedAt;
	}

}
