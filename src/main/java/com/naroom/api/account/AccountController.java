package com.naroom.api.account;

import com.naroom.api.account.domain.entity.NotificationType;
import com.naroom.api.account.dto.AccountWithdrawalResponse;
import com.naroom.api.account.dto.DevicePushTokenUpdateRequest;
import com.naroom.api.account.dto.NotificationPreferenceResponse;
import com.naroom.api.account.dto.NotificationPreferenceUpdateRequest;
import com.naroom.api.account.dto.OnboardingCompleteRequest;
import com.naroom.api.account.dto.OnboardingCompleteResponse;
import com.naroom.api.auth.security.MemberAuthentication;
import com.naroom.api.global.response.ApiResponse;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/account")
public class AccountController {

	private final OnboardingService onboardingService;
	private final DeviceInstallationService deviceInstallationService;
	private final NotificationPreferenceService notificationPreferenceService;
	private final AccountWithdrawalService accountWithdrawalService;

	public AccountController(
			OnboardingService onboardingService,
			DeviceInstallationService deviceInstallationService,
			NotificationPreferenceService notificationPreferenceService,
			AccountWithdrawalService accountWithdrawalService) {
		this.onboardingService = onboardingService;
		this.deviceInstallationService = deviceInstallationService;
		this.notificationPreferenceService = notificationPreferenceService;
		this.accountWithdrawalService = accountWithdrawalService;
	}

	@PostMapping("/onboarding/complete")
	public ApiResponse<OnboardingCompleteResponse> completeOnboarding(@Valid @RequestBody OnboardingCompleteRequest request) {
		return ApiResponse.of(onboardingService.complete(currentMemberId(), request));
	}

	@PatchMapping("/device/push-token")
	@ResponseStatus(HttpStatus.NO_CONTENT)
	public void updateDevicePushToken(@Valid @RequestBody DevicePushTokenUpdateRequest request) {
		deviceInstallationService.updatePushToken(currentMemberId(), request);
	}

	@GetMapping("/notification-preferences")
	public ApiResponse<List<NotificationPreferenceResponse>> getNotificationPreferences() {
		return ApiResponse.of(notificationPreferenceService.list(currentMemberId()));
	}

	@PutMapping("/notification-preferences/{type}")
	public ApiResponse<NotificationPreferenceResponse> updateNotificationPreference(
			@PathVariable NotificationType type, @Valid @RequestBody NotificationPreferenceUpdateRequest request) {
		return ApiResponse.of(notificationPreferenceService.update(currentMemberId(), type, request));
	}

	@PostMapping("/withdrawal")
	public ApiResponse<AccountWithdrawalResponse> requestWithdrawal() {
		return ApiResponse.of(accountWithdrawalService.requestWithdrawal(currentMemberId()));
	}

	// JwtAuthenticationFilter가 SecurityContextHolder에 직접 채워 넣는 방식이라 여기서도 직접 꺼낸다
	// (AuthController.logout()과 동일한 이유).
	private UUID currentMemberId() {
		MemberAuthentication authentication =
				(MemberAuthentication) SecurityContextHolder.getContext().getAuthentication();
		return authentication.getMemberId();
	}

}
