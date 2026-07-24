package com.naroom.api.content.domain.entity;

import jakarta.persistence.Embeddable;

import java.io.Serializable;
import java.util.Objects;
import java.util.UUID;

@Embeddable
public class MemberSavedQuoteId implements Serializable {

	private UUID memberId;
	private UUID quoteId;

	protected MemberSavedQuoteId() {
	}

	public MemberSavedQuoteId(UUID memberId, UUID quoteId) {
		this.memberId = memberId;
		this.quoteId = quoteId;
	}

	public UUID getMemberId() {
		return memberId;
	}

	public UUID getQuoteId() {
		return quoteId;
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) {
			return true;
		}
		if (!(o instanceof MemberSavedQuoteId that)) {
			return false;
		}
		return Objects.equals(memberId, that.memberId) && Objects.equals(quoteId, that.quoteId);
	}

	@Override
	public int hashCode() {
		return Objects.hash(memberId, quoteId);
	}

}
