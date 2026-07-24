package com.naroom.api.record.domain.entity;

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

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "entry_tags")
public class EntryTag {

	@Id
	@GeneratedValue(strategy = GenerationType.UUID)
	@Column(name = "id", nullable = false, updatable = false)
	private UUID id;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "entry_id", nullable = false, updatable = false)
	private Entry entry;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "tag_id", nullable = false, updatable = false)
	private Tag tag;

	@Enumerated(EnumType.STRING)
	@JdbcTypeCode(SqlTypes.NAMED_ENUM)
	@Column(name = "source", nullable = false, updatable = false)
	private TagSource source;

	@Enumerated(EnumType.STRING)
	@JdbcTypeCode(SqlTypes.NAMED_ENUM)
	@Column(name = "state", nullable = false)
	private TagState state;

	@Column(name = "confidence", precision = 5, scale = 4)
	private BigDecimal confidence;

	@Column(name = "evidence_excerpt")
	private String evidenceExcerpt;

	@Column(name = "evidence_start")
	private Integer evidenceStart;

	@Column(name = "evidence_end")
	private Integer evidenceEnd;

	@CreationTimestamp
	@Column(name = "created_at", nullable = false, updatable = false)
	private Instant createdAt;

	@UpdateTimestamp
	@Column(name = "updated_at", nullable = false)
	private Instant updatedAt;

	protected EntryTag() {
	}

	private EntryTag(
			Entry entry,
			Tag tag,
			TagSource source,
			TagState state,
			BigDecimal confidence,
			String evidenceExcerpt,
			Integer evidenceStart,
			Integer evidenceEnd) {
		this.entry = entry;
		this.tag = tag;
		this.source = source;
		this.state = state;
		this.confidence = confidence;
		this.evidenceExcerpt = evidenceExcerpt;
		this.evidenceStart = evidenceStart;
		this.evidenceEnd = evidenceEnd;
	}

	// 사용자가 직접 붙인 태그는 확인 절차 없이 바로 CONFIRMED로 시작한다.
	public static EntryTag attachByUser(Entry entry, Tag tag) {
		return new EntryTag(entry, tag, TagSource.USER, TagState.CONFIRMED, null, null, null, null);
	}

	// 체크인/자기회고/실험처럼 시스템이 정한 태그는 사용자 확인 없이 SYSTEM 상태로 확정된다.
	public static EntryTag attachSystem(Entry entry, Tag tag, TagSource source) {
		return new EntryTag(entry, tag, source, TagState.SYSTEM, null, null, null, null);
	}

	// AI 제안은 사용자가 확인/거절하기 전까지 SUGGESTED 상태로 남는다.
	public static EntryTag suggestByAi(
			Entry entry, Tag tag, BigDecimal confidence, String evidenceExcerpt, Integer evidenceStart, Integer evidenceEnd) {
		return new EntryTag(
				entry, tag, TagSource.AI, TagState.SUGGESTED, confidence, evidenceExcerpt, evidenceStart, evidenceEnd);
	}

	public void confirm() {
		this.state = TagState.CONFIRMED;
	}

	public void reject() {
		this.state = TagState.REJECTED;
	}

	public UUID getId() {
		return id;
	}

	public Entry getEntry() {
		return entry;
	}

	public Tag getTag() {
		return tag;
	}

	public TagSource getSource() {
		return source;
	}

	public TagState getState() {
		return state;
	}

	public BigDecimal getConfidence() {
		return confidence;
	}

	public String getEvidenceExcerpt() {
		return evidenceExcerpt;
	}

	public Integer getEvidenceStart() {
		return evidenceStart;
	}

	public Integer getEvidenceEnd() {
		return evidenceEnd;
	}

	public Instant getCreatedAt() {
		return createdAt;
	}

	public Instant getUpdatedAt() {
		return updatedAt;
	}

}
