package com.naroom.api.content.dto;

import com.naroom.api.content.domain.entity.Quote;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

public record QuoteResponse(
		UUID id,
		String text,
		String authorName,
		String sourceName,
		String sourceUrl,
		List<QuoteTopicResponse> topics,
		boolean saved) {

	public static QuoteResponse of(Quote quote, boolean saved) {
		return new QuoteResponse(
				quote.getId(),
				quote.getText(),
				quote.getAuthorName(),
				quote.getSourceName(),
				quote.getSourceUrl(),
				quote.getTopics().stream().map(QuoteTopicResponse::from).collect(Collectors.toList()),
				saved);
	}

}
