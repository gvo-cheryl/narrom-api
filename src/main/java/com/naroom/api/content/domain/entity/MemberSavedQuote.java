package com.naroom.api.content.domain.entity;

import com.naroom.api.account.domain.entity.Member;
import jakarta.persistence.Column;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.MapsId;
import jakarta.persistence.Table;

import java.time.Instant;

@Entity
@Table(name = "member_saved_quotes")
public class MemberSavedQuote {

	@EmbeddedId
	private MemberSavedQuoteId id;

	@ManyToOne(fetch = FetchType.LAZY)
	@MapsId("memberId")
	@JoinColumn(name = "member_id", nullable = false, updatable = false)
	private Member member;

	@ManyToOne(fetch = FetchType.LAZY)
	@MapsId("quoteId")
	@JoinColumn(name = "quote_id", nullable = false, updatable = false)
	private Quote quote;

	@Column(name = "saved_at", nullable = false, updatable = false)
	private Instant savedAt;

	protected MemberSavedQuote() {
	}

	private MemberSavedQuote(Member member, Quote quote) {
		this.member = member;
		this.quote = quote;
		this.id = new MemberSavedQuoteId(member.getId(), quote.getId());
		this.savedAt = Instant.now();
	}

	public static MemberSavedQuote save(Member member, Quote quote) {
		return new MemberSavedQuote(member, quote);
	}

	public MemberSavedQuoteId getId() {
		return id;
	}

	public Member getMember() {
		return member;
	}

	public Quote getQuote() {
		return quote;
	}

	public Instant getSavedAt() {
		return savedAt;
	}

}
