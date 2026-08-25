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
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.Instant;
import java.util.UUID;

// 회원 auth_sessions·admin_sessions와 완전히 분리된 모바일 미리보기 전용 세션. 원문 토큰은 저장하지 않고
// 해시만 둔다(admin_sessions와 같은 원칙).
@Entity
@Table(name = "preview_sessions")
public class PreviewSession {

	@Id
	@GeneratedValue(strategy = GenerationType.UUID)
	@Column(name = "id", nullable = false, updatable = false)
	private UUID id;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "admin_user_id", nullable = false, updatable = false)
	private AdminUser adminUser;

	@Column(name = "token_hash", nullable = false, length = 128, unique = true)
	private String tokenHash;

	// 콘텐츠 종류별 미리볼 DRAFT 버전 id를 담은 JSON(예: {"quote": "<uuid>"}). 직렬화된 문자열 그대로 두고
	// 실제 조회 시점(D-3/D-4)에 파싱한다.
	@Column(name = "selected_content_versions", nullable = false, columnDefinition = "jsonb")
	@JdbcTypeCode(SqlTypes.JSON)
	private String selectedContentVersions;

	@Column(name = "scenario_key", length = 60)
	private String scenarioKey;

	@CreationTimestamp
	@Column(name = "created_at", nullable = false, updatable = false)
	private Instant createdAt;

	@Column(name = "expires_at", nullable = false)
	private Instant expiresAt;

	@Column(name = "revoked_at")
	private Instant revokedAt;

	protected PreviewSession() {
	}

	private PreviewSession(
			AdminUser adminUser, String tokenHash, String selectedContentVersions, String scenarioKey, Instant expiresAt) {
		this.adminUser = adminUser;
		this.tokenHash = tokenHash;
		this.selectedContentVersions = selectedContentVersions;
		this.scenarioKey = scenarioKey;
		this.expiresAt = expiresAt;
	}

	public static PreviewSession issue(
			AdminUser adminUser, String tokenHash, String selectedContentVersions, String scenarioKey, Instant expiresAt) {
		return new PreviewSession(adminUser, tokenHash, selectedContentVersions, scenarioKey, expiresAt);
	}

	public void revoke() {
		this.revokedAt = Instant.now();
	}

	public boolean isActive(Instant now) {
		if (revokedAt != null) {
			return false;
		}
		return expiresAt.isAfter(now);
	}

	public UUID getId() {
		return id;
	}

	public AdminUser getAdminUser() {
		return adminUser;
	}

	public String getTokenHash() {
		return tokenHash;
	}

	public String getSelectedContentVersions() {
		return selectedContentVersions;
	}

	public String getScenarioKey() {
		return scenarioKey;
	}

	public Instant getCreatedAt() {
		return createdAt;
	}

	public Instant getExpiresAt() {
		return expiresAt;
	}

	public Instant getRevokedAt() {
		return revokedAt;
	}

}
