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

// 감정·생각·관계 등 작은 실험의 주제 분류. Flyway 시드(V16)로 최초 채워지고, 이후에는
// 관리자 웹(com.naroom.api.admin.experiment)에서 명시적으로 수정한다. code는 다른 테이블이
// 참조하는 안정 식별자라 생성 후 불변으로 둔다.
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

	private ExperimentTopic(String code, String name, String description, int displayOrder, boolean active) {
		this.code = code;
		this.name = name;
		this.description = description;
		this.displayOrder = displayOrder;
		this.active = active;
	}

	public static ExperimentTopic create(String code, String name, String description, int displayOrder, boolean active) {
		return new ExperimentTopic(code, name, description, displayOrder, active);
	}

	public void update(String name, String description, int displayOrder, boolean active) {
		this.name = name;
		this.description = description;
		this.displayOrder = displayOrder;
		this.active = active;
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
