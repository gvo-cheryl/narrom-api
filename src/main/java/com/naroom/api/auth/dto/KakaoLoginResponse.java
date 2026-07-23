package com.naroom.api.auth.dto;

import com.naroom.api.account.domain.entity.MemberStatus;
import com.naroom.api.auth.NextAction;

import java.time.Instant;
import java.util.UUID;

// authentication.md 카카오 로그인 성공 응답과 1:1로 대응한다.
public record KakaoLoginResponse(
		String tokenType,
		String accessToken,
		Instant accessTokenExpiresAt,
		String refreshToken,
		Instant refreshTokenExpiresAt,
		SessionSummary session,
		Account account,
		NextAction nextAction) {

	public record Account(UUID memberId, MemberStatus status, Instant onboardingCompletedAt, Long version) {
	}

}
