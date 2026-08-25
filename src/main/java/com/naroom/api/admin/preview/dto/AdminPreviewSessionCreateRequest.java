package com.naroom.api.admin.preview.dto;

import jakarta.validation.constraints.NotNull;

import java.util.Map;
import java.util.UUID;

public record AdminPreviewSessionCreateRequest(
		@NotNull Map<String, UUID> selectedContentVersions,
		String scenarioKey) {
}
