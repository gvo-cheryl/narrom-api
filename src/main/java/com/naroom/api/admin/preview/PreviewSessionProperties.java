package com.naroom.api.admin.preview;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;

// Admin Web Implementation Spec §16.2: 만료는 15~30분, 새로고침 시 재발급 가능.
@ConfigurationProperties(prefix = "naroom.admin.preview")
public record PreviewSessionProperties(Duration tokenTimeout) {
}
