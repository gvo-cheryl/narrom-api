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
import java.time.LocalDate;
import java.util.UUID;

// ai_generation_runs에서 계산되는 파생 집계 테이블이라, 원본 호출 기록을 다시 읽지 않고도
// 하루 단위 회원·기능·모델별 합계를 바로 조회할 수 있게 한다.
@Entity
@Table(name = "ai_usage_daily")
public class AiUsageDaily {

	@Id
	@GeneratedValue(strategy = GenerationType.UUID)
	@Column(name = "id", nullable = false, updatable = false)
	private UUID id;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "member_id", nullable = false, updatable = false)
	private Member member;

	@Column(name = "usage_date", nullable = false, updatable = false)
	private LocalDate usageDate;

	@Enumerated(EnumType.STRING)
	@JdbcTypeCode(SqlTypes.NAMED_ENUM)
	@Column(name = "feature_type", nullable = false, updatable = false)
	private AiFeatureType featureType;

	@Column(name = "model_name", nullable = false, updatable = false, length = 80)
	private String modelName;

	@Column(name = "call_count", nullable = false)
	private int callCount;

	@Column(name = "regeneration_count", nullable = false)
	private int regenerationCount;

	@Column(name = "input_tokens", nullable = false)
	private long inputTokens;

	@Column(name = "output_tokens", nullable = false)
	private long outputTokens;

	@CreationTimestamp
	@Column(name = "created_at", nullable = false, updatable = false)
	private Instant createdAt;

	@UpdateTimestamp
	@Column(name = "updated_at", nullable = false)
	private Instant updatedAt;

	protected AiUsageDaily() {
	}

	private AiUsageDaily(Member member, LocalDate usageDate, AiFeatureType featureType, String modelName) {
		this.member = member;
		this.usageDate = usageDate;
		this.featureType = featureType;
		this.modelName = modelName;
		this.callCount = 0;
		this.regenerationCount = 0;
		this.inputTokens = 0;
		this.outputTokens = 0;
	}

	public static AiUsageDaily start(Member member, LocalDate usageDate, AiFeatureType featureType, String modelName) {
		return new AiUsageDaily(member, usageDate, featureType, modelName);
	}

	public void addUsage(long inputTokens, long outputTokens, boolean isRegeneration) {
		this.callCount++;
		if (isRegeneration) {
			this.regenerationCount++;
		}
		this.inputTokens += inputTokens;
		this.outputTokens += outputTokens;
	}

	public UUID getId() {
		return id;
	}

	public Member getMember() {
		return member;
	}

	public LocalDate getUsageDate() {
		return usageDate;
	}

	public AiFeatureType getFeatureType() {
		return featureType;
	}

	public String getModelName() {
		return modelName;
	}

	public int getCallCount() {
		return callCount;
	}

	public int getRegenerationCount() {
		return regenerationCount;
	}

	public long getInputTokens() {
		return inputTokens;
	}

	public long getOutputTokens() {
		return outputTokens;
	}

	public Instant getCreatedAt() {
		return createdAt;
	}

	public Instant getUpdatedAt() {
		return updatedAt;
	}

}
