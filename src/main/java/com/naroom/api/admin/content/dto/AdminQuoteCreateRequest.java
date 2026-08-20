package com.naroom.api.admin.content.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.Instant;
import java.util.Set;
import java.util.UUID;

public record AdminQuoteCreateRequest(
		@NotBlank String code,
		@NotBlank String text,
		String authorName,
		String sourceName,
		String sourceUrl,
		@NotNull Set<UUID> topicIds,
		Instant activeFrom,
		Instant activeUntil) {
}
