package com.naroom.api.lifetime.dto;

import com.naroom.api.lifetime.domain.entity.PersonalSummary;
import com.naroom.api.lifetime.domain.entity.SummaryScope;

import java.time.Instant;
import java.util.UUID;

// 실제 글 내용은 personal_summaries가 아니라 entry.getBody()(SELF_SUMMARY)에 있다(1단계 설계).
public record PersonalSummaryResponse(
		UUID id,
		SummaryScope scope,
		String content,
		boolean archived,
		Instant archivedAt,
		Instant createdAt,
		Instant updatedAt) {

	public static PersonalSummaryResponse from(PersonalSummary summary) {
		return new PersonalSummaryResponse(
				summary.getId(),
				summary.getScope(),
				summary.getEntry().getBody(),
				summary.getArchivedAt() != null,
				summary.getArchivedAt(),
				summary.getCreatedAt(),
				summary.getUpdatedAt());
	}

}
