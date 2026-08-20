package com.naroom.api.record.domain.entity;

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

// 기록 시작 질문 마스터(§9.2). entries.prompt_snapshot이 기록 당시 질문 원문을 이미 스냅샷으로 남기므로
// (com.naroom.api.record.domain.entity.Entry), 발행본을 그대로 UPDATE하지 않고 code당 새 row(versionNo 증가)로만
// 바꾼다 - 과거 기록이 참조하는 질문의 의미가 바뀌지 않게 하기 위함이다.
@Entity
@Table(name = "record_prompts")
public class RecordPrompt {

	@Id
	@GeneratedValue(strategy = GenerationType.UUID)
	@Column(name = "id", nullable = false, updatable = false)
	private UUID id;

	@Column(name = "code", nullable = false, updatable = false, length = 80)
	private String code;

	@Column(name = "version_no", nullable = false, updatable = false)
	private int versionNo;

	@Column(name = "question_text", nullable = false)
	private String questionText;

	@Column(name = "helper_text")
	private String helperText;

	@Enumerated(EnumType.STRING)
	@JdbcTypeCode(SqlTypes.NAMED_ENUM)
	@Column(name = "entry_type", nullable = false)
	private EntryType entryType;

	@Column(name = "display_order", nullable = false)
	private int displayOrder;

	@Enumerated(EnumType.STRING)
	@JdbcTypeCode(SqlTypes.NAMED_ENUM)
	@Column(name = "status", nullable = false)
	private RecordPromptStatus status;

	@Column(name = "active_from")
	private Instant activeFrom;

	@Column(name = "active_until")
	private Instant activeUntil;

	// admin_users는 별도 신원 체계(com.naroom.api.admin)라 엔티티 연관관계 대신 원문 UUID만 보관한다.
	@Column(name = "supersedes_prompt_id", updatable = false)
	private UUID supersedesPromptId;

	@Column(name = "created_by_admin_id", nullable = false, updatable = false)
	private UUID createdByAdminId;

	@CreationTimestamp
	@Column(name = "created_at", nullable = false, updatable = false)
	private Instant createdAt;

	@UpdateTimestamp
	@Column(name = "updated_at", nullable = false)
	private Instant updatedAt;

	@Version
	@Column(name = "version", nullable = false)
	private Long version;

	protected RecordPrompt() {
	}

	private RecordPrompt(
			String code,
			int versionNo,
			String questionText,
			String helperText,
			EntryType entryType,
			int displayOrder,
			Instant activeFrom,
			Instant activeUntil,
			UUID supersedesPromptId,
			UUID createdByAdminId) {
		this.code = code;
		this.versionNo = versionNo;
		this.questionText = questionText;
		this.helperText = helperText;
		this.entryType = entryType;
		this.displayOrder = displayOrder;
		this.status = RecordPromptStatus.DRAFT;
		this.activeFrom = activeFrom;
		this.activeUntil = activeUntil;
		this.supersedesPromptId = supersedesPromptId;
		this.createdByAdminId = createdByAdminId;
	}

	public static RecordPrompt create(
			String code,
			int versionNo,
			String questionText,
			String helperText,
			EntryType entryType,
			int displayOrder,
			Instant activeFrom,
			Instant activeUntil,
			UUID supersedesPromptId,
			UUID createdByAdminId) {
		return new RecordPrompt(
				code, versionNo, questionText, helperText, entryType, displayOrder, activeFrom, activeUntil,
				supersedesPromptId, createdByAdminId);
	}

	// DRAFT 상태에서만 그대로 수정한다 - 이미 PUBLISHED된 버전은 절대 UPDATE하지 않는다.
	public void updateDraft(
			String questionText, String helperText, EntryType entryType, int displayOrder,
			Instant activeFrom, Instant activeUntil) {
		this.questionText = questionText;
		this.helperText = helperText;
		this.entryType = entryType;
		this.displayOrder = displayOrder;
		this.activeFrom = activeFrom;
		this.activeUntil = activeUntil;
	}

	public void publish() {
		this.status = RecordPromptStatus.PUBLISHED;
	}

	public void archive() {
		this.status = RecordPromptStatus.ARCHIVED;
	}

	public UUID getId() {
		return id;
	}

	public String getCode() {
		return code;
	}

	public int getVersionNo() {
		return versionNo;
	}

	public String getQuestionText() {
		return questionText;
	}

	public String getHelperText() {
		return helperText;
	}

	public EntryType getEntryType() {
		return entryType;
	}

	public int getDisplayOrder() {
		return displayOrder;
	}

	public RecordPromptStatus getStatus() {
		return status;
	}

	public Instant getActiveFrom() {
		return activeFrom;
	}

	public Instant getActiveUntil() {
		return activeUntil;
	}

	public UUID getSupersedesPromptId() {
		return supersedesPromptId;
	}

	public UUID getCreatedByAdminId() {
		return createdByAdminId;
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
