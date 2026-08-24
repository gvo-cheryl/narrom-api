package com.naroom.api.admin.content.dto;

import com.naroom.api.appcontent.domain.entity.AppContentValueType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.Instant;

public record AdminAppContentItemUpdateRequest(
		@NotBlank String surface,
		@NotNull AppContentValueType valueType,
		String valueText,
		String valueJson,
		@NotBlank String schemaVersion,
		Instant activeFrom,
		Instant activeUntil,
		@NotNull Boolean fallbackRequired) {
}
