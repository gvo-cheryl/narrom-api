package com.naroom.api.preview.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.List;

// naroom-app의 Expo Web preview 빌드(별도 origin)가 이 API를 직접 호출한다. preview token은 쿠키가
// 아니라 X-Preview-Token 헤더로 보내므로 allowCredentials는 필요 없다(Admin Web Implementation Spec §16.5).
@ConfigurationProperties(prefix = "naroom.preview.cors")
public record PreviewCorsProperties(List<String> allowedOrigins) {
}
