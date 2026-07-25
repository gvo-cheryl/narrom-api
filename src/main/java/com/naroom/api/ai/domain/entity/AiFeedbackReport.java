package com.naroom.api.ai.domain.entity;

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
import org.hibernate.annotations.CreationTimestamp;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "ai_feedback_reports")
public class AiFeedbackReport {

	@Id
	@GeneratedValue(strategy = GenerationType.UUID)
	@Column(name = "id", nullable = false, updatable = false)
	private UUID id;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "member_id", nullable = false, updatable = false)
	private Member member;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "ai_generation_run_id", nullable = false, updatable = false)
	private AiGenerationRun generationRun;

	@Column(name = "reason_code", nullable = false, updatable = false, length = 50)
	private String reasonCode;

	@Column(name = "comment", updatable = false)
	private String comment;

	@CreationTimestamp
	@Column(name = "created_at", nullable = false, updatable = false)
	private Instant createdAt;

	protected AiFeedbackReport() {
	}

	private AiFeedbackReport(Member member, AiGenerationRun generationRun, String reasonCode, String comment) {
		this.member = member;
		this.generationRun = generationRun;
		this.reasonCode = reasonCode;
		this.comment = comment;
	}

	public static AiFeedbackReport create(Member member, AiGenerationRun generationRun, String reasonCode, String comment) {
		return new AiFeedbackReport(member, generationRun, reasonCode, comment);
	}

	public UUID getId() {
		return id;
	}

	public Member getMember() {
		return member;
	}

	public AiGenerationRun getGenerationRun() {
		return generationRun;
	}

	public String getReasonCode() {
		return reasonCode;
	}

	public String getComment() {
		return comment;
	}

	public Instant getCreatedAt() {
		return createdAt;
	}

}
