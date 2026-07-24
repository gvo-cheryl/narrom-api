package com.naroom.api.auth;

import com.naroom.api.account.domain.entity.MemberStatus;
import com.naroom.api.account.domain.repository.AuthSessionRepository;
import com.naroom.api.account.dto.AccountSummary;
import com.naroom.api.auth.domain.error.AuthErrorCode;
import com.naroom.api.auth.dto.KakaoLoginResponse;
import com.naroom.api.auth.dto.RefreshResponse;
import com.naroom.api.auth.dto.SessionCheckResponse;
import com.naroom.api.auth.dto.SessionSummary;
import com.naroom.api.auth.security.JwtTokenProvider;
import com.naroom.api.auth.security.MemberAuthentication;
import com.naroom.api.global.config.SecurityConfig;
import com.naroom.api.global.error.exception.BusinessException;
import com.naroom.api.global.error.response.ProblemDetailFactory;
import com.naroom.api.global.security.ApiAccessDeniedHandler;
import com.naroom.api.global.security.ApiAuthenticationEntryPoint;
import com.naroom.api.global.security.SecurityProblemWriter;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

// @WebMvcTest 슬라이스는 SecurityConfig(@Configuration)를 자동 포함하지 않는다 — Filter 타입인
// JwtAuthenticationFilter만 자동 포함되고, authorizeHttpRequests 규칙 자체는 SecurityConfig를 명시적으로
// import해야 실제로 적용된다(logout처럼 인증이 필요한 경로를 검증하려면 필수).
@WebMvcTest(AuthController.class)
@Import({
		ProblemDetailFactory.class,
		SecurityConfig.class,
		ApiAuthenticationEntryPoint.class,
		ApiAccessDeniedHandler.class,
		SecurityProblemWriter.class
})
class AuthControllerTest {

	@Autowired
	private MockMvc mockMvc;

	@MockitoBean
	private KakaoLoginService kakaoLoginService;

	@MockitoBean
	private TokenRefreshService tokenRefreshService;

	@MockitoBean
	private AuthSessionService authSessionService;

	@MockitoBean
	private SessionCheckService sessionCheckService;

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
				new SessionSummary(sessionId, now.plusSeconds(1_209_600)),
				new AccountSummary(memberId, "지연", MemberStatus.ACTIVE, null, 0L),
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

	@Test
	void refresh_returns200WithNewTokens() throws Exception {
		UUID sessionId = UUID.randomUUID();
		Instant now = Instant.now();
		when(tokenRefreshService.refresh(any())).thenReturn(new RefreshResponse(
				"Bearer",
				"new-access-token",
				now.plusSeconds(3600),
				"new-refresh-token",
				now.plusSeconds(1_209_600),
				new SessionSummary(sessionId, now.plusSeconds(1_209_600))));

		mockMvc.perform(post("/api/v1/auth/refresh")
						.contentType("application/json")
						.content(refreshRequestJson("old-refresh-token", "installation-key")))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.data.accessToken").value("new-access-token"))
				.andExpect(jsonPath("$.data.session.id").value(sessionId.toString()));
	}

	@Test
	void refresh_invalidToken_returnsProblemDetail() throws Exception {
		when(tokenRefreshService.refresh(any()))
				.thenThrow(new BusinessException(AuthErrorCode.AUTH_REFRESH_TOKEN_INVALID));

		mockMvc.perform(post("/api/v1/auth/refresh")
						.contentType("application/json")
						.content(refreshRequestJson("bad-refresh-token", "installation-key")))
				.andExpect(status().isUnauthorized())
				.andExpect(jsonPath("$.code").value("AUTH_REFRESH_TOKEN_INVALID"));
	}

	@Test
	void session_authenticated_returns200WithAccountState() throws Exception {
		UUID memberId = UUID.randomUUID();
		UUID sessionId = UUID.randomUUID();
		Instant now = Instant.now();
		when(sessionCheckService.check(memberId, sessionId)).thenReturn(new SessionCheckResponse(
				true,
				new SessionSummary(sessionId, now.plusSeconds(1_209_600)),
				new AccountSummary(memberId, "지연", MemberStatus.ACTIVE, null, 0L),
				NextAction.COMPLETE_ONBOARDING));

		mockMvc.perform(get("/api/v1/auth/session")
						.with(authentication(new MemberAuthentication(memberId, sessionId))))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.data.authenticated").value(true))
				.andExpect(jsonPath("$.data.nextAction").value("COMPLETE_ONBOARDING"))
				.andExpect(jsonPath("$.data.account.memberId").value(memberId.toString()));
	}

	@Test
	void session_withoutAuthentication_returnsAuthRequired() throws Exception {
		mockMvc.perform(get("/api/v1/auth/session"))
				.andExpect(status().isUnauthorized())
				.andExpect(jsonPath("$.code").value("AUTH_REQUIRED"));
	}

	@Test
	void session_lockedMember_returnsProblemDetail() throws Exception {
		UUID memberId = UUID.randomUUID();
		UUID sessionId = UUID.randomUUID();
		when(sessionCheckService.check(memberId, sessionId))
				.thenThrow(new BusinessException(AuthErrorCode.ACCOUNT_LOCKED));

		mockMvc.perform(get("/api/v1/auth/session")
						.with(authentication(new MemberAuthentication(memberId, sessionId))))
				.andExpect(status().isForbidden())
				.andExpect(jsonPath("$.code").value("ACCOUNT_LOCKED"));
	}

	@Test
	void logout_authenticated_returns204AndRevokesSession() throws Exception {
		UUID memberId = UUID.randomUUID();
		UUID sessionId = UUID.randomUUID();

		mockMvc.perform(post("/api/v1/auth/logout")
						.with(authentication(new MemberAuthentication(memberId, sessionId))))
				.andExpect(status().isNoContent());

		verify(authSessionService).revoke(sessionId, "LOGOUT");
	}

	@Test
	void logout_withoutAuthentication_returnsAuthRequired() throws Exception {
		mockMvc.perform(post("/api/v1/auth/logout"))
				.andExpect(status().isUnauthorized())
				.andExpect(jsonPath("$.code").value("AUTH_REQUIRED"));
	}

	private String refreshRequestJson(String refreshToken, String installationKey) {
		return """
				{
				  "refreshToken": "%s",
				  "installationKey": "%s"
				}
				""".formatted(refreshToken, installationKey);
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
