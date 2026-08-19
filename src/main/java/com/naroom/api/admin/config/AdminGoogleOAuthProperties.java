package com.naroom.api.admin.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

// 실제 값이 없어도(로컬/CI) 부팅이 깨지지 않도록 플레이스홀더 기본값을 쓴다 - 자세한 이유는
// AdminSecurityConfig의 clientRegistrationRepository() 주석 참고.
@ConfigurationProperties(prefix = "naroom.admin.google-oauth")
public record AdminGoogleOAuthProperties(String clientId, String clientSecret) {
}
