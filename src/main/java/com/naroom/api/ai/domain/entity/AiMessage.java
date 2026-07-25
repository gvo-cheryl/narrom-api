package com.naroom.api.ai.domain.entity;

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
@Table(name = "ai_messages")
public class AiMessage {

	@Id
	@GeneratedValue(strategy = GenerationType.UUID)
	@Column(name = "id", nullable = false, updatable = false)
	private UUID id;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "conversation_id", nullable = false, updatable = false)
	private AiConversation conversation;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "generation_run_id", updatable = false)
	private AiGenerationRun generationRun;

	@Enumerated(EnumType.STRING)
	@JdbcTypeCode(SqlTypes.NAMED_ENUM)
	@Column(name = "role", nullable = false, updatable = false)
	private AiMessageRole role;

	@Column(name = "content", nullable = false, updatable = false)
	private String content;

	@CreationTimestamp
	@Column(name = "created_at", nullable = false, updatable = false)
	private Instant createdAt;

	protected AiMessage() {
	}

	private AiMessage(AiConversation conversation, AiGenerationRun generationRun, AiMessageRole role, String content) {
		this.conversation = conversation;
		this.generationRun = generationRun;
		this.role = role;
		this.content = content;
	}

	public static AiMessage fromUser(AiConversation conversation, String content) {
		return new AiMessage(conversation, null, AiMessageRole.USER, content);
	}

	public static AiMessage fromAssistant(AiConversation conversation, AiGenerationRun generationRun, String content) {
		return new AiMessage(conversation, generationRun, AiMessageRole.ASSISTANT, content);
	}

	public UUID getId() {
		return id;
	}

	public AiConversation getConversation() {
		return conversation;
	}

	public AiGenerationRun getGenerationRun() {
		return generationRun;
	}

	public AiMessageRole getRole() {
		return role;
	}

	public String getContent() {
		return content;
	}

	public Instant getCreatedAt() {
		return createdAt;
	}

}
