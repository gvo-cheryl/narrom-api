package com.naroom.api.admin.preview.dto;

import com.naroom.api.admin.preview.IssuedPreviewSession;

import java.time.Instant;

// rawToken은 이 응답에서만 한 번 내려간다 - 이후에는 조회할 방법이 없다(admin_sessions와 같은 원칙).
public record AdminPreviewSessionResponse(String token, Instant expiresAt) {

	public static AdminPreviewSessionResponse from(IssuedPreviewSession issued) {
		return new AdminPreviewSessionResponse(issued.rawToken(), issued.session().getExpiresAt());
	}

}
