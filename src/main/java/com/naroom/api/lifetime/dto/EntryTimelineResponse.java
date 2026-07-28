package com.naroom.api.lifetime.dto;

import com.naroom.api.ai.domain.entity.AiJobStatus;
import com.naroom.api.record.domain.entity.Entry;
import com.naroom.api.record.domain.entity.EntryStatus;
import com.naroom.api.record.domain.entity.EntryType;
import com.naroom.api.record.dto.EntryTagResponse;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public record EntryTimelineResponse(
		UUID id,
		EntryType entryType,
		EntryStatus status,
		String title,
		String body,
		LocalDate recordDate,
		List<EntryTagResponse> tags,
		AiJobStatus aiStatus,
		boolean hasSelfReflection,
		Instant publishedAt,
		Instant createdAt) {

	public static EntryTimelineResponse of(
			Entry entry, List<EntryTagResponse> tags, AiJobStatus aiStatus, boolean hasSelfReflection) {
		return new EntryTimelineResponse(
				entry.getId(),
				entry.getEntryType(),
				entry.getStatus(),
				entry.getTitle(),
				entry.getBody(),
				entry.getRecordDate(),
				tags,
				aiStatus,
				hasSelfReflection,
				entry.getPublishedAt(),
				entry.getCreatedAt());
	}

}
