package com.naroom.api.auth.dto;

import com.naroom.api.account.dto.AccountSummary;
import com.naroom.api.auth.NextAction;

import java.time.Instant;

// authentication.md 카카오 로그인 성공 응답과 1:1로 대응한다.
public record KakaoLoginResponse(
		String tokenType,
		String accessToken,
		Instant accessTokenExpiresAt,
		String refreshToken,
		Instant refreshTokenExpiresAt,
		SessionSummary session,
		AccountSummary account,
		NextAction nextAction) {
}
