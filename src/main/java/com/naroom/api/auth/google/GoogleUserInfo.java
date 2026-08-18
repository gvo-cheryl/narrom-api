package com.naroom.api.auth.google;

// Google ID Token claim 중 로그인에 필요한 값만 정규화한 것. email/name/picture는 계정 상태에 따라 null일 수 있다.
public record GoogleUserInfo(
		String sub,
		String email,
		boolean emailVerified,
		String name,
		String picture) {
}
