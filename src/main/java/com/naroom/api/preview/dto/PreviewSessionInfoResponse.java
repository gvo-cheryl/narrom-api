package com.naroom.api.preview.dto;

import com.naroom.api.preview.auth.PreviewAuthentication;

import java.util.Map;
import java.util.UUID;

public record PreviewSessionInfoResponse(
		UUID previewSessionId, Map<String, UUID> selectedContentVersions, String scenarioKey) {

	public static PreviewSessionInfoResponse from(PreviewAuthentication authentication) {
		return new PreviewSessionInfoResponse(
				authentication.getPreviewSessionId(),
				authentication.getSelectedContentVersions(),
				authentication.getScenarioKey());
	}

}
