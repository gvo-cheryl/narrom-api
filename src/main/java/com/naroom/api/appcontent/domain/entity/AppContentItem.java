package com.naroom.api.appcontent.domain.entity;

import com.naroom.api.appcontent.domain.error.AppContentErrorCode;
import com.naroom.api.global.error.exception.BusinessException;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.UpdateTimestamp;
import org.hibernate.type.SqlTypes;

import java.time.Instant;
import java.util.UUID;

// 기타 앱 문구(홈 인사말, 안내, placeholder, 빈 상태 등) 마스터(Admin Web Implementation Spec §11).
// record_prompts/quotes와 같은 패턴: 발행본은 절대 UPDATE하지 않고 같은 content_key+locale의 새 row
// (version_no 증가)로만 바꾼다.
@Entity
@Table(name = "app_content_items")
public class AppContentItem {

	@Id
	@GeneratedValue(strategy = GenerationType.UUID)
	@Column(name = "id", nullable = false, updatable = false)
	private UUID id;

	@Column(name = "content_key", nullable = false, updatable = false, length = 120)
	private String contentKey;

	@Column(name = "surface", nullable = false, length = 60)
	private String surface;

	@Column(name = "locale", nullable = false, updatable = false, length = 10)
	private String locale;

	@Enumerated(EnumType.STRING)
	@JdbcTypeCode(SqlTypes.NAMED_ENUM)
	@Column(name = "value_type", nullable = false)
	private AppContentValueType valueType;

	@Column(name = "value_text")
	private String valueText;

	// 구조화된 문장 묶음(value_type=JSON)일 때만 채워지는 원문 JSON. schema_version이 이 구조를 식별한다.
	@Column(name = "value_json", columnDefinition = "jsonb")
	@JdbcTypeCode(SqlTypes.JSON)
	private String valueJson;

	@Column(name = "schema_version", nullable = false, length = 20)
	private String schemaVersion;

	@Column(name = "version_no", nullable = false, updatable = false)
	private int versionNo;

	@Enumerated(EnumType.STRING)
	@JdbcTypeCode(SqlTypes.NAMED_ENUM)
	@Column(name = "status", nullable = false)
	private AppContentItemStatus status;

	@Column(name = "active_from")
	private Instant activeFrom;

	@Column(name = "active_until")
	private Instant activeUntil;

	@Column(name = "fallback_required", nullable = false)
	private boolean fallbackRequired;

	@Column(name = "created_by_admin_id", nullable = false, updatable = false)
	private UUID createdByAdminId;

	// 다른 app_content_items row를 가리키는 이력 정보라 연관관계 대신 원문 UUID만 보관한다(RecordPrompt/Quote와 동일한 이유).
	@Column(name = "supersedes_item_id", updatable = false)
	private UUID supersedesItemId;

	@CreationTimestamp
	@Column(name = "created_at", nullable = false, updatable = false)
	private Instant createdAt;

	@UpdateTimestamp
	@Column(name = "updated_at", nullable = false)
	private Instant updatedAt;

	@Version
	@Column(name = "version", nullable = false)
	private Long version;

	protected AppContentItem() {
	}

	private AppContentItem(
			String contentKey,
			String surface,
			String locale,
			AppContentValueType valueType,
			String valueText,
			String valueJson,
			String schemaVersion,
			int versionNo,
			Instant activeFrom,
			Instant activeUntil,
			boolean fallbackRequired,
			UUID createdByAdminId,
			UUID supersedesItemId) {
		requireValueMatchesType(valueType, valueText, valueJson);
		this.contentKey = contentKey;
		this.surface = surface;
		this.locale = locale;
		this.valueType = valueType;
		this.valueText = valueText;
		this.valueJson = valueJson;
		this.schemaVersion = schemaVersion;
		this.versionNo = versionNo;
		this.status = AppContentItemStatus.DRAFT;
		this.activeFrom = activeFrom;
		this.activeUntil = activeUntil;
		this.fallbackRequired = fallbackRequired;
		this.createdByAdminId = createdByAdminId;
		this.supersedesItemId = supersedesItemId;
	}

	public static AppContentItem create(
			String contentKey,
			String surface,
			String locale,
			AppContentValueType valueType,
			String valueText,
			String valueJson,
			String schemaVersion,
			int versionNo,
			Instant activeFrom,
			Instant activeUntil,
			boolean fallbackRequired,
			UUID createdByAdminId,
			UUID supersedesItemId) {
		return new AppContentItem(
				contentKey, surface, locale, valueType, valueText, valueJson, schemaVersion, versionNo,
				activeFrom, activeUntil, fallbackRequired, createdByAdminId, supersedesItemId);
	}

	// DRAFT 상태에서만 그대로 수정한다 - 이미 PUBLISHED된 버전은 절대 UPDATE하지 않는다(§11.6).
	public void updateDraft(
			String surface,
			AppContentValueType valueType,
			String valueText,
			String valueJson,
			String schemaVersion,
			Instant activeFrom,
			Instant activeUntil,
			boolean fallbackRequired) {
		requireValueMatchesType(valueType, valueText, valueJson);
		this.surface = surface;
		this.valueType = valueType;
		this.valueText = valueText;
		this.valueJson = valueJson;
		this.schemaVersion = schemaVersion;
		this.activeFrom = activeFrom;
		this.activeUntil = activeUntil;
		this.fallbackRequired = fallbackRequired;
	}

	public void publish() {
		this.status = AppContentItemStatus.PUBLISHED;
	}

	public void archive() {
		this.status = AppContentItemStatus.ARCHIVED;
	}

	// DB CHECK 제약(V28)과 동일한 규칙을 애플리케이션 계층에서도 먼저 검증해 원문 DataIntegrityViolationException
	// 대신 명확한 BusinessException을 던진다.
	private static void requireValueMatchesType(AppContentValueType valueType, String valueText, String valueJson) {
		boolean valid = switch (valueType) {
			case TEXT -> valueText != null && !valueText.isBlank() && valueJson == null;
			case JSON -> valueJson != null && !valueJson.isBlank() && valueText == null;
		};
		if (!valid) {
			throw new BusinessException(AppContentErrorCode.VALUE_TYPE_MISMATCH);
		}
	}

	public UUID getId() {
		return id;
	}

	public String getContentKey() {
		return contentKey;
	}

	public String getSurface() {
		return surface;
	}

	public String getLocale() {
		return locale;
	}

	public AppContentValueType getValueType() {
		return valueType;
	}

	public String getValueText() {
		return valueText;
	}

	public String getValueJson() {
		return valueJson;
	}

	public String getSchemaVersion() {
		return schemaVersion;
	}

	public int getVersionNo() {
		return versionNo;
	}

	public AppContentItemStatus getStatus() {
		return status;
	}

	public Instant getActiveFrom() {
		return activeFrom;
	}

	public Instant getActiveUntil() {
		return activeUntil;
	}

	public boolean isFallbackRequired() {
		return fallbackRequired;
	}

	public UUID getCreatedByAdminId() {
		return createdByAdminId;
	}

	public UUID getSupersedesItemId() {
		return supersedesItemId;
	}

	public Instant getCreatedAt() {
		return createdAt;
	}

	public Instant getUpdatedAt() {
		return updatedAt;
	}

	public Long getVersion() {
		return version;
	}

}
