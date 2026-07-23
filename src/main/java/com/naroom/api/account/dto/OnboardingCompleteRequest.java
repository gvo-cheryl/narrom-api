package com.naroom.api.account.dto;

import com.naroom.api.account.domain.entity.ConsentType;
import com.naroom.api.account.domain.entity.NotificationType;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.time.LocalTime;
import java.util.List;

// authentication.md 온보딩 완료 요청과 1:1로 대응한다.
public record OnboardingCompleteRequest(
		@NotNull Long version,
		@NotBlank String displayName,
		@NotBlank String timezone,
		@NotBlank String locale,
		@NotEmpty List<@Valid ConsentRequest> consents,
		List<@Valid NotificationPreferenceRequest> notificationPreferences) {

	public record ConsentRequest(
			@NotNull ConsentType type,
			@NotBlank String documentVersion,
			@NotNull Boolean agreed) {
	}

	public record NotificationPreferenceRequest(
			@NotNull NotificationType type,
			@NotNull Boolean enabled,
			@Min(1) @Max(7) Integer dayOfWeek,
			LocalTime localTime) {
	}

}
