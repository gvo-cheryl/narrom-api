package com.naroom.api.auth.dto;

// installationKey/platform은 Bean Validation이 아니라 SocialLoginService가 직접 검증한다.
// error-response.md가 이 둘을 COMMON_VALIDATION_FAILED가 아닌 DEVICE_* 전용 코드로 분리해 두었기 때문이다.
public record DeviceInfo(
		String installationKey,
		String platform,
		String appVersion) {
}
