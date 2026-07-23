package com.naroom.api.account;

import com.naroom.api.account.domain.entity.ConsentType;
import com.naroom.api.account.domain.entity.MemberStatus;
import com.naroom.api.account.domain.error.AccountErrorCode;
import com.naroom.api.account.domain.repository.AuthSessionRepository;
import com.naroom.api.account.dto.AccountSummary;
import com.naroom.api.account.dto.OnboardingCompleteResponse;
import com.naroom.api.auth.NextAction;
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
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(AccountController.class)
@Import({
		ProblemDetailFactory.class,
		SecurityConfig.class,
		ApiAuthenticationEntryPoint.class,
		ApiAccessDeniedHandler.class,
		SecurityProblemWriter.class
})
class AccountControllerTest {

	@Autowired
	private MockMvc mockMvc;

	@MockitoBean
	private OnboardingService onboardingService;

	@MockitoBean
	private JwtTokenProvider jwtTokenProvider;

	@MockitoBean
	private AuthSessionRepository authSessionRepository;

	@Test
	void completeOnboarding_authenticated_returns200() throws Exception {
		UUID memberId = UUID.randomUUID();
		when(onboardingService.complete(any(), any())).thenReturn(new OnboardingCompleteResponse(
				new AccountSummary(memberId, MemberStatus.ACTIVE, Instant.now(), 1L),
				NextAction.ENTER_APP));

		mockMvc.perform(post("/api/v1/account/onboarding/complete")
						.with(authentication(new MemberAuthentication(memberId, UUID.randomUUID())))
						.contentType("application/json")
						.content(requestJson(0)))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.data.nextAction").value("ENTER_APP"))
				.andExpect(jsonPath("$.data.account.memberId").value(memberId.toString()));
	}

	@Test
	void completeOnboarding_withoutAuthentication_returnsAuthRequired() throws Exception {
		mockMvc.perform(post("/api/v1/account/onboarding/complete")
						.contentType("application/json")
						.content(requestJson(0)))
				.andExpect(status().isUnauthorized())
				.andExpect(jsonPath("$.code").value("AUTH_REQUIRED"));
	}

	@Test
	void completeOnboarding_versionConflict_returnsProblemDetail() throws Exception {
		when(onboardingService.complete(any(), any()))
				.thenThrow(new BusinessException(AccountErrorCode.ACCOUNT_VERSION_CONFLICT));

		mockMvc.perform(post("/api/v1/account/onboarding/complete")
						.with(authentication(new MemberAuthentication(UUID.randomUUID(), UUID.randomUUID())))
						.contentType("application/json")
						.content(requestJson(0)))
				.andExpect(status().isConflict())
				.andExpect(jsonPath("$.code").value("ACCOUNT_VERSION_CONFLICT"));
	}

	@Test
	void completeOnboarding_missingConsents_returnsValidationFailed() throws Exception {
		mockMvc.perform(post("/api/v1/account/onboarding/complete")
						.with(authentication(new MemberAuthentication(UUID.randomUUID(), UUID.randomUUID())))
						.contentType("application/json")
						.content("""
								{
								  "version": 0,
								  "displayName": "지연",
								  "timezone": "Asia/Seoul",
								  "locale": "ko-KR",
								  "consents": []
								}
								"""))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.code").value("COMMON_VALIDATION_FAILED"));
	}

	private String requestJson(int version) {
		return """
				{
				  "version": %d,
				  "displayName": "지연",
				  "timezone": "Asia/Seoul",
				  "locale": "ko-KR",
				  "consents": [
				    { "type": "%s", "documentVersion": "1.0", "agreed": true },
				    { "type": "%s", "documentVersion": "1.0", "agreed": true },
				    { "type": "%s", "documentVersion": "1.0", "agreed": true }
				  ]
				}
				""".formatted(version, ConsentType.TERMS, ConsentType.PRIVACY, ConsentType.AI_PROCESSING);
	}

}
