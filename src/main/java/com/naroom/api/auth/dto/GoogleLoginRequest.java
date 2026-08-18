package com.naroom.api.auth.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record GoogleLoginRequest(
		@NotBlank String idToken,
		@Valid @NotNull DeviceInfo device) {
}
