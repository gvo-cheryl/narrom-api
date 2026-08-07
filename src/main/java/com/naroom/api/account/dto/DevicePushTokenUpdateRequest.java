package com.naroom.api.account.dto;

import jakarta.validation.constraints.NotBlank;

public record DevicePushTokenUpdateRequest(
		@NotBlank String installationKey,
		@NotBlank String pushToken) {
}
