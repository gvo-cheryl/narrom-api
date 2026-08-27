package com.naroom.api.record.domain.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.Instant;
import java.util.UUID;

// 단일 행 설정 테이블 - id는 마이그레이션이 심어둔 고정값 하나뿐이다(SINGLETON_ID). 관리자가 바꾸기 전까지는
// 기존에 하드코딩돼 있던 값(2000)과 동일하게 동작한다.
@Entity
@Table(name = "record_content_limits")
public class RecordContentLimit {

	public static final UUID SINGLETON_ID = UUID.fromString("00000000-0000-0000-0000-000000000001");

	// bodyMaxLength가 아무리 커도 넘을 수 없는 절대 상한 - AI 컨텍스트로 그대로 들어가는 값이라 관리자가
	// 실수로 과도하게 크게 설정해도 비용·성능이 통제 불능이 되지 않도록 막는다. Entry의 body 컬럼(TEXT)
	// 자체는 이 값보다 훨씬 클 수 있지만, 요청 DTO의 @Size(max)도 이 상수와 맞춰 둔다.
	public static final int HARD_MAX_BODY_LENGTH = 20000;

	@Id
	@Column(name = "id", nullable = false, updatable = false)
	private UUID id;

	@Column(name = "body_max_length", nullable = false)
	private Integer bodyMaxLength;

	@Column(name = "updated_by_admin_id")
	private UUID updatedByAdminId;

	@UpdateTimestamp
	@Column(name = "updated_at", nullable = false)
	private Instant updatedAt;

	protected RecordContentLimit() {
	}

	public void update(Integer bodyMaxLength, UUID updatedByAdminId) {
		this.bodyMaxLength = bodyMaxLength;
		this.updatedByAdminId = updatedByAdminId;
	}

	public UUID getId() {
		return id;
	}

	public Integer getBodyMaxLength() {
		return bodyMaxLength;
	}

	public UUID getUpdatedByAdminId() {
		return updatedByAdminId;
	}

	public Instant getUpdatedAt() {
		return updatedAt;
	}

}
