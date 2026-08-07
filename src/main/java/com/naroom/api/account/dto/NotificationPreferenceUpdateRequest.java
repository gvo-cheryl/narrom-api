package com.naroom.api.account.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

import java.time.LocalTime;

public record NotificationPreferenceUpdateRequest(
		@NotNull Boolean enabled,
		@Min(1) @Max(7) Integer dayOfWeek,
		LocalTime localTime) {
}
