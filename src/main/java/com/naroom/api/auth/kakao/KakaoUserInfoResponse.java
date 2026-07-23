package com.naroom.api.auth.kakao;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

// 카카오 GET /v2/user/me 응답 중 로그인에 필요한 필드만 매핑한다. kakao_account/profile은
// 동의 항목에 따라 null일 수 있다.
@JsonIgnoreProperties(ignoreUnknown = true)
public record KakaoUserInfoResponse(
		@JsonProperty("id") Long id,
		@JsonProperty("kakao_account") KakaoAccount kakaoAccount) {

	@JsonIgnoreProperties(ignoreUnknown = true)
	public record KakaoAccount(
			@JsonProperty("email") String email,
			@JsonProperty("is_email_verified") boolean emailVerified,
			@JsonProperty("profile") Profile profile) {

		@JsonIgnoreProperties(ignoreUnknown = true)
		public record Profile(
				@JsonProperty("nickname") String nickname,
				@JsonProperty("profile_image_url") String profileImageUrl) {
		}
	}

	public String email() {
		return kakaoAccount == null ? null : kakaoAccount.email();
	}

	public boolean emailVerified() {
		return kakaoAccount != null && kakaoAccount.emailVerified();
	}

	public String nickname() {
		return kakaoAccount == null || kakaoAccount.profile() == null ? null : kakaoAccount.profile().nickname();
	}

	public String profileImageUrl() {
		return kakaoAccount == null || kakaoAccount.profile() == null ? null : kakaoAccount.profile().profileImageUrl();
	}

}
