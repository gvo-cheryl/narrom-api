package com.naroom.api.lifetime.domain.entity;

import com.naroom.api.account.domain.entity.Member;
import com.naroom.api.record.domain.entity.Entry;
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
import java.time.LocalDate;
import java.util.UUID;

// 실제 글 내용은 이 테이블이 아니라 entry.getBody()(entry_type=SELF_SUMMARY)에 있다. 이 엔티티는
// 범위·기간·보관 메타데이터만 담당한다 - entry_self_reflections(기록 1건에 대한 후기)와는 다른 개념이다.
@Entity
@Table(name = "personal_summaries")
public class PersonalSummary {

	@Id
	@GeneratedValue(strategy = GenerationType.UUID)
	@Column(name = "id", nullable = false, updatable = false)
	private UUID id;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "member_id", nullable = false, updatable = false)
	private Member member;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "entry_id", nullable = false, updatable = false)
	private Entry entry;

	@Enumerated(EnumType.STRING)
	@JdbcTypeCode(SqlTypes.NAMED_ENUM)
	@Column(name = "scope", nullable = false, updatable = false)
	private SummaryScope scope;

	@Column(name = "period_start", updatable = false)
	private LocalDate periodStart;

	@Column(name = "period_end", updatable = false)
	private LocalDate periodEnd;

	@Column(name = "archived_at")
	private Instant archivedAt;

	@CreationTimestamp
	@Column(name = "created_at", nullable = false, updatable = false)
	private Instant createdAt;

	@UpdateTimestamp
	@Column(name = "updated_at", nullable = false)
	private Instant updatedAt;

	protected PersonalSummary() {
	}

	private PersonalSummary(Member member, Entry entry, SummaryScope scope, LocalDate periodStart, LocalDate periodEnd) {
		this.member = member;
		this.entry = entry;
		this.scope = scope;
		this.periodStart = periodStart;
		this.periodEnd = periodEnd;
	}

	public static PersonalSummary create(Member member, Entry entry, SummaryScope scope, LocalDate periodStart, LocalDate periodEnd) {
		return new PersonalSummary(member, entry, scope, periodStart, periodEnd);
	}

	public void archive(Instant archivedAt) {
		this.archivedAt = archivedAt;
	}

	public UUID getId() {
		return id;
	}

	public Member getMember() {
		return member;
	}

	public Entry getEntry() {
		return entry;
	}

	public SummaryScope getScope() {
		return scope;
	}

	public LocalDate getPeriodStart() {
		return periodStart;
	}

	public LocalDate getPeriodEnd() {
		return periodEnd;
	}

	public Instant getArchivedAt() {
		return archivedAt;
	}

	public Instant getCreatedAt() {
		return createdAt;
	}

	public Instant getUpdatedAt() {
		return updatedAt;
	}

}
