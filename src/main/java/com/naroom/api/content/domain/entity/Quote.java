package com.naroom.api.content.domain.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
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

@Entity
@Table(name = "quotes")
public class Quote {

	@Id
	@GeneratedValue(strategy = GenerationType.UUID)
	@Column(name = "id", nullable = false, updatable = false)
	private UUID id;

	@Column(name = "text", nullable = false)
	private String text;

	@Column(name = "author_name", length = 120)
	private String authorName;

	@Column(name = "source_name", length = 255)
	private String sourceName;

	@Column(name = "source_url")
	private String sourceUrl;

	@Enumerated(EnumType.STRING)
	@JdbcTypeCode(SqlTypes.NAMED_ENUM)
	@Column(name = "status", nullable = false)
	private QuoteStatus status;

	@Column(name = "code", nullable = false, updatable = false, length = 80)
	private String code;

	@Column(name = "version_no", nullable = false, updatable = false)
	private int versionNo;

	@Column(name = "active_from")
	private Instant activeFrom;

	@Column(name = "active_until")
	private Instant activeUntil;

	// 다른 quotes row를 가리키는 이력 정보라 연관관계 대신 원문 UUID만 보관한다(RecordPrompt와 동일한 이유).
	@Column(name = "supersedes_quote_id", updatable = false)
	private UUID supersedesQuoteId;

	// admin_users는 별도 신원 체계(com.naroom.api.admin)라 엔티티 연관관계 대신 원문 UUID만 보관한다.
	// 초기 시드 데이터는 관리자가 만든 게 아니라 NULL일 수 있다.
	@Column(name = "created_by_admin_id", updatable = false)
	private UUID createdByAdminId;

	@ManyToMany(fetch = FetchType.LAZY)
	@JoinTable(
			name = "quote_topic_links",
			joinColumns = @JoinColumn(name = "quote_id"),
			inverseJoinColumns = @JoinColumn(name = "topic_id"))
	private Set<QuoteTopic> topics = new HashSet<>();

	@CreationTimestamp
	@Column(name = "created_at", nullable = false, updatable = false)
	private Instant createdAt;

	@UpdateTimestamp
	@Column(name = "updated_at", nullable = false)
	private Instant updatedAt;

	@Version
	@Column(name = "version", nullable = false)
	private Long version;

	protected Quote() {
	}

	private Quote(
			String code,
			int versionNo,
			String text,
			String authorName,
			String sourceName,
			String sourceUrl,
			Instant activeFrom,
			Instant activeUntil,
			UUID supersedesQuoteId,
			UUID createdByAdminId) {
		this.code = code;
		this.versionNo = versionNo;
		this.text = text;
		this.authorName = authorName;
		this.sourceName = sourceName;
		this.sourceUrl = sourceUrl;
		this.status = QuoteStatus.DRAFT;
		this.activeFrom = activeFrom;
		this.activeUntil = activeUntil;
		this.supersedesQuoteId = supersedesQuoteId;
		this.createdByAdminId = createdByAdminId;
	}

	// 테스트 픽스처 등 관리자 리비전 흐름 밖에서 쓰는 단순 생성 - 자기 자신을 code로 삼아 1번째 버전으로 취급한다.
	public static Quote create(String text, String authorName, String sourceName, String sourceUrl) {
		return new Quote(UUID.randomUUID().toString(), 1, text, authorName, sourceName, sourceUrl, null, null, null, null);
	}

	public static Quote create(
			String code,
			int versionNo,
			String text,
			String authorName,
			String sourceName,
			String sourceUrl,
			Instant activeFrom,
			Instant activeUntil,
			UUID supersedesQuoteId,
			UUID createdByAdminId) {
		return new Quote(
				code, versionNo, text, authorName, sourceName, sourceUrl, activeFrom, activeUntil,
				supersedesQuoteId, createdByAdminId);
	}

	// DRAFT 상태에서만 그대로 수정한다 - 이미 PUBLISHED된 버전은 절대 UPDATE하지 않는다(§10.3).
	public void updateDraft(
			String text, String authorName, String sourceName, String sourceUrl,
			Instant activeFrom, Instant activeUntil) {
		this.text = text;
		this.authorName = authorName;
		this.sourceName = sourceName;
		this.sourceUrl = sourceUrl;
		this.activeFrom = activeFrom;
		this.activeUntil = activeUntil;
	}

	public void publish() {
		this.status = QuoteStatus.PUBLISHED;
	}

	public void archive() {
		this.status = QuoteStatus.ARCHIVED;
	}

	public void addTopic(QuoteTopic topic) {
		this.topics.add(topic);
	}

	public void replaceTopics(Set<QuoteTopic> newTopics) {
		this.topics.clear();
		this.topics.addAll(newTopics);
	}

	public UUID getId() {
		return id;
	}

	public String getText() {
		return text;
	}

	public String getAuthorName() {
		return authorName;
	}

	public String getSourceName() {
		return sourceName;
	}

	public String getSourceUrl() {
		return sourceUrl;
	}

	public QuoteStatus getStatus() {
		return status;
	}

	public String getCode() {
		return code;
	}

	public int getVersionNo() {
		return versionNo;
	}

	public Instant getActiveFrom() {
		return activeFrom;
	}

	public Instant getActiveUntil() {
		return activeUntil;
	}

	public UUID getSupersedesQuoteId() {
		return supersedesQuoteId;
	}

	public UUID getCreatedByAdminId() {
		return createdByAdminId;
	}

	public Set<QuoteTopic> getTopics() {
		return topics;
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
