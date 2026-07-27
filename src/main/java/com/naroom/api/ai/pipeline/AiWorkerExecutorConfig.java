package com.naroom.api.ai.pipeline;

import com.naroom.api.ai.config.AiWorkerProperties;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executor;

// naroom.ai.worker.enabled가 true일 때만 생성한다. Spring Boot 기본 @Async 실행기(SimpleAsyncTaskExecutor)는
// 호출마다 스레드를 새로 만들고 풀링하지 않아 처리량이 튀면 스레드가 무제한으로 늘어난다 - 이를 피하려고
// core/max 풀 크기와 큐 용량을 명시한 전용 실행기를 둔다(claim 배치 크기와는 독립적으로 튜닝 가능).
@Configuration
public class AiWorkerExecutorConfig {

	@Bean
	@ConditionalOnProperty(prefix = "naroom.ai.worker", name = "enabled", havingValue = "true")
	public Executor aiJobExecutor(AiWorkerProperties properties) {
		ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
		executor.setCorePoolSize(properties.executor().coreSize());
		executor.setMaxPoolSize(properties.executor().maxSize());
		executor.setQueueCapacity(properties.executor().queueCapacity());
		executor.setThreadNamePrefix("ai-job-");
		executor.initialize();
		return executor;
	}

}
