package com.naroom.api.content.dto;

import com.naroom.api.content.domain.entity.MemberSavedQuote;

import java.time.Instant;

public record SavedQuoteResponse(QuoteResponse quote, Instant savedAt) {

	public static SavedQuoteResponse from(MemberSavedQuote savedQuote) {
		return new SavedQuoteResponse(QuoteResponse.of(savedQuote.getQuote(), true), savedQuote.getSavedAt());
	}

}
