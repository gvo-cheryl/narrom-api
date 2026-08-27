package com.naroom.api.record.dto;

import com.naroom.api.record.domain.entity.RecordContentLimit;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record EntryUpdateRequest(
		@Size(max = 200) String title,
		@Size(max = RecordContentLimit.HARD_MAX_BODY_LENGTH) String body,
		@NotNull Long version) {
}
