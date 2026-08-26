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
//
// content가 NULL인 row는 AiPromptVersionResolver.getOrCreateCommon/Feature가 만드는 코드(AiInstructionCatalog)
// 버전 라벨 북마킹용이다. content가 있는 row만 관리자가 작성한 실제 지침이고, draftCommon/draftFeature로
// 생성한다 - 두 종류는 항상 content IS NULL 여부로 구분되어 서로 섞이지 않는다(14.6절).
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

	@Column(name = "content")
	private String content;

	// FEATURE 범위에서만 의미 있음. NULL이면 naroom.ai.openai.model 기본값을 쓴다.
	@Column(name = "model_name", length = 80)
	private String modelName;

	// FEATURE 범위에서만 의미 있음. summary 필드 최대 글자 수, NULL이면 제한 없음.
	@Column(name = "output_max_length")
	private Integer outputMaxLength;

	@Enumerated(EnumType.STRING)
	@JdbcTypeCode(SqlTypes.NAMED_ENUM)
	@Column(name = "status", nullable = false)
	private AiPromptVersionStatus status;

	@Column(name = "supersedes_version_id", updatable = false)
	private UUID supersedesVersionId;

	// admin_users는 별도 신원 체계라 엔티티 연관관계 대신 원문 UUID만 보관한다(Quote와 동일한 이유).
	@Column(name = "created_by_admin_id", updatable = false)
	private UUID createdByAdminId;

	@CreationTimestamp
	@Column(name = "created_at", nullable = false, updatable = false)
	private Instant createdAt;

	protected AiPromptVersion() {
	}

	private AiPromptVersion(
			AiPromptScope scope,
			AiFeatureType featureType,
			String versionLabel,
			String outputSchemaVersion,
			String content,
			String modelName,
			Integer outputMaxLength,
			AiPromptVersionStatus status,
			UUID supersedesVersionId,
			UUID createdByAdminId) {
		this.scope = scope;
		this.featureType = featureType;
		this.versionLabel = versionLabel;
		this.outputSchemaVersion = outputSchemaVersion;
		this.content = content;
		this.modelName = modelName;
		this.outputMaxLength = outputMaxLength;
		this.status = status;
		this.supersedesVersionId = supersedesVersionId;
		this.createdByAdminId = createdByAdminId;
	}

	public static AiPromptVersion forCommon(String versionLabel) {
		return new AiPromptVersion(
				AiPromptScope.COMMON, null, versionLabel, null, null, null, null,
				AiPromptVersionStatus.PUBLISHED, null, null);
	}

	public static AiPromptVersion forFeature(AiFeatureType featureType, String versionLabel, String outputSchemaVersion) {
		return new AiPromptVersion(
				AiPromptScope.FEATURE, featureType, versionLabel, outputSchemaVersion, null, null, null,
				AiPromptVersionStatus.PUBLISHED, null, null);
	}

	public static AiPromptVersion draftCommon(String versionLabel, String content, UUID createdByAdminId) {
		return new AiPromptVersion(
				AiPromptScope.COMMON, null, versionLabel, null, content, null, null,
				AiPromptVersionStatus.DRAFT, null, createdByAdminId);
	}

	public static AiPromptVersion draftFeature(
			AiFeatureType featureType,
			String versionLabel,
			String content,
			String modelName,
			Integer outputMaxLength,
			UUID supersedesVersionId,
			UUID createdByAdminId) {
		return new AiPromptVersion(
				AiPromptScope.FEATURE, featureType, versionLabel, null, content, modelName, outputMaxLength,
				AiPromptVersionStatus.DRAFT, supersedesVersionId, createdByAdminId);
	}

	public static AiPromptVersion draftCommonRevision(String versionLabel, String content, UUID supersedesVersionId, UUID createdByAdminId) {
		return new AiPromptVersion(
				AiPromptScope.COMMON, null, versionLabel, null, content, null, null,
				AiPromptVersionStatus.DRAFT, supersedesVersionId, createdByAdminId);
	}

	public void updateDraft(String versionLabel, String content, String modelName, Integer outputMaxLength) {
		this.versionLabel = versionLabel;
		this.content = content;
		this.modelName = modelName;
		this.outputMaxLength = outputMaxLength;
	}

	public void publish() {
		this.status = AiPromptVersionStatus.PUBLISHED;
	}

	public void archive() {
		this.status = AiPromptVersionStatus.ARCHIVED;
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

	public String getContent() {
		return content;
	}

	public String getModelName() {
		return modelName;
	}

	public Integer getOutputMaxLength() {
		return outputMaxLength;
	}

	public AiPromptVersionStatus getStatus() {
		return status;
	}

	public UUID getSupersedesVersionId() {
		return supersedesVersionId;
	}

	public UUID getCreatedByAdminId() {
		return createdByAdminId;
	}

	public Instant getCreatedAt() {
		return createdAt;
	}

}
