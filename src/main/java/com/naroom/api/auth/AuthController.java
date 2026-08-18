package com.naroom.api.auth;

import com.naroom.api.auth.dto.AppleLoginRequest;
import com.naroom.api.auth.dto.GoogleLoginRequest;
import com.naroom.api.auth.dto.KakaoLoginRequest;
import com.naroom.api.auth.dto.RefreshRequest;
import com.naroom.api.auth.dto.RefreshResponse;
import com.naroom.api.auth.dto.SessionCheckResponse;
import com.naroom.api.auth.dto.SocialLoginResponse;
import com.naroom.api.auth.security.MemberAuthentication;
import com.naroom.api.global.response.ApiResponse;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {

	private static final String LOGOUT_REVOKE_REASON = "LOGOUT";

	private final KakaoLoginService kakaoLoginService;
	private final GoogleLoginService googleLoginService;
	private final AppleLoginService appleLoginService;
	private final TokenRefreshService tokenRefreshService;
	private final AuthSessionService authSessionService;
	private final SessionCheckService sessionCheckService;

	public AuthController(
			KakaoLoginService kakaoLoginService,
			GoogleLoginService googleLoginService,
			AppleLoginService appleLoginService,
			TokenRefreshService tokenRefreshService,
			AuthSessionService authSessionService,
			SessionCheckService sessionCheckService) {
		this.kakaoLoginService = kakaoLoginService;
		this.googleLoginService = googleLoginService;
		this.appleLoginService = appleLoginService;
		this.tokenRefreshService = tokenRefreshService;
		this.authSessionService = authSessionService;
		this.sessionCheckService = sessionCheckService;
	}

	@PostMapping("/kakao/login")
	public ApiResponse<SocialLoginResponse> kakaoLogin(@Valid @RequestBody KakaoLoginRequest request) {
		return ApiResponse.of(kakaoLoginService.login(request));
	}

	// 탈퇴 유예(PENDING_DELETION) 상태에서 카카오 재인증으로 본인 확인 후 명시적으로 복구를 확인하는
	// 전용 엔드포인트다 - kakao/login과 분리해, 일반 로그인 시도가 계정을 자동 복구하지 않게 한다.
	@PostMapping("/restore")
	public ApiResponse<SocialLoginResponse> restore(@Valid @RequestBody KakaoLoginRequest request) {
		return ApiResponse.of(kakaoLoginService.restore(request));
	}

	@PostMapping("/google/login")
	public ApiResponse<SocialLoginResponse> googleLogin(@Valid @RequestBody GoogleLoginRequest request) {
		return ApiResponse.of(googleLoginService.login(request));
	}

	// 탈퇴 유예(PENDING_DELETION) 상태에서 Google 재인증으로 본인 확인 후 명시적으로 복구를 확인하는
	// 전용 엔드포인트다 - google/login과 분리해, 일반 로그인 시도가 계정을 자동 복구하지 않게 한다.
	@PostMapping("/google/restore")
	public ApiResponse<SocialLoginResponse> googleRestore(@Valid @RequestBody GoogleLoginRequest request) {
		return ApiResponse.of(googleLoginService.restore(request));
	}

	@PostMapping("/apple/login")
	public ApiResponse<SocialLoginResponse> appleLogin(@Valid @RequestBody AppleLoginRequest request) {
		return ApiResponse.of(appleLoginService.login(request));
	}

	// 탈퇴 유예(PENDING_DELETION) 상태에서 Apple 재인증으로 본인 확인 후 명시적으로 복구를 확인하는
	// 전용 엔드포인트다 - apple/login과 분리해, 일반 로그인 시도가 계정을 자동 복구하지 않게 한다.
	@PostMapping("/apple/restore")
	public ApiResponse<SocialLoginResponse> appleRestore(@Valid @RequestBody AppleLoginRequest request) {
		return ApiResponse.of(appleLoginService.restore(request));
	}

	@PostMapping("/refresh")
	public ApiResponse<RefreshResponse> refresh(@Valid @RequestBody RefreshRequest request) {
		return ApiResponse.of(tokenRefreshService.refresh(request));
	}

	@GetMapping("/session")
	public ApiResponse<SessionCheckResponse> session() {
		// JwtAuthenticationFilter가 SecurityContextHolder에 직접 채워 넣는 방식이라 여기서도 직접 꺼낸다
		// (Authentication 파라미터 자동 바인딩에 의존하지 않는다).
		MemberAuthentication authentication =
				(MemberAuthentication) SecurityContextHolder.getContext().getAuthentication();
		return ApiResponse.of(sessionCheckService.check(authentication.getMemberId(), authentication.getSessionId()));
	}

	@PostMapping("/logout")
	@ResponseStatus(HttpStatus.NO_CONTENT)
	public void logout() {
		// JwtAuthenticationFilter가 SecurityContextHolder에 직접 채워 넣는 방식이라 여기서도 직접 꺼낸다
		// (Authentication 파라미터 자동 바인딩에 의존하지 않는다).
		MemberAuthentication authentication =
				(MemberAuthentication) SecurityContextHolder.getContext().getAuthentication();
		authSessionService.revoke(authentication.getSessionId(), LOGOUT_REVOKE_REASON);
	}

}
