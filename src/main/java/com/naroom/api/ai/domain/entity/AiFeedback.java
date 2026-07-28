package com.naroom.api.ai.domain.entity;

import com.naroom.api.account.domain.entity.Member;
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
import java.util.UUID;

@Entity
@Table(name = "ai_feedback")
public class AiFeedback {

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

	@Enumerated(EnumType.STRING)
	@JdbcTypeCode(SqlTypes.NAMED_ENUM)
	@Column(name = "helpfulness", nullable = false, updatable = false)
	private AiFeedbackHelpfulness helpfulness;

	@Column(name = "reason_code", length = 50)
	private String reasonCode;

	@Column(name = "custom_reason")
	private String customReason;

	@Column(name = "apply_long_term")
	private Boolean applyLongTerm;

	@CreationTimestamp
	@Column(name = "created_at", nullable = false, updatable = false)
	private Instant createdAt;

	@UpdateTimestamp
	@Column(name = "updated_at", nullable = false)
	private Instant updatedAt;

	protected AiFeedback() {
	}

	private AiFeedback(Member member, AiGenerationRun generationRun, AiFeedbackHelpfulness helpfulness) {
		this.member = member;
		this.generationRun = generationRun;
		this.helpfulness = helpfulness;
	}

	public static AiFeedback rate(Member member, AiGenerationRun generationRun, AiFeedbackHelpfulness helpfulness) {
		return new AiFeedback(member, generationRun, helpfulness);
	}

	// 부정 평가(SOMEWHAT_UNHELPFUL/UNHELPFUL)일 때만 2차 사유를 받는다.
	public void addReason(String reasonCode, String customReason) {
		this.reasonCode = reasonCode;
		this.customReason = customReason;
	}

	// 평가는 (member, generation_run) 1건뿐이라(UNIQUE 제약) 재제출은 새로 만들지 않고 이 기존 행을 고친다.
	// 긍정 평가로 바뀌면 2차 사유는 더 이상 의미가 없으므로 함께 지운다.
	public void updateRating(AiFeedbackHelpfulness helpfulness, String reasonCode, String customReason) {
		this.helpfulness = helpfulness;
		if (helpfulness == AiFeedbackHelpfulness.HELPFUL) {
			this.reasonCode = null;
			this.customReason = null;
		} else {
			this.reasonCode = reasonCode;
			this.customReason = customReason;
		}
	}

	public void confirmLongTermApplication(boolean applyLongTerm) {
		this.applyLongTerm = applyLongTerm;
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

	public AiFeedbackHelpfulness getHelpfulness() {
		return helpfulness;
	}

	public String getReasonCode() {
		return reasonCode;
	}

	public String getCustomReason() {
		return customReason;
	}

	public Boolean getApplyLongTerm() {
		return applyLongTerm;
	}

	public Instant getCreatedAt() {
		return createdAt;
	}

	public Instant getUpdatedAt() {
		return updatedAt;
	}

}
