package com.naroom.api.record.dto;

import com.naroom.api.record.domain.entity.Entry;
import com.naroom.api.record.domain.entity.EntryStatus;
import com.naroom.api.record.domain.entity.EntryType;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

public record EntryResponse(
		UUID id,
		EntryType entryType,
		EntryStatus status,
		String title,
		String body,
		LocalDate recordDate,
		UUID parentEntryId,
		UUID quoteId,
		boolean aiProcessingAllowed,
		Instant publishedAt,
		Instant createdAt,
		Instant updatedAt,
		Long version) {

	public static EntryResponse from(Entry entry) {
		return new EntryResponse(
				entry.getId(),
				entry.getEntryType(),
				entry.getStatus(),
				entry.getTitle(),
				entry.getBody(),
				entry.getRecordDate(),
				entry.getParentEntry() == null ? null : entry.getParentEntry().getId(),
				entry.getQuote() == null ? null : entry.getQuote().getId(),
				entry.isAiProcessingAllowed(),
				entry.getPublishedAt(),
				entry.getCreatedAt(),
				entry.getUpdatedAt(),
				entry.getVersion());
	}

}
