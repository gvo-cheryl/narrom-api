package com.naroom.api.admin.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.List;

// naroom-admin(별도 origin)이 admin_sessions 쿠키로 인증하려면 크리덴셜 포함 CORS가 필수다(Admin Web
// Implementation Spec 4.3). allowCredentials(true)와는 와일드카드 origin을 함께 쓸 수 없어 allowlist로 받는다.
@ConfigurationProperties(prefix = "naroom.admin.cors")
public record AdminCorsProperties(List<String> allowedOrigins) {
}
