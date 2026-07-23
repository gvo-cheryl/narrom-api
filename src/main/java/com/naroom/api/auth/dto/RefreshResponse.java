package com.naroom.api.auth.dto;

import java.time.Instant;

// authentication.md 토큰 재발급 응답: 새 Access·Refresh Token과 세션 정보만 반환한다.
public record RefreshResponse(
		String tokenType,
		String accessToken,
		Instant accessTokenExpiresAt,
		String refreshToken,
		Instant refreshTokenExpiresAt,
		SessionSummary session) {
}
