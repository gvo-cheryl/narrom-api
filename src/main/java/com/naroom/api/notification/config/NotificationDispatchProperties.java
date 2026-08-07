package com.naroom.api.notification.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;

// AiWorkerProperties와 같은 이유로 poll-interval을 설정으로 뺐다 - 예약 시각과의 허용 오차 범위이기도
// 하다(§2 DEC-02: 배치 주기 안에 들어오면 발송, 이미 그날 보냈으면 건너뜀).
@ConfigurationProperties(prefix = "naroom.notification.dispatch")
public record NotificationDispatchProperties(boolean enabled, Duration pollInterval) {
}
