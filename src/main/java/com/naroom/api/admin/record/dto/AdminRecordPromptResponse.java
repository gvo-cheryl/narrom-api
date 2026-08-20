package com.naroom.api.admin.record.dto;

import com.naroom.api.record.domain.entity.EntryType;
import com.naroom.api.record.domain.entity.RecordPrompt;
import com.naroom.api.record.domain.entity.RecordPromptStatus;

import java.time.Instant;
import java.util.UUID;

public record AdminRecordPromptResponse(
		UUID id,
		String code,
		int versionNo,
		String questionText,
		String helperText,
		EntryType entryType,
		int displayOrder,
		RecordPromptStatus status,
		Instant activeFrom,
		Instant activeUntil,
		UUID supersedesPromptId,
		UUID createdByAdminId,
		Instant createdAt,
		Instant updatedAt) {

	public static AdminRecordPromptResponse from(RecordPrompt prompt) {
		return new AdminRecordPromptResponse(
				prompt.getId(),
				prompt.getCode(),
				prompt.getVersionNo(),
				prompt.getQuestionText(),
				prompt.getHelperText(),
				prompt.getEntryType(),
				prompt.getDisplayOrder(),
				prompt.getStatus(),
				prompt.getActiveFrom(),
				prompt.getActiveUntil(),
				prompt.getSupersedesPromptId(),
				prompt.getCreatedByAdminId(),
				prompt.getCreatedAt(),
				prompt.getUpdatedAt());
	}

}
