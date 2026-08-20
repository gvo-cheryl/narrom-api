package com.naroom.api.admin.record.dto;

import com.naroom.api.record.domain.entity.EntryType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;

import java.time.Instant;

public record AdminRecordPromptCreateRequest(
		@NotBlank String code,
		@NotBlank String questionText,
		String helperText,
		@NotNull EntryType entryType,
		@NotNull @PositiveOrZero Integer displayOrder,
		Instant activeFrom,
		Instant activeUntil) {
}
