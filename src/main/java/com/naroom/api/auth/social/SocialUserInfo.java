package com.naroom.api.auth.social;

// provider별 원본 응답(카카오 사용자 정보, Google/Apple ID Token claim)을 공통 형태로 정규화한 것.
public record SocialUserInfo(
		String providerUserId,
		String email,
		boolean emailVerified,
		String profileName,
		String profileImageUrl) {
}
