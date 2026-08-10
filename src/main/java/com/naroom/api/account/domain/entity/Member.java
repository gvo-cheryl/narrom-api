package com.naroom.api.account.domain.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.UpdateTimestamp;
import org.hibernate.type.SqlTypes;

import java.time.Instant;
import java.util.UUID;

// status와 scheduledDeletionAt은 DB CHECK 제약으로 묶여 있다: PENDING_DELETION일 때만 scheduledDeletionAt이 채워진다.
@Entity
@Table(name = "members")
public class Member {

	@Id
	@GeneratedValue(strategy = GenerationType.UUID)
	@Column(name = "id", nullable = false, updatable = false)
	private UUID id;

	@Column(name = "display_name", nullable = false, length = 80)
	private String displayName;

	@Enumerated(EnumType.STRING)
	@JdbcTypeCode(SqlTypes.NAMED_ENUM)
	@Column(name = "status", nullable = false)
	private MemberStatus status;

	@Column(name = "timezone", nullable = false, length = 50)
	private String timezone;

	@Column(name = "locale", nullable = false, length = 10)
	private String locale;

	// null이면 온보딩 미완료 (conventions.md).
	@Column(name = "onboarding_completed_at")
	private Instant onboardingCompletedAt;

	@Column(name = "withdrawal_requested_at")
	private Instant withdrawalRequestedAt;

	// PENDING_DELETION 상태일 때만 값이 있다.
	@Column(name = "scheduled_deletion_at")
	private Instant scheduledDeletionAt;

	@CreationTimestamp
	@Column(name = "created_at", nullable = false, updatable = false)
	private Instant createdAt;

	@UpdateTimestamp
	@Column(name = "updated_at", nullable = false)
	private Instant updatedAt;

	// conventions.md 낙관적 잠금 규칙(ACCOUNT_VERSION_CONFLICT)의 기준 컬럼.
	@Version
	@Column(name = "version", nullable = false)
	private Long version;

	protected Member() {
	}

	private Member(String displayName, String timezone, String locale) {
		this.displayName = displayName;
		this.status = MemberStatus.ACTIVE;
		this.timezone = timezone;
		this.locale = locale;
	}

	/**
	 * 카카오 로그인 등으로 신규 회원을 생성할 때 사용한다.
	 * PRODUCT_CONTEXT.md 기준 한국어 사용자를 기본값으로 한다.
	 */
	public static Member create(String displayName) {
		return new Member(displayName, "Asia/Seoul", "ko-KR");
	}

	// 온보딩 완료 트랜잭션 전체가 성공해야만 호출된다(onboarding_completed_at은 마지막에 기록).
	public void completeOnboarding(String displayName, String timezone, String locale) {
		this.displayName = displayName;
		this.timezone = timezone;
		this.locale = locale;
		this.onboardingCompletedAt = Instant.now();
	}

	// Account Deletion Rules: 요청 직후 상태를 PENDING_DELETION으로 바꿔 로그인 접근을 즉시 막는다
	// (requireLoginableStatus가 이 상태를 확인). scheduledDeletionAt은 호출자가 그레이스 기간을 더해 넘긴다.
	public void requestWithdrawal(Instant now, Instant scheduledDeletionAt) {
		this.status = MemberStatus.PENDING_DELETION;
		this.withdrawalRequestedAt = now;
		this.scheduledDeletionAt = scheduledDeletionAt;
	}

	// 그레이스 기간 안에 카카오로 본인 확인을 거친 뒤 명시적으로 복구를 확인했을 때만 호출된다
	// (로그인만으로는 자동 복구되지 않는다 - CLAUDE.md 계정 삭제 규칙).
	public void restore() {
		this.status = MemberStatus.ACTIVE;
		this.withdrawalRequestedAt = null;
		this.scheduledDeletionAt = null;
	}

	public UUID getId() {
		return id;
	}

	public String getDisplayName() {
		return displayName;
	}

	public MemberStatus getStatus() {
		return status;
	}

	public String getTimezone() {
		return timezone;
	}

	public String getLocale() {
		return locale;
	}

	public Instant getOnboardingCompletedAt() {
		return onboardingCompletedAt;
	}

	public Instant getWithdrawalRequestedAt() {
		return withdrawalRequestedAt;
	}

	public Instant getScheduledDeletionAt() {
		return scheduledDeletionAt;
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
