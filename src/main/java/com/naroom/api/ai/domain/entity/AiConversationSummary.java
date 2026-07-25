package com.naroom.api.ai.domain.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import org.hibernate.annotations.CreationTimestamp;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "ai_conversation_summaries")
public class AiConversationSummary {

	@Id
	@GeneratedValue(strategy = GenerationType.UUID)
	@Column(name = "id", nullable = false, updatable = false)
	private UUID id;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "conversation_id", nullable = false, updatable = false)
	private AiConversation conversation;

	@Column(name = "summary_text", nullable = false, updatable = false)
	private String summaryText;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "covers_until_message_id", nullable = false, updatable = false)
	private AiMessage coversUntilMessage;

	@CreationTimestamp
	@Column(name = "created_at", nullable = false, updatable = false)
	private Instant createdAt;

	protected AiConversationSummary() {
	}

	private AiConversationSummary(AiConversation conversation, String summaryText, AiMessage coversUntilMessage) {
		this.conversation = conversation;
		this.summaryText = summaryText;
		this.coversUntilMessage = coversUntilMessage;
	}

	public static AiConversationSummary create(AiConversation conversation, String summaryText, AiMessage coversUntilMessage) {
		return new AiConversationSummary(conversation, summaryText, coversUntilMessage);
	}

	public UUID getId() {
		return id;
	}

	public AiConversation getConversation() {
		return conversation;
	}

	public String getSummaryText() {
		return summaryText;
	}

	public AiMessage getCoversUntilMessage() {
		return coversUntilMessage;
	}

	public Instant getCreatedAt() {
		return createdAt;
	}

}
