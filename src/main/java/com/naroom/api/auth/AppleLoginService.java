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

}
