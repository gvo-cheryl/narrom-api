package com.naroom.api.admin.record.dto;

import com.naroom.api.record.domain.entity.RecordContentLimit;

import java.time.Instant;
import java.util.UUID;

public record AdminRecordContentLimitResponse(Integer bodyMaxLength, UUID updatedByAdminId, Instant updatedAt) {

	public static AdminRecordContentLimitResponse from(RecordContentLimit limit) {
		return new AdminRecordContentLimitResponse(limit.getBodyMaxLength(), limit.getUpdatedByAdminId(), limit.getUpdatedAt());
	}

}
