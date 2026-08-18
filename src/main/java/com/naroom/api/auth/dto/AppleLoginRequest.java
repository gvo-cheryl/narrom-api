package com.naroom.api.auth.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

// fullName은 Apple이 최초 승인 응답에서만 내려주는 값이라 재로그인 시에는 비어 있을 수 있다(선택값).
public record AppleLoginRequest(
		@NotBlank String identityToken,
		@NotBlank String rawNonce,
		String fullName,
		@Valid @NotNull DeviceInfo device) {
}
