package com.naroom.api.admin.content.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.Instant;
import java.util.Set;
import java.util.UUID;

// code는 사용자가 입력하지 않는다 - 생성 시 서버가 자동으로 부여한다.
public record AdminQuoteCreateRequest(
		@NotBlank String text,
		String authorName,
		String sourceName,
		String sourceUrl,
		@NotNull Set<UUID> topicIds,
		Instant activeFrom,
		Instant activeUntil) {
}
