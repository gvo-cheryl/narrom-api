package com.naroom.api.admin.domain.entity;

import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.UpdateTimestamp;
import org.hibernate.type.SqlTypes;

import java.time.Instant;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

// 회원(members)과 완전히 분리된 별도 신원 체계다. google_sub가 인가 키이며 이메일은 표시용 메타데이터일 뿐이다
// (Admin Web Implementation Spec 17.2). 로그인 성공만으로 이 엔티티가 자동 생성되지 않는다 - bootstrap 또는
// 기존 관리자의 명시적 승인으로만 생성된다.
@Entity
@Table(name = "admin_users")
public class AdminUser {

	@Id
	@GeneratedValue(strategy = GenerationType.UUID)
	@Column(name = "id", nullable = false, updatable = false)
	private UUID id;

	@Column(name = "google_sub", nullable = false, updatable = false, length = 255)
	private String googleSub;

	@Column(name = "email", nullable = false, length = 320)
	private String email;

	@Column(name = "email_verified", nullable = false)
	private boolean emailVerified;

	@Column(name = "display_name", length = 100)
	private String displayName;

	@Enumerated(EnumType.STRING)
	@JdbcTypeCode(SqlTypes.NAMED_ENUM)
	@Column(name = "status", nullable = false)
	private AdminStatus status;

	@ElementCollection(fetch = FetchType.EAGER)
	@CollectionTable(name = "admin_user_roles", joinColumns = @JoinColumn(name = "admin_user_id"))
	@Enumerated(EnumType.STRING)
	@JdbcTypeCode(SqlTypes.NAMED_ENUM)
	@Column(name = "role", nullable = false)
	private Set<AdminRole> roles;

	@Column(name = "approved_by_admin_id")
	private UUID approvedByAdminId;

	@Column(name = "approved_at")
	private Instant approvedAt;

	@Column(name = "last_login_at")
	private Instant lastLoginAt;

	@CreationTimestamp
	@Column(name = "created_at", nullable = false, updatable = false)
	private Instant createdAt;

	@UpdateTimestamp
	@Column(name = "updated_at", nullable = false)
	private Instant updatedAt;

	@Version
	@Column(name = "version", nullable = false)
	private Long version;

	protected AdminUser() {
	}

	// roles를 그대로 참조로 들고 있으면 안 된다 - AdminLoginResolver처럼 다른 엔티티(AdminInvitation)의
	// roles 컬렉션 인스턴스를 그대로 넘기는 호출부가 있으면, 같은 컬렉션 객체가 서로 다른 두 엔티티의
	// @ElementCollection에 동시에 걸려 flush 시 Hibernate가 "Found shared references to a collection"으로 거부한다.
	private AdminUser(String googleSub, String email, boolean emailVerified, String displayName, Set<AdminRole> roles) {
		this.googleSub = googleSub;
		this.email = email;
		this.emailVerified = emailVerified;
		this.displayName = displayName;
		this.status = AdminStatus.ACTIVE;
		this.roles = new HashSet<>(roles);
	}

	// bootstrap 명령 전용 - 웹 로그인 경로로는 호출되지 않는다(관리자 자동 가입 금지).
	public static AdminUser bootstrap(String googleSub, String email, String displayName, Set<AdminRole> roles) {
		return new AdminUser(googleSub, email, true, displayName, roles);
	}

	// AdminLoginResolver 전용 - 이메일이 일치하는 대기 중인 admin_invitations가 있을 때만 호출된다.
	// approvedByAdminId는 그 초대를 만든 관리자다(NULL이면 SUPER_ADMIN 자동 시드 초대).
	public static AdminUser activateFromInvitation(
			String googleSub, String email, String displayName, Set<AdminRole> roles, UUID approvedByAdminId) {
		AdminUser adminUser = new AdminUser(googleSub, email, true, displayName, roles);
		adminUser.approvedByAdminId = approvedByAdminId;
		adminUser.approvedAt = Instant.now();
		return adminUser;
	}

	// DISABLED면 AdminLoginResolver가 다음 로그인 시도부터 거부한다 - 기존 세션도 다음 요청에서 거부된다.
	public void disable() {
		this.status = AdminStatus.DISABLED;
	}

	// Google 로그인 성공 후 매번 호출된다. 이메일은 인가 키가 아니라 표시·변경 감지용이라 최신화한다.
	public void recordLogin(String email, boolean emailVerified, String displayName) {
		this.email = email;
		this.emailVerified = emailVerified;
		if (displayName != null) {
			this.displayName = displayName;
		}
		this.lastLoginAt = Instant.now();
	}

	public UUID getId() {
		return id;
	}

	public String getGoogleSub() {
		return googleSub;
	}

	public String getEmail() {
		return email;
	}

	public boolean isEmailVerified() {
		return emailVerified;
	}

	public String getDisplayName() {
		return displayName;
	}

	public AdminStatus getStatus() {
		return status;
	}

	public Set<AdminRole> getRoles() {
		return roles;
	}

	public UUID getApprovedByAdminId() {
		return approvedByAdminId;
	}

	public Instant getApprovedAt() {
		return approvedAt;
	}

	public Instant getLastLoginAt() {
		return lastLoginAt;
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
