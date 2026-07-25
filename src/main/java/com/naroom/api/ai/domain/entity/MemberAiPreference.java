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
import org.hibernate.annotations.UpdateTimestamp;

import java.time.Instant;
import java.util.UUID;

// tone/responseLength는 정책 문서 15.5의 예시(DIRECT, SHORT 등)만 있고 전체 허용값 목록이
// 확정되지 않아 DB enum이 아닌 varchar로 두고, 허용 값 검증은 애플리케이션 레벨에서 한다.
@Entity
@Table(name = "member_ai_preferences")
public class MemberAiPreference {

	@Id
	@GeneratedValue(strategy = GenerationType.UUID)
	@Column(name = "id", nullable = false, updatable = false)
	private UUID id;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "member_id", nullable = false, updatable = false)
	private Member member;

	@Column(name = "tone", length = 30)
	private String tone;

	@Column(name = "response_length", length = 30)
	private String responseLength;

	@Column(name = "reduce_emotional_validation", nullable = false)
	private boolean reduceEmotionalValidation;

	@CreationTimestamp
	@Column(name = "created_at", nullable = false, updatable = false)
	private Instant createdAt;

	@UpdateTimestamp
	@Column(name = "updated_at", nullable = false)
	private Instant updatedAt;

	protected MemberAiPreference() {
	}

	private MemberAiPreference(Member member) {
		this.member = member;
		this.reduceEmotionalValidation = false;
	}

	public static MemberAiPreference createDefault(Member member) {
		return new MemberAiPreference(member);
	}

	public void update(String tone, String responseLength, boolean reduceEmotionalValidation) {
		this.tone = tone;
		this.responseLength = responseLength;
		this.reduceEmotionalValidation = reduceEmotionalValidation;
	}

	public void reset() {
		this.tone = null;
		this.responseLength = null;
		this.reduceEmotionalValidation = false;
	}

	public UUID getId() {
		return id;
	}

	public Member getMember() {
		return member;
	}

	public String getTone() {
		return tone;
	}

	public String getResponseLength() {
		return responseLength;
	}

	public boolean isReduceEmotionalValidation() {
		return reduceEmotionalValidation;
	}

	public Instant getCreatedAt() {
		return createdAt;
	}

	public Instant getUpdatedAt() {
		return updatedAt;
	}

}
