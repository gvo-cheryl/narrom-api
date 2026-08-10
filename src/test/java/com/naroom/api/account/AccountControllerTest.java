package com.naroom.api.account;

import com.naroom.api.account.domain.entity.ConsentType;
import com.naroom.api.account.domain.entity.MemberStatus;
import com.naroom.api.account.domain.entity.NotificationType;
import com.naroom.api.account.domain.error.AccountErrorCode;
import com.naroom.api.account.domain.repository.AuthSessionRepository;
import com.naroom.api.account.dto.AccountSummary;
import com.naroom.api.account.dto.AccountWithdrawalResponse;
import com.naroom.api.account.dto.NotificationPreferenceResponse;
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
import java.time.LocalTime;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
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
	private DeviceInstallationService deviceInstallationService;

	@MockitoBean
	private NotificationPreferenceService notificationPreferenceService;

	@MockitoBean
	private AccountWithdrawalService accountWithdrawalService;

	@MockitoBean
	private JwtTokenProvider jwtTokenProvider;

	@MockitoBean
	private AuthSessionRepository authSessionRepository;

	@Test
	void completeOnboarding_authenticated_returns200() throws Exception {
		UUID memberId = UUID.randomUUID();
		when(onboardingService.complete(any(), any())).thenReturn(new OnboardingCompleteResponse(
				new AccountSummary(memberId, "지연", MemberStatus.ACTIVE, Instant.now(), 1L),
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

	@Test
	void updateDevicePushToken_authenticated_returns204() throws Exception {
		mockMvc.perform(patch("/api/v1/account/device/push-token")
						.with(authentication(new MemberAuthentication(UUID.randomUUID(), UUID.randomUUID())))
						.contentType("application/json")
						.content("""
								{ "installationKey": "device-1", "pushToken": "ExponentPushToken[abc]" }
								"""))
				.andExpect(status().isNoContent());
		verify(deviceInstallationService).updatePushToken(any(), any());
	}

	@Test
	void updateDevicePushToken_deviceNotOwned_returnsProblemDetail() throws Exception {
		doThrow(new BusinessException(AccountErrorCode.DEVICE_INSTALLATION_NOT_FOUND))
				.when(deviceInstallationService).updatePushToken(any(), any());

		mockMvc.perform(patch("/api/v1/account/device/push-token")
						.with(authentication(new MemberAuthentication(UUID.randomUUID(), UUID.randomUUID())))
						.contentType("application/json")
						.content("""
								{ "installationKey": "device-1", "pushToken": "ExponentPushToken[abc]" }
								"""))
				.andExpect(status().isNotFound())
				.andExpect(jsonPath("$.code").value("DEVICE_INSTALLATION_NOT_FOUND"));
	}

	@Test
	void getNotificationPreferences_authenticated_returnsAllTypes() throws Exception {
		when(notificationPreferenceService.list(any())).thenReturn(List.of(
				new NotificationPreferenceResponse(NotificationType.WEEKLY_REFLECTION, false, null, null),
				new NotificationPreferenceResponse(NotificationType.EXPERIMENT_MISSION, false, null, null),
				new NotificationPreferenceResponse(NotificationType.DAILY_QUOTE, false, null, null)));

		mockMvc.perform(get("/api/v1/account/notification-preferences")
						.with(authentication(new MemberAuthentication(UUID.randomUUID(), UUID.randomUUID()))))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.data.length()").value(3));
	}

	@Test
	void updateNotificationPreference_authenticated_returnsUpdated() throws Exception {
		when(notificationPreferenceService.update(any(), eq(NotificationType.DAILY_QUOTE), any()))
				.thenReturn(new NotificationPreferenceResponse(NotificationType.DAILY_QUOTE, true, LocalTime.of(9, 0), null));

		mockMvc.perform(put("/api/v1/account/notification-preferences/DAILY_QUOTE")
						.with(authentication(new MemberAuthentication(UUID.randomUUID(), UUID.randomUUID())))
						.contentType("application/json")
						.content("""
								{ "enabled": true, "localTime": "09:00:00" }
								"""))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.data.enabled").value(true));
	}

	@Test
	void requestWithdrawal_authenticated_returnsScheduledDeletionAt() throws Exception {
		Instant scheduledDeletionAt = Instant.parse("2026-08-14T00:00:00Z");
		when(accountWithdrawalService.requestWithdrawal(any()))
				.thenReturn(new AccountWithdrawalResponse(scheduledDeletionAt));

		mockMvc.perform(post("/api/v1/account/withdrawal")
						.with(authentication(new MemberAuthentication(UUID.randomUUID(), UUID.randomUUID()))))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.data.scheduledDeletionAt").value("2026-08-14T00:00:00Z"));
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
