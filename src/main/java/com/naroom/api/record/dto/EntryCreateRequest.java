package com.naroom.api.record.dto;

import com.naroom.api.record.domain.entity.EntryType;
import com.naroom.api.record.domain.entity.RecordContentLimit;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;
import java.util.UUID;

public record EntryCreateRequest(
		@NotNull EntryType entryType,
		@Size(max = 200) String title,
		// 실제 적용되는 글자 수 제한은 record_content_limits(관리자 설정, EntryService.requireBodyWithinLimit)다 -
		// 이 상한은 그보다 항상 커야 하는 절대 기술적 한계일 뿐이다.
		@Size(max = RecordContentLimit.HARD_MAX_BODY_LENGTH) String body,
		@NotNull LocalDate recordDate,
		UUID parentEntryId,
		UUID quoteId,
		String promptSnapshot) {
}
