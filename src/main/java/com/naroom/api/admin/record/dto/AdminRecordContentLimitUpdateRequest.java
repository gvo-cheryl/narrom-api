package com.naroom.api.admin.record.dto;

import com.naroom.api.record.domain.entity.RecordContentLimit;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public record AdminRecordContentLimitUpdateRequest(
		@NotNull @Min(1) @Max(RecordContentLimit.HARD_MAX_BODY_LENGTH) Integer bodyMaxLength) {
}
