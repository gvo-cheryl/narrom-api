package com.naroom.api.badge.domain.entity;

import com.naroom.api.account.domain.entity.Member;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import org.hibernate.annotations.CreationTimestamp;

import java.time.Instant;
import java.util.UUID;

// 같은 뱃지는 회원당 한 번만 획득한다(UNIQUE(member_id, badge_definition_id) - DEC-01 재획득 없음).
@Entity
@Table(
		name = "member_badges",
		uniqueConstraints = @UniqueConstraint(columnNames = {"member_id", "badge_definition_id"}))
public class MemberBadge {

	@Id
	@GeneratedValue(strategy = GenerationType.UUID)
	@Column(name = "id", nullable = false, updatable = false)
	private UUID id;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "member_id", nullable = false, updatable = false)
	private Member member;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "badge_definition_id", nullable = false, updatable = false)
	private BadgeDefinition badgeDefinition;

	@Column(name = "earned_at", nullable = false, updatable = false)
	private Instant earnedAt;

	@CreationTimestamp
	@Column(name = "created_at", nullable = false, updatable = false)
	private Instant createdAt;

	protected MemberBadge() {
	}

	private MemberBadge(Member member, BadgeDefinition badgeDefinition, Instant earnedAt) {
		this.member = member;
		this.badgeDefinition = badgeDefinition;
		this.earnedAt = earnedAt;
	}

	public static MemberBadge award(Member member, BadgeDefinition badgeDefinition, Instant earnedAt) {
		return new MemberBadge(member, badgeDefinition, earnedAt);
	}

	public UUID getId() {
		return id;
	}

	public Member getMember() {
		return member;
	}

	public BadgeDefinition getBadgeDefinition() {
		return badgeDefinition;
	}

	public Instant getEarnedAt() {
		return earnedAt;
	}

	public Instant getCreatedAt() {
		return createdAt;
	}

}
