package com.naroom.api.auth.kakao;

import com.naroom.api.account.domain.entity.SocialProvider;
import com.naroom.api.auth.social.SocialProviderClient;
import com.naroom.api.auth.social.SocialUserInfo;
import org.springframework.stereotype.Component;

// KakaoClient(provider access token으로 카카오 API를 직접 호출)를 SocialLoginService가 쓰는
// 공통 SocialProviderClient 형태로 어댑팅한다.
@Component
public class KakaoSocialProviderClient implements SocialProviderClient {

	private final KakaoClient kakaoClient;

	public KakaoSocialProviderClient(KakaoClient kakaoClient) {
		this.kakaoClient = kakaoClient;
	}

	@Override
	public SocialProvider provider() {
		return SocialProvider.KAKAO;
	}

	@Override
	public SocialUserInfo fetchUserInfo(String credential) {
		KakaoUserInfoResponse kakaoUser = kakaoClient.fetchUserInfo(credential);
		return new SocialUserInfo(
				String.valueOf(kakaoUser.id()),
				kakaoUser.email(),
				kakaoUser.emailVerified(),
				kakaoUser.nickname(),
				kakaoUser.profileImageUrl());
	}

}
