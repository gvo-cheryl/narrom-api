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
@Table(name = "ai_generation_runs")
public class AiGenerationRun {

	@Id
	@GeneratedValue(strategy = GenerationType.UUID)
	@Column(name = "id", nullable = false, updatable = false)
	private UUID id;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "ai_job_id", nullable = false, updatable = false)
	private AiJob aiJob;

	@Column(name = "model_name", nullable = false, updatable = false, length = 80)
	private String modelName;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "common_prompt_version_id", nullable = false, updatable = false)
	private AiPromptVersion commonPromptVersion;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "feature_prompt_version_id", nullable = false, updatable = false)
	private AiPromptVersion featurePromptVersion;

	@Column(name = "output_schema_version", nullable = false, updatable = false, length = 40)
	private String outputSchemaVersion;

	@Column(name = "input_tokens")
	private Integer inputTokens;

	@Column(name = "output_tokens")
	private Integer outputTokens;

	@Enumerated(EnumType.STRING)
	@JdbcTypeCode(SqlTypes.NAMED_ENUM)
	@Column(name = "input_safety_status")
	private AiSafetyGrade inputSafetyStatus;

	@Enumerated(EnumType.STRING)
	@JdbcTypeCode(SqlTypes.NAMED_ENUM)
	@Column(name = "output_safety_status")
	private AiSafetyGrade outputSafetyStatus;

	@Column(name = "latency_ms")
	private Integer latencyMs;

	@Column(name = "store_enabled", nullable = false, updatable = false)
	private boolean storeEnabled;

	@Column(name = "is_regeneration", nullable = false, updatable = false)
	private boolean regeneration;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "parent_run_id", updatable = false)
	private AiGenerationRun parentRun;

	@CreationTimestamp
	@Column(name = "requested_at", nullable = false, updatable = false)
	private Instant requestedAt;

	@Column(name = "completed_at")
	private Instant completedAt;

	protected AiGenerationRun() {
	}

	private AiGenerationRun(
			AiJob aiJob,
			String modelName,
			AiPromptVersion commonPromptVersion,
			AiPromptVersion featurePromptVersion,
			String outputSchemaVersion,
			boolean regeneration,
			AiGenerationRun parentRun) {
		this.aiJob = aiJob;
		this.modelName = modelName;
		this.commonPromptVersion = commonPromptVersion;
		this.featurePromptVersion = featurePromptVersion;
		this.outputSchemaVersion = outputSchemaVersion;
		this.storeEnabled = false;
		this.regeneration = regeneration;
		this.parentRun = parentRun;
	}

	public static AiGenerationRun start(
			AiJob aiJob,
			String modelName,
			AiPromptVersion commonPromptVersion,
			AiPromptVersion featurePromptVersion,
			String outputSchemaVersion) {
		return new AiGenerationRun(aiJob, modelName, commonPromptVersion, featurePromptVersion, outputSchemaVersion, false, null);
	}

	public static AiGenerationRun regenerate(
			AiJob aiJob,
			String modelName,
			AiPromptVersion commonPromptVersion,
			AiPromptVersion featurePromptVersion,
			String outputSchemaVersion,
			AiGenerationRun parentRun) {
		return new AiGenerationRun(aiJob, modelName, commonPromptVersion, featurePromptVersion, outputSchemaVersion, true, parentRun);
	}

	public void complete(
			Integer inputTokens,
			Integer outputTokens,
			AiSafetyGrade inputSafetyStatus,
			AiSafetyGrade outputSafetyStatus,
			Integer latencyMs,
			Instant completedAt) {
		this.inputTokens = inputTokens;
		this.outputTokens = outputTokens;
		this.inputSafetyStatus = inputSafetyStatus;
		this.outputSafetyStatus = outputSafetyStatus;
		this.latencyMs = latencyMs;
		this.completedAt = completedAt;
	}

	public UUID getId() {
		return id;
	}

	public AiJob getAiJob() {
		return aiJob;
	}

	public String getModelName() {
		return modelName;
	}

	public AiPromptVersion getCommonPromptVersion() {
		return commonPromptVersion;
	}

	public AiPromptVersion getFeaturePromptVersion() {
		return featurePromptVersion;
	}

	public String getOutputSchemaVersion() {
		return outputSchemaVersion;
	}

	public Integer getInputTokens() {
		return inputTokens;
	}

	public Integer getOutputTokens() {
		return outputTokens;
	}

	public AiSafetyGrade getInputSafetyStatus() {
		return inputSafetyStatus;
	}

	public AiSafetyGrade getOutputSafetyStatus() {
		return outputSafetyStatus;
	}

	public Integer getLatencyMs() {
		return latencyMs;
	}

	public boolean isStoreEnabled() {
		return storeEnabled;
	}

	public boolean isRegeneration() {
		return regeneration;
	}

	public AiGenerationRun getParentRun() {
		return parentRun;
	}

	public Instant getRequestedAt() {
		return requestedAt;
	}

	public Instant getCompletedAt() {
		return completedAt;
	}

}
