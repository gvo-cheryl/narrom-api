package com.naroom.api.ai.domain.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.Instant;
import java.util.UUID;

// scope와 featureType은 DB CHECK 제약(ck_ai_prompt_versions_1)으로 묶여 있다: COMMON은 featureType이 없고,
// FEATURE는 반드시 featureType이 있다. forCommon/forFeature로만 생성해 이 규칙을 강제한다.
@Entity
@Table(name = "ai_prompt_versions")
public class AiPromptVersion {

	@Id
	@GeneratedValue(strategy = GenerationType.UUID)
	@Column(name = "id", nullable = false, updatable = false)
	private UUID id;

	@Enumerated(EnumType.STRING)
	@JdbcTypeCode(SqlTypes.NAMED_ENUM)
	@Column(name = "scope", nullable = false, updatable = false)
	private AiPromptScope scope;

	@Enumerated(EnumType.STRING)
	@JdbcTypeCode(SqlTypes.NAMED_ENUM)
	@Column(name = "feature_type", updatable = false)
	private AiFeatureType featureType;

	@Column(name = "version_label", nullable = false, updatable = false, length = 40)
	private String versionLabel;

	@Column(name = "output_schema_version", updatable = false, length = 40)
	private String outputSchemaVersion;

	@Column(name = "is_active", nullable = false)
	private boolean active;

	@CreationTimestamp
	@Column(name = "created_at", nullable = false, updatable = false)
	private Instant createdAt;

	protected AiPromptVersion() {
	}

	private AiPromptVersion(AiPromptScope scope, AiFeatureType featureType, String versionLabel, String outputSchemaVersion) {
		this.scope = scope;
		this.featureType = featureType;
		this.versionLabel = versionLabel;
		this.outputSchemaVersion = outputSchemaVersion;
		this.active = true;
	}

	public static AiPromptVersion forCommon(String versionLabel) {
		return new AiPromptVersion(AiPromptScope.COMMON, null, versionLabel, null);
	}

	public static AiPromptVersion forFeature(AiFeatureType featureType, String versionLabel, String outputSchemaVersion) {
		return new AiPromptVersion(AiPromptScope.FEATURE, featureType, versionLabel, outputSchemaVersion);
	}

	public void deactivate() {
		this.active = false;
	}

	public UUID getId() {
		return id;
	}

	public AiPromptScope getScope() {
		return scope;
	}

	public AiFeatureType getFeatureType() {
		return featureType;
	}

	public String getVersionLabel() {
		return versionLabel;
	}

	public String getOutputSchemaVersion() {
		return outputSchemaVersion;
	}

	public boolean isActive() {
		return active;
	}

	public Instant getCreatedAt() {
		return createdAt;
	}

}
