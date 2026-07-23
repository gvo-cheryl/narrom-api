package com.naroom.api.auth.dto;

import jakarta.validation.constraints.NotBlank;

// installationKey는 Bean Validation이 아니라 TokenRefreshService가 직접 검증한다.
// error-response.md가 DEVICE_INSTALLATION_KEY_REQUIRED로 별도 분리해 두었기 때문이다(KakaoLoginRequest와 동일한 이유).
public record RefreshRequest(
		@NotBlank String refreshToken,
		String installationKey) {
}
