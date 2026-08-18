package com.naroom.api.auth.dto;

import com.naroom.api.account.dto.AccountSummary;
import com.naroom.api.auth.NextAction;

import java.time.Instant;

// authentication.md 소셜 로그인(카카오·Google) 성공 응답과 1:1로 대응한다. provider와 무관하게 형태가 같다.
public record SocialLoginResponse(
		String tokenType,
		String accessToken,
		Instant accessTokenExpiresAt,
		String refreshToken,
		Instant refreshTokenExpiresAt,
		SessionSummary session,
		AccountSummary account,
		NextAction nextAction) {
}
