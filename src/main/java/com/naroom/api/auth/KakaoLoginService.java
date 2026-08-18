package com.naroom.api.auth;

import com.naroom.api.account.domain.entity.SocialProvider;
import com.naroom.api.auth.dto.KakaoLoginRequest;
import com.naroom.api.auth.dto.SocialLoginResponse;
import com.naroom.api.auth.social.SocialLoginService;
import org.springframework.stereotype.Service;

@Service
public class KakaoLoginService {

	private final SocialLoginService socialLoginService;

	public KakaoLoginService(SocialLoginService socialLoginService) {
		this.socialLoginService = socialLoginService;
	}

	public SocialLoginResponse login(KakaoLoginRequest request) {
		return socialLoginService.login(SocialProvider.KAKAO, request.providerAccessToken(), request.device());
	}

	// 탈퇴 유예(PENDING_DELETION) 상태에서 카카오 재인증으로 본인 확인 후 명시적으로 복구를 확인하는
	// 전용 엔드포인트다 - kakao/login과 분리해, 일반 로그인 시도가 계정을 자동 복구하지 않게 한다.
	public SocialLoginResponse restore(KakaoLoginRequest request) {
		return socialLoginService.restore(SocialProvider.KAKAO, request.providerAccessToken(), request.device());
	}

}
