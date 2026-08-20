package com.naroom.api.admin.content.dto;

import com.naroom.api.content.domain.entity.Quote;
import com.naroom.api.content.domain.entity.QuoteStatus;
import com.naroom.api.content.domain.entity.QuoteTopic;

import java.time.Instant;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

public record AdminQuoteResponse(
		UUID id,
		String code,
		int versionNo,
		String text,
		String authorName,
		String sourceName,
		String sourceUrl,
		QuoteStatus status,
		Instant activeFrom,
		Instant activeUntil,
		UUID supersedesQuoteId,
		UUID createdByAdminId,
		Set<UUID> topicIds,
		Instant createdAt,
		Instant updatedAt) {

	public static AdminQuoteResponse from(Quote quote) {
		return new AdminQuoteResponse(
				quote.getId(),
				quote.getCode(),
				quote.getVersionNo(),
				quote.getText(),
				quote.getAuthorName(),
				quote.getSourceName(),
				quote.getSourceUrl(),
				quote.getStatus(),
				quote.getActiveFrom(),
				quote.getActiveUntil(),
				quote.getSupersedesQuoteId(),
				quote.getCreatedByAdminId(),
				quote.getTopics().stream().map(QuoteTopic::getId).collect(Collectors.toSet()),
				quote.getCreatedAt(),
				quote.getUpdatedAt());
	}

}
