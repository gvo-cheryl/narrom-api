package com.naroom.api.auth;

import com.naroom.api.account.domain.entity.MemberStatus;
import com.naroom.api.account.domain.repository.AuthSessionRepository;
import com.naroom.api.auth.domain.error.AuthErrorCode;
import com.naroom.api.auth.dto.KakaoLoginResponse;
import com.naroom.api.auth.security.JwtTokenProvider;
import com.naroom.api.global.error.exception.BusinessException;
import com.naroom.api.global.error.response.ProblemDetailFactory;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

// SecurityConfig가 /api/v1/auth/kakao/login을 permitAll로 등록해 두었으므로 인증 없이 호출된다(health와 동일한 이유로
// JwtTokenProvider/AuthSessionRepository를 mock으로 채워야 슬라이스가 기동된다).
@WebMvcTest(AuthController.class)
@Import(ProblemDetailFactory.class)
class AuthControllerTest {

	@Autowired
	private MockMvc mockMvc;

	@MockitoBean
	private KakaoLoginService kakaoLoginService;

	@MockitoBean
	private JwtTokenProvider jwtTokenProvider;

	@MockitoBean
	private AuthSessionRepository authSessionRepository;

	@Test
	void kakaoLogin_returns200WithTokens() throws Exception {
		UUID memberId = UUID.randomUUID();
		UUID sessionId = UUID.randomUUID();
		Instant now = Instant.now();
		when(kakaoLoginService.login(any())).thenReturn(new KakaoLoginResponse(
				"Bearer",
				"access-token",
				now.plusSeconds(3600),
				"refresh-token",
				now.plusSeconds(1_209_600),
				new KakaoLoginResponse.Session(sessionId, now.plusSeconds(1_209_600)),
				new KakaoLoginResponse.Account(memberId, MemberStatus.ACTIVE, null, 0L),
				NextAction.COMPLETE_ONBOARDING));

		mockMvc.perform(post("/api/v1/auth/kakao/login")
						.contentType("application/json")
						.content(loginRequestJson("kakao-provider-token", "installation-key")))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.data.accessToken").value("access-token"))
				.andExpect(jsonPath("$.data.nextAction").value("COMPLETE_ONBOARDING"))
				.andExpect(jsonPath("$.data.account.memberId").value(memberId.toString()));
	}

	@Test
	void kakaoLogin_invalidKakaoToken_returnsProblemDetail() throws Exception {
		when(kakaoLoginService.login(any()))
				.thenThrow(new BusinessException(AuthErrorCode.AUTH_KAKAO_TOKEN_INVALID));

		mockMvc.perform(post("/api/v1/auth/kakao/login")
						.contentType("application/json")
						.content(loginRequestJson("kakao-provider-token", "installation-key")))
				.andExpect(status().isUnauthorized())
				.andExpect(jsonPath("$.code").value("AUTH_KAKAO_TOKEN_INVALID"));
	}

	@Test
	void kakaoLogin_blankProviderAccessToken_returnsValidationFailed() throws Exception {
		mockMvc.perform(post("/api/v1/auth/kakao/login")
						.contentType("application/json")
						.content(loginRequestJson("", "installation-key")))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.code").value("COMMON_VALIDATION_FAILED"));
	}

	private String loginRequestJson(String providerAccessToken, String installationKey) {
		return """
				{
				  "providerAccessToken": "%s",
				  "device": {
				    "installationKey": "%s",
				    "platform": "IOS",
				    "appVersion": "1.0.0"
				  }
				}
				""".formatted(providerAccessToken, installationKey);
	}

}
