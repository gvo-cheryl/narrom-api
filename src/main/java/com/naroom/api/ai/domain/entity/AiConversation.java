package com.naroom.api.ai.domain.entity;

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
import org.hibernate.type.SqlTypes;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "ai_conversations")
public class AiConversation {

	@Id
	@GeneratedValue(strategy = GenerationType.UUID)
	@Column(name = "id", nullable = false, updatable = false)
	private UUID id;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "member_id", nullable = false, updatable = false)
	private Member member;

	@Enumerated(EnumType.STRING)
	@JdbcTypeCode(SqlTypes.NAMED_ENUM)
	@Column(name = "feature_type", nullable = false, updatable = false)
	private AiFeatureType featureType;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "source_entry_id", updatable = false)
	private Entry sourceEntry;

	@CreationTimestamp
	@Column(name = "started_at", nullable = false, updatable = false)
	private Instant startedAt;

	@Column(name = "last_message_at", nullable = false)
	private Instant lastMessageAt;

	@Column(name = "closed_at")
	private Instant closedAt;

	protected AiConversation() {
	}

	private AiConversation(Member member, AiFeatureType featureType, Entry sourceEntry) {
		this.member = member;
		this.featureType = featureType;
		this.sourceEntry = sourceEntry;
		this.lastMessageAt = Instant.now();
	}

	public static AiConversation start(Member member, AiFeatureType featureType, Entry sourceEntry) {
		return new AiConversation(member, featureType, sourceEntry);
	}

	public void touch(Instant messageAt) {
		this.lastMessageAt = messageAt;
	}

	public void close(Instant closedAt) {
		this.closedAt = closedAt;
	}

	public UUID getId() {
		return id;
	}

	public Member getMember() {
		return member;
	}

	public AiFeatureType getFeatureType() {
		return featureType;
	}

	public Entry getSourceEntry() {
		return sourceEntry;
	}

	public Instant getStartedAt() {
		return startedAt;
	}

	public Instant getLastMessageAt() {
		return lastMessageAt;
	}

	public Instant getClosedAt() {
		return closedAt;
	}

}
