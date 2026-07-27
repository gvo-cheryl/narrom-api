package com.naroom.api.ai;

import com.naroom.api.ai.config.AiWorkerProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

// naroom.ai.worker.enabled가 true일 때만 활성화된다. 큐 선점(claimNextBatch)을 실제로 트리거하는 스케줄러는
// 처리기가 붙는 4-D에서 추가하고, 여기서는 죽은 워커가 남긴 PROCESSING 작업 회수만 담당한다.
@Component
@ConditionalOnProperty(prefix = "naroom.ai.worker", name = "enabled", havingValue = "true")
public class AiJobLeaseReclaimScheduler {

	private static final Logger log = LoggerFactory.getLogger(AiJobLeaseReclaimScheduler.class);

	private final AiJobService aiJobService;
	private final AiWorkerProperties properties;

	public AiJobLeaseReclaimScheduler(AiJobService aiJobService, AiWorkerProperties properties) {
		this.aiJobService = aiJobService;
		this.properties = properties;
	}

	@Scheduled(fixedDelayString = "${naroom.ai.worker.poll-interval}")
	public void reclaimExpiredLeases() {
		int reclaimed = aiJobService.reclaimExpiredLeases(properties.leaseTimeout());
		if (reclaimed > 0) {
			log.info("reclaimed {} expired AI job leases", reclaimed);
		}
	}

}
