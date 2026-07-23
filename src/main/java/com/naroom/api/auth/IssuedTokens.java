package com.naroom.api.auth;

import com.naroom.api.account.domain.entity.AuthSession;

import java.time.Instant;

// authentication.md 카카오 로그인 성공 응답의 tokenType/accessToken/... 필드와 1:1로 대응한다.
public record IssuedTokens(
		String accessToken,
		Instant accessTokenExpiresAt,
		String refreshToken,
		Instant refreshTokenExpiresAt,
		AuthSession session) {
}
