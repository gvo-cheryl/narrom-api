package com.naroom.api.account.domain.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "inquiries")
public class Inquiry {

	@Id
	@GeneratedValue(strategy = GenerationType.UUID)
	@Column(name = "id", nullable = false, updatable = false)
	private UUID id;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "member_id", nullable = false, updatable = false)
	private Member member;

	@Column(name = "content", nullable = false, updatable = false)
	private String content;

	@Column(name = "created_at", nullable = false, updatable = false)
	private Instant createdAt;

	protected Inquiry() {
	}

	private Inquiry(Member member, String content) {
		this.member = member;
		this.content = content;
		this.createdAt = Instant.now();
	}

	public static Inquiry create(Member member, String content) {
		return new Inquiry(member, content);
	}

	public UUID getId() {
		return id;
	}

	public Member getMember() {
		return member;
	}

	public String getContent() {
		return content;
	}

	public Instant getCreatedAt() {
		return createdAt;
	}

}
