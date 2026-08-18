package com.naroom.api.auth.social;

// provider별 로그인 요청이 실어 보내는 값. token 외 나머지는 대부분의 provider에서 null이다:
// Apple만 replay 방지를 위한 rawNonce와, identity token에 이름이 없어 최초 승인 응답으로만 받을 수 있는
// fullNameHint를 함께 쓴다.
public record SocialCredential(String token, String rawNonce, String fullNameHint) {

	public static SocialCredential of(String token) {
		return new SocialCredential(token, null, null);
	}

}
