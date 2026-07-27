package com.naroom.api.ai.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;

// batchSize·leaseTimeout·pollInterval은 4-B(비동기 실행 방식) 결정에 따라 값으로 튜닝하는 항목이라
// 코드에 고정하지 않고 설정으로 뺐다. bounded 실행기(스레드풀) 설정은 실제 처리기가 붙는 4-D에서 추가한다.
@ConfigurationProperties(prefix = "naroom.ai.worker")
public record AiWorkerProperties(boolean enabled, int batchSize, Duration leaseTimeout, Duration pollInterval) {
}
