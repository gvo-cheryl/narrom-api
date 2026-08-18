package com.naroom.api.auth;

import com.naroom.api.account.domain.entity.SocialProvider;
import com.naroom.api.auth.dto.AppleLoginRequest;
import com.naroom.api.auth.dto.SocialLoginResponse;
import com.naroom.api.auth.social.SocialCredential;
import com.naroom.api.auth.social.SocialLoginService;
import org.springframework.stereotype.Service;

@Service
public class AppleLoginService {

	private final SocialLoginService socialLoginService;

	public AppleLoginService(SocialLoginService socialLoginService) {
		this.socialLoginService = socialLoginService;
	}

	public SocialLoginResponse login(AppleLoginRequest request) {
		return socialLoginService.login(
				SocialProvider.APPLE,
				new SocialCredential(request.identityToken(), request.rawNonce(), request.fullName()),
				request.device());
	}

	// 탈퇴 유예(PENDING_DELETION) 상태에서 Apple 재인증으로 본인 확인 후 명시적으로 복구를 확인하는
	// 전용 엔드포인트다 - apple/login과 분리해, 일반 로그인 시도가 계정을 자동 복구하지 않게 한다.
	public SocialLoginResponse restore(AppleLoginRequest request) {
		return socialLoginService.restore(
				SocialProvider.APPLE,
				new SocialCredential(request.identityToken(), request.rawNonce(), request.fullName()),
				request.device());
	}

}
