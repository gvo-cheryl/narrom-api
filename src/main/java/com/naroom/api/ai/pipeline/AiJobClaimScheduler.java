package com.naroom.api.ai.pipeline;

import com.naroom.api.ai.AiJobService;
import com.naroom.api.ai.config.AiWorkerProperties;
import com.naroom.api.ai.domain.entity.AiFeatureType;
import com.naroom.api.ai.dto.AiJobResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.concurrent.Executor;

// naroom.ai.worker.enabled가 true일 때만 활성화된다. 큐에서 배치를 선점(4-B)한 뒤 각 작업 처리를
// bounded 실행기로 넘긴다 - 실제 처리(4-D~4-H)는 feature_type에 맞는 처리기(EntryReflectionJobProcessor/
// PeriodReflectionJobProcessor)가 맡는다. 후속 대화(CONVERSATION_REPLY/CONVERSATION_SUMMARY)는 아직
// 처리기가 없어 영구 실패로 종결한다.
@Component
@ConditionalOnProperty(prefix = "naroom.ai.worker", name = "enabled", havingValue = "true")
public class AiJobClaimScheduler {

	private static final Logger log = LoggerFactory.getLogger(AiJobClaimScheduler.class);

	private final AiJobService aiJobService;
	private final AiWorkerProperties properties;
	private final EntryReflectionJobProcessor entryReflectionProcessor;
	private final PeriodReflectionJobProcessor periodReflectionProcessor;
	private final Executor aiJobExecutor;

	public AiJobClaimScheduler(
			AiJobService aiJobService,
			AiWorkerProperties properties,
			EntryReflectionJobProcessor entryReflectionProcessor,
			PeriodReflectionJobProcessor periodReflectionProcessor,
			Executor aiJobExecutor) {
		this.aiJobService = aiJobService;
		this.properties = properties;
		this.entryReflectionProcessor = entryReflectionProcessor;
		this.periodReflectionProcessor = periodReflectionProcessor;
		this.aiJobExecutor = aiJobExecutor;
	}

	@Scheduled(fixedDelayString = "${naroom.ai.worker.poll-interval}")
	public void claimAndDispatch() {
		List<AiJobResponse> claimed = aiJobService.claimNextBatch(properties.batchSize());
		if (claimed.isEmpty()) {
			return;
		}
		log.info("claimed {} AI jobs for processing", claimed.size());
		for (AiJobResponse job : claimed) {
			aiJobExecutor.execute(() -> dispatch(job));
		}
	}

	private void dispatch(AiJobResponse job) {
		if (job.featureType() == AiFeatureType.ENTRY_REFLECTION) {
			entryReflectionProcessor.process(job);
		} else if (job.featureType() == AiFeatureType.WEEKLY_REFLECTION || job.featureType() == AiFeatureType.THREE_DAY_REFLECTION) {
			periodReflectionProcessor.process(job);
		} else {
			aiJobService.failJobPermanently(job.id(), job.startedAt(), "UNSUPPORTED_FEATURE_TYPE");
		}
	}

}
