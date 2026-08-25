package com.naroom.api.admin.record.dto;

import com.naroom.api.record.domain.entity.EntryType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;

import java.time.Instant;

// code는 사용자가 입력하지 않는다 - 생성 시 서버가 자동으로 부여한다.
public record AdminRecordPromptCreateRequest(
		@NotBlank String questionText,
		String helperText,
		@NotNull EntryType entryType,
		@NotNull @PositiveOrZero Integer displayOrder,
		Instant activeFrom,
		Instant activeUntil) {
}
