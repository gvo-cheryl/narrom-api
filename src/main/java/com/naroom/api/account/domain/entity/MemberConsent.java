package com.naroom.api.account.domain.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.Instant;
import java.util.UUID;

// agreed_at까지 unique 키에 포함되어 있어, 재동의(재로그인 등)마다 새 행이 쌓이는 이력 테이블이다. upsert 대상이 아니다.
@Entity
@Table(
		name = "member_consents",
		uniqueConstraints = @UniqueConstraint(
				columnNames = {"member_id", "consent_type", "document_version", "agreed_at"}))
public class MemberConsent {

	@Id
	@GeneratedValue(strategy = GenerationType.UUID)
	@Column(name = "id", nullable = false, updatable = false)
	private UUID id;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "member_id", nullable = false, updatable = false)
	private Member member;

	@Enumerated(EnumType.STRING)
	@JdbcTypeCode(SqlTypes.NAMED_ENUM)
	@Column(name = "consent_type", nullable = false, updatable = false)
	private ConsentType consentType;

	@Column(name = "document_version", nullable = false, length = 30, updatable = false)
	private String documentVersion;

	@Column(name = "agreed", nullable = false)
	private boolean agreed;

	@Column(name = "agreed_at", nullable = false, updatable = false)
	private Instant agreedAt;

	@Column(name = "withdrawn_at")
	private Instant withdrawnAt;

	// 예: ONBOARDING, SETTINGS. Java enum 아님, ERD 원본 varchar(30) 그대로.
	@Column(name = "source", nullable = false, length = 30, updatable = false)
	private String source;

	protected MemberConsent() {
	}

	private MemberConsent(
			Member member,
			ConsentType consentType,
			String documentVersion,
			boolean agreed,
			String source) {
		this.member = member;
		this.consentType = consentType;
		this.documentVersion = documentVersion;
		this.agreed = agreed;
		this.agreedAt = Instant.now();
		this.source = source;
	}

	public static MemberConsent record(
			Member member,
			ConsentType consentType,
			String documentVersion,
			boolean agreed,
			String source) {
		return new MemberConsent(member, consentType, documentVersion, agreed, source);
	}

	public UUID getId() {
		return id;
	}

	public Member getMember() {
		return member;
	}

	public ConsentType getConsentType() {
		return consentType;
	}

	public String getDocumentVersion() {
		return documentVersion;
	}

	public boolean isAgreed() {
		return agreed;
	}

	public Instant getAgreedAt() {
		return agreedAt;
	}

	public Instant getWithdrawnAt() {
		return withdrawnAt;
	}

	public String getSource() {
		return source;
	}

}
