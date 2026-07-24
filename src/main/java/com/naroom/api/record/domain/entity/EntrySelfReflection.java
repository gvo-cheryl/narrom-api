package com.naroom.api.record.domain.entity;

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

// ai_reflection_id는 reference 스키마상 ai_reflections를 참조하지만 AI 도메인이 아직 없어
// FK 없는 순수 컬럼으로만 둔다(V5 마이그레이션 주석 참고). AI 도메인 구현 시 FK와 연관관계를 추가한다.
// 하나의 기록에 여러 개의 자기회고가 있을 수 있다(reference 스키마에 entry_id UNIQUE 제약 없음).
@Entity
@Table(name = "entry_self_reflections")
public class EntrySelfReflection {

	@Id
	@GeneratedValue(strategy = GenerationType.UUID)
	@Column(name = "id", nullable = false, updatable = false)
	private UUID id;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "entry_id", nullable = false, updatable = false)
	private Entry entry;

	@Column(name = "ai_reflection_id", updatable = false)
	private UUID aiReflectionId;

	@Column(name = "content", nullable = false)
	private String content;

	@CreationTimestamp
	@Column(name = "created_at", nullable = false, updatable = false)
	private Instant createdAt;

	@UpdateTimestamp
	@Column(name = "updated_at", nullable = false)
	private Instant updatedAt;

	protected EntrySelfReflection() {
	}

	private EntrySelfReflection(Entry entry, UUID aiReflectionId, String content) {
		this.entry = entry;
		this.aiReflectionId = aiReflectionId;
		this.content = content;
	}

	public static EntrySelfReflection create(Entry entry, String content) {
		return new EntrySelfReflection(entry, null, content);
	}

	public static EntrySelfReflection createFromAiReflection(Entry entry, UUID aiReflectionId, String content) {
		return new EntrySelfReflection(entry, aiReflectionId, content);
	}

	public void update(String content) {
		this.content = content;
	}

	public UUID getId() {
		return id;
	}

	public Entry getEntry() {
		return entry;
	}

	public UUID getAiReflectionId() {
		return aiReflectionId;
	}

	public String getContent() {
		return content;
	}

	public Instant getCreatedAt() {
		return createdAt;
	}

	public Instant getUpdatedAt() {
		return updatedAt;
	}

}
