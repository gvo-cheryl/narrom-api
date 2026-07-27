package com.naroom.api.ai.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;

// batchSize·leaseTimeout·pollInterval은 4-B(비동기 실행 방식) 결정에 따라 값으로 튜닝하는 항목이라
// 코드에 고정하지 않고 설정으로 뺐다. executor는 실제 처리기가 붙는 4-I에서 추가했다(claim 배치 크기와
// OpenAI 동시 호출 상한을 분리하기로 한 결정에 따라 DB claim 배치와는 독립적으로 튜닝 가능하다).
@ConfigurationProperties(prefix = "naroom.ai.worker")
public record AiWorkerProperties(
		boolean enabled, int batchSize, Duration leaseTimeout, Duration pollInterval, Executor executor) {

	public record Executor(int coreSize, int maxSize, int queueCapacity) {
	}

}
