package com.naroom.api.experiment.domain.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.Instant;
import java.util.UUID;

// 감정·생각·관계 등 작은 실험의 주제 분류. 콘텐츠 원본이라 Flyway 시드(V16)로만 채워지고
// 별도 작성 API는 두지 않는다.
@Entity
@Table(name = "experiment_topics")
public class ExperimentTopic {

	@Id
	@GeneratedValue(strategy = GenerationType.UUID)
	@Column(name = "id", nullable = false, updatable = false)
	private UUID id;

	@Column(name = "code", nullable = false, updatable = false, length = 50)
	private String code;

	@Column(name = "name", nullable = false, length = 80)
	private String name;

	@Column(name = "description")
	private String description;

	@Column(name = "display_order", nullable = false)
	private int displayOrder;

	@Column(name = "active", nullable = false)
	private boolean active;

	@CreationTimestamp
	@Column(name = "created_at", nullable = false, updatable = false)
	private Instant createdAt;

	@UpdateTimestamp
	@Column(name = "updated_at", nullable = false)
	private Instant updatedAt;

	protected ExperimentTopic() {
	}

	public UUID getId() {
		return id;
	}

	public String getCode() {
		return code;
	}

	public String getName() {
		return name;
	}

	public String getDescription() {
		return description;
	}

	public int getDisplayOrder() {
		return displayOrder;
	}

	public boolean isActive() {
		return active;
	}

	public Instant getCreatedAt() {
		return createdAt;
	}

	public Instant getUpdatedAt() {
		return updatedAt;
	}

}
