package com.naroom.api.auth.google;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.List;

// 값은 application.yml(naroom.auth.google.*)에서 오고, GOOGLE_OAUTH_IOS_CLIENT_ID/ANDROID/WEB
// placeholder 3개로 위임된다. 셋 다 허용 audience로 등록한다(플랫폼별 분기 없이 단순 허용 목록).
@ConfigurationProperties(prefix = "naroom.auth.google")
public record GoogleAuthProperties(List<String> allowedAudiences) {
}
