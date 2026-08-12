package com.naroom.api.global.config;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

// naroom.scheduling.enabled=false로 테스트에서만 끈다(build.gradle.kts의 Test 태스크 참고).
// @SpringBootTest는 전체 컨텍스트를 띄우므로, 이 설정 없이는 AI 워커·알림 발송 스케줄러가 테스트 중에도
// 실제로 폴링하며 다른 테스트가 만든 행을 커밋해 버려 카운트 기반 단정을 어긋나게 한다.
@Configuration
@ConditionalOnProperty(prefix = "naroom.scheduling", name = "enabled", havingValue = "true", matchIfMissing = true)
@EnableScheduling
public class SchedulingConfig {
}
