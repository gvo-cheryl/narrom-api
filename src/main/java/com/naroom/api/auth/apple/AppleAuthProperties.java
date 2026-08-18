package com.naroom.api.auth.apple;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.List;

// 값은 application.yml(naroom.auth.apple.*)에서 오고, APPLE_OAUTH_CLIENT_ID(iOS 앱 Bundle ID) placeholder로
// 위임된다. Apple 로그인은 Beta 1 범위에서 iOS 전용이라 Google과 달리 플랫폼별 값이 하나뿐이지만, 나중에
// Services ID(웹) 등을 추가할 수 있게 목록 형태를 유지한다.
@ConfigurationProperties(prefix = "naroom.auth.apple")
public record AppleAuthProperties(List<String> allowedAudiences) {
}
