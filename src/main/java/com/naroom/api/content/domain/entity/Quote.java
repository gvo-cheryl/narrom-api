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

	protected Quote() {
	}

	private Quote(String text, String authorName, String sourceName, String sourceUrl) {
		this.text = text;
		this.authorName = authorName;
		this.sourceName = sourceName;
		this.sourceUrl = sourceUrl;
		this.status = QuoteStatus.DRAFT;
	}

	public static Quote create(String text, String authorName, String sourceName, String sourceUrl) {
		return new Quote(text, authorName, sourceName, sourceUrl);
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

	public Set<QuoteTopic> getTopics() {
		return topics;
	}

	public Instant getCreatedAt() {
		return createdAt;
	}

	public Instant getUpdatedAt() {
		return updatedAt;
	}

}
