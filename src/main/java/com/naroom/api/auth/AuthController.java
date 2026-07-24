package com.naroom.api.auth;

import com.naroom.api.auth.dto.KakaoLoginRequest;
import com.naroom.api.auth.dto.KakaoLoginResponse;
import com.naroom.api.auth.dto.RefreshRequest;
import com.naroom.api.auth.dto.RefreshResponse;
import com.naroom.api.auth.dto.SessionCheckResponse;
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
	private final TokenRefreshService tokenRefreshService;
	private final AuthSessionService authSessionService;
	private final SessionCheckService sessionCheckService;

	public AuthController(
			KakaoLoginService kakaoLoginService,
			TokenRefreshService tokenRefreshService,
			AuthSessionService authSessionService,
			SessionCheckService sessionCheckService) {
		this.kakaoLoginService = kakaoLoginService;
		this.tokenRefreshService = tokenRefreshService;
		this.authSessionService = authSessionService;
		this.sessionCheckService = sessionCheckService;
	}

	@PostMapping("/kakao/login")
	public ApiResponse<KakaoLoginResponse> kakaoLogin(@Valid @RequestBody KakaoLoginRequest request) {
		return ApiResponse.of(kakaoLoginService.login(request));
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
