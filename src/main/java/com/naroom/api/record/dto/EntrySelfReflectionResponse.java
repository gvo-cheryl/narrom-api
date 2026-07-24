package com.naroom.api.record.dto;

import com.naroom.api.record.domain.entity.EntrySelfReflection;

import java.time.Instant;
import java.util.UUID;

public record EntrySelfReflectionResponse(UUID id, String content, Instant createdAt, Instant updatedAt) {

	public static EntrySelfReflectionResponse from(EntrySelfReflection reflection) {
		return new EntrySelfReflectionResponse(
				reflection.getId(), reflection.getContent(), reflection.getCreatedAt(), reflection.getUpdatedAt());
	}

}
