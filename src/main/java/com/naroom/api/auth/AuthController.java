package com.naroom.api.auth;

import com.naroom.api.auth.dto.KakaoLoginRequest;
import com.naroom.api.auth.dto.KakaoLoginResponse;
import com.naroom.api.global.response.ApiResponse;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {

	private final KakaoLoginService kakaoLoginService;

	public AuthController(KakaoLoginService kakaoLoginService) {
		this.kakaoLoginService = kakaoLoginService;
	}

	@PostMapping("/kakao/login")
	public ApiResponse<KakaoLoginResponse> kakaoLogin(@Valid @RequestBody KakaoLoginRequest request) {
		return ApiResponse.of(kakaoLoginService.login(request));
	}

}
