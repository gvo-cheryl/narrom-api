package com.naroom.api.admin.auth;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;

// Admin Web Implementation Spec 17.8: idle 30분, absolute 8시간 권장값.
@ConfigurationProperties(prefix = "naroom.admin.session")
public record AdminSessionProperties(
		Duration idleTimeout,
		Duration absoluteTimeout,
		String cookieName,
		String cookieDomain,
		// 로그인 성공 후 돌아갈 naroom-admin 프론트 URL. 프론트 저장소가 아직 없어 당장은 빈 값일 수 있다.
		String frontendRedirectUri) {
}
