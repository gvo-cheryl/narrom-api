package com.naroom.api.record.dto;

import com.naroom.api.record.domain.entity.EntryType;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;
import java.util.UUID;

public record EntryCreateRequest(
		@NotNull EntryType entryType,
		@Size(max = 200) String title,
		String body,
		@NotNull LocalDate recordDate,
		UUID parentEntryId,
		UUID quoteId,
		String promptSnapshot) {
}
