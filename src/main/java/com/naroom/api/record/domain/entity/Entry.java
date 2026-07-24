package com.naroom.api.record.domain.entity;

import com.naroom.api.account.domain.entity.Member;
import com.naroom.api.content.domain.entity.Quote;
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
import jakarta.persistence.Version;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.UpdateTimestamp;
import org.hibernate.type.SqlTypes;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

// status와 publishedAt은 이 엔티티만으로는 DB CHECK로 묶이지 않는다(reference 스키마 기준, V2 draft의
// ck_entries_publish_state 같은 제약을 두지 않음). publish()를 통해서만 발행 상태로 바뀌게 해 일관성을 지킨다.
@Entity
@Table(name = "entries")
public class Entry {

	@Id
	@GeneratedValue(strategy = GenerationType.UUID)
	@Column(name = "id", nullable = false, updatable = false)
	private UUID id;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "member_id", nullable = false, updatable = false)
	private Member member;

	@Enumerated(EnumType.STRING)
	@JdbcTypeCode(SqlTypes.NAMED_ENUM)
	@Column(name = "entry_type", nullable = false, updatable = false)
	private EntryType entryType;

	@Enumerated(EnumType.STRING)
	@JdbcTypeCode(SqlTypes.NAMED_ENUM)
	@Column(name = "status", nullable = false)
	private EntryStatus status;

	@Column(name = "title", length = 200)
	private String title;

	@Column(name = "body")
	private String body;

	@Column(name = "record_date", nullable = false)
	private LocalDate recordDate;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "parent_entry_id")
	private Entry parentEntry;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "quote_id")
	private Quote quote;

	@Column(name = "prompt_snapshot")
	private String promptSnapshot;

	@Column(name = "ai_processing_allowed", nullable = false)
	private boolean aiProcessingAllowed;

	@Column(name = "published_at")
	private Instant publishedAt;

	@CreationTimestamp
	@Column(name = "created_at", nullable = false, updatable = false)
	private Instant createdAt;

	@UpdateTimestamp
	@Column(name = "updated_at", nullable = false)
	private Instant updatedAt;

	@Version
	@Column(name = "version", nullable = false)
	private Long version;

	protected Entry() {
	}

	private Entry(
			Member member,
			EntryType entryType,
			String title,
			String body,
			LocalDate recordDate,
			Entry parentEntry,
			Quote quote,
			String promptSnapshot) {
		this.member = member;
		this.entryType = entryType;
		this.status = EntryStatus.DRAFT;
		this.title = title;
		this.body = body;
		this.recordDate = recordDate;
		this.parentEntry = parentEntry;
		this.quote = quote;
		this.promptSnapshot = promptSnapshot;
		this.aiProcessingAllowed = true;
	}

	public static Entry create(
			Member member,
			EntryType entryType,
			String title,
			String body,
			LocalDate recordDate,
			Entry parentEntry,
			Quote quote,
			String promptSnapshot) {
		return new Entry(member, entryType, title, body, recordDate, parentEntry, quote, promptSnapshot);
	}

	public void update(String title, String body) {
		this.title = title;
		this.body = body;
	}

	public void publish() {
		this.status = EntryStatus.PUBLISHED;
		this.publishedAt = Instant.now();
	}

	public void disallowAiProcessing() {
		this.aiProcessingAllowed = false;
	}

	public UUID getId() {
		return id;
	}

	public Member getMember() {
		return member;
	}

	public EntryType getEntryType() {
		return entryType;
	}

	public EntryStatus getStatus() {
		return status;
	}

	public String getTitle() {
		return title;
	}

	public String getBody() {
		return body;
	}

	public LocalDate getRecordDate() {
		return recordDate;
	}

	public Entry getParentEntry() {
		return parentEntry;
	}

	public Quote getQuote() {
		return quote;
	}

	public String getPromptSnapshot() {
		return promptSnapshot;
	}

	public boolean isAiProcessingAllowed() {
		return aiProcessingAllowed;
	}

	public Instant getPublishedAt() {
		return publishedAt;
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
