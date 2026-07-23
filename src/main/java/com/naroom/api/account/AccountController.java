package com.naroom.api.account;

import com.naroom.api.account.dto.OnboardingCompleteRequest;
import com.naroom.api.account.dto.OnboardingCompleteResponse;
import com.naroom.api.auth.security.MemberAuthentication;
import com.naroom.api.global.response.ApiResponse;
import jakarta.validation.Valid;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/account")
public class AccountController {

	private final OnboardingService onboardingService;

	public AccountController(OnboardingService onboardingService) {
		this.onboardingService = onboardingService;
	}

	@PostMapping("/onboarding/complete")
	public ApiResponse<OnboardingCompleteResponse> completeOnboarding(@Valid @RequestBody OnboardingCompleteRequest request) {
		// JwtAuthenticationFilter가 SecurityContextHolder에 직접 채워 넣는 방식이라 여기서도 직접 꺼낸다
		// (AuthController.logout()과 동일한 이유).
		MemberAuthentication authentication =
				(MemberAuthentication) SecurityContextHolder.getContext().getAuthentication();
		return ApiResponse.of(onboardingService.complete(authentication.getMemberId(), request));
	}

}
