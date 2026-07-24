package com.naroom.api.content.domain.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.util.UUID;

@Entity
@Table(name = "quote_topics")
public class QuoteTopic {

	@Id
	@GeneratedValue(strategy = GenerationType.UUID)
	@Column(name = "id", nullable = false, updatable = false)
	private UUID id;

	@Column(name = "code", nullable = false, length = 50, unique = true)
	private String code;

	@Column(name = "name", nullable = false, length = 80)
	private String name;

	@Column(name = "active", nullable = false)
	private boolean active;

	protected QuoteTopic() {
	}

	private QuoteTopic(String code, String name) {
		this.code = code;
		this.name = name;
		this.active = true;
	}

	public static QuoteTopic create(String code, String name) {
		return new QuoteTopic(code, name);
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

	public boolean isActive() {
		return active;
	}

}
