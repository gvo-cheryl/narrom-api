package com.naroom.api.admin.record.dto;

import com.naroom.api.record.domain.entity.EntryType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;

import java.time.Instant;

// code/versionNo는 불변이라 수정 대상에서 제외한다. DRAFT 상태에서만 호출 가능하다.
public record AdminRecordPromptUpdateRequest(
		@NotBlank String questionText,
		String helperText,
		@NotNull EntryType entryType,
		@NotNull @PositiveOrZero Integer displayOrder,
		Instant activeFrom,
		Instant activeUntil) {
}
