package com.naroom.api.auth;

import com.naroom.api.account.domain.entity.SocialProvider;
import com.naroom.api.auth.dto.GoogleLoginRequest;
import com.naroom.api.auth.dto.SocialLoginResponse;
import com.naroom.api.auth.social.SocialLoginService;
import org.springframework.stereotype.Service;

@Service
public class GoogleLoginService {

	private final SocialLoginService socialLoginService;

	public GoogleLoginService(SocialLoginService socialLoginService) {
		this.socialLoginService = socialLoginService;
	}

	public SocialLoginResponse login(GoogleLoginRequest request) {
		return socialLoginService.login(SocialProvider.GOOGLE, request.idToken(), request.device());
	}

}
