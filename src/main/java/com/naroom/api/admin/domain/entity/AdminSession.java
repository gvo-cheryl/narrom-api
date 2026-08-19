package com.naroom.api.admin.domain.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import java.time.Duration;
import java.time.Instant;
import java.util.UUID;

// 회원 auth_sessions와 완전히 분리된 관리자 전용 세션. 원문 토큰은 저장하지 않고 해시만 둔다.
@Entity
@Table(name = "admin_sessions")
public class AdminSession {

	@Id
	@GeneratedValue(strategy = GenerationType.UUID)
	@Column(name = "id", nullable = false, updatable = false)
	private UUID id;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "admin_user_id", nullable = false, updatable = false)
	private AdminUser adminUser;

	@Column(name = "session_token_hash", nullable = false, length = 128, unique = true)
	private String sessionTokenHash;

	@Column(name = "issued_at", nullable = false, updatable = false)
	private Instant issuedAt;

	@Column(name = "last_used_at", nullable = false)
	private Instant lastUsedAt;

	@Column(name = "expires_at", nullable = false)
	private Instant expiresAt;

	@Column(name = "revoked_at")
	private Instant revokedAt;

	@Column(name = "ip_hash", length = 128)
	private String ipHash;

	@Column(name = "user_agent_summary", length = 255)
	private String userAgentSummary;

	protected AdminSession() {
	}

	private AdminSession(AdminUser adminUser, String sessionTokenHash, Instant expiresAt, String ipHash, String userAgentSummary) {
		this.adminUser = adminUser;
		this.sessionTokenHash = sessionTokenHash;
		this.issuedAt = Instant.now();
		this.lastUsedAt = this.issuedAt;
		this.expiresAt = expiresAt;
		this.ipHash = ipHash;
		this.userAgentSummary = userAgentSummary;
	}

	public static AdminSession issue(
			AdminUser adminUser, String sessionTokenHash, Instant expiresAt, String ipHash, String userAgentSummary) {
		return new AdminSession(adminUser, sessionTokenHash, expiresAt, ipHash, userAgentSummary);
	}

	// idle timeout 판정: 요청마다 갱신한다. absolute timeout(expiresAt)은 여기서 늘리지 않는다.
	public void markUsed() {
		this.lastUsedAt = Instant.now();
	}

	public void revoke() {
		this.revokedAt = Instant.now();
	}

	public boolean isActive(Instant now, Duration idleTimeout) {
		if (revokedAt != null) {
			return false;
		}
		if (expiresAt.isBefore(now)) {
			return false;
		}
		return !lastUsedAt.plus(idleTimeout).isBefore(now);
	}

	public UUID getId() {
		return id;
	}

	public AdminUser getAdminUser() {
		return adminUser;
	}

	public String getSessionTokenHash() {
		return sessionTokenHash;
	}

	public Instant getIssuedAt() {
		return issuedAt;
	}

	public Instant getLastUsedAt() {
		return lastUsedAt;
	}

	public Instant getExpiresAt() {
		return expiresAt;
	}

	public Instant getRevokedAt() {
		return revokedAt;
	}

	public String getIpHash() {
		return ipHash;
	}

	public String getUserAgentSummary() {
		return userAgentSummary;
	}

}
