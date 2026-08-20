package com.naroom.api.admin.content.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.Instant;
import java.util.Set;
import java.util.UUID;

// code/versionNo는 불변이라 수정 대상에서 제외한다. DRAFT 상태에서만 호출 가능하다.
public record AdminQuoteUpdateRequest(
		@NotBlank String text,
		String authorName,
		String sourceName,
		String sourceUrl,
		@NotNull Set<UUID> topicIds,
		Instant activeFrom,
		Instant activeUntil) {
}
