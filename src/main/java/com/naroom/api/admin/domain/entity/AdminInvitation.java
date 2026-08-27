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
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.Instant;
import java.util.Set;
import java.util.UUID;

// 로그인 전에 미리 등록해두는 관리자 초대. AdminLoginResolver가 이메일이 일치하는 첫 Google 로그인에서
// 이 초대를 admin_users로 전환한다 - 로그인 성공만으로 관리자가 자동 생성되지는 않는다는 불변식은 그대로
// 유지된다(초대 자체가 명시적 승인 행위다).
@Entity
@Table(name = "admin_invitations")
public class AdminInvitation {

	@Id
	@GeneratedValue(strategy = GenerationType.UUID)
	@Column(name = "id", nullable = false, updatable = false)
	private UUID id;

	@Column(name = "email", nullable = false, length = 320, updatable = false)
	private String email;

	@ElementCollection(fetch = FetchType.EAGER)
	@CollectionTable(name = "admin_invitation_roles", joinColumns = @JoinColumn(name = "admin_invitation_id"))
	@Enumerated(EnumType.STRING)
	@JdbcTypeCode(SqlTypes.NAMED_ENUM)
	@Column(name = "role", nullable = false)
	private Set<AdminRole> roles;

	// NULL이면 AdminSuperAdminInvitationSeeder가 만든 것이다.
	@Column(name = "invited_by_admin_id", updatable = false)
	private UUID invitedByAdminId;

	@CreationTimestamp
	@Column(name = "invited_at", nullable = false, updatable = false)
	private Instant invitedAt;

	@Column(name = "consumed_at")
	private Instant consumedAt;

	@Column(name = "consumed_admin_user_id")
	private UUID consumedAdminUserId;

	@Column(name = "revoked_at")
	private Instant revokedAt;

	protected AdminInvitation() {
	}

	private AdminInvitation(String email, Set<AdminRole> roles, UUID invitedByAdminId) {
		this.email = email;
		this.roles = roles;
		this.invitedByAdminId = invitedByAdminId;
	}

	public static AdminInvitation invite(String email, Set<AdminRole> roles, UUID invitedByAdminId) {
		return new AdminInvitation(email, roles, invitedByAdminId);
	}

	public void consume(UUID adminUserId) {
		this.consumedAt = Instant.now();
		this.consumedAdminUserId = adminUserId;
	}

	public void revoke() {
		this.revokedAt = Instant.now();
	}

	public boolean isPending() {
		return consumedAt == null && revokedAt == null;
	}

	public UUID getId() {
		return id;
	}

	public String getEmail() {
		return email;
	}

	public Set<AdminRole> getRoles() {
		return roles;
	}

	public UUID getInvitedByAdminId() {
		return invitedByAdminId;
	}

	public Instant getInvitedAt() {
		return invitedAt;
	}

	public Instant getConsumedAt() {
		return consumedAt;
	}

	public UUID getConsumedAdminUserId() {
		return consumedAdminUserId;
	}

	public Instant getRevokedAt() {
		return revokedAt;
	}

}
