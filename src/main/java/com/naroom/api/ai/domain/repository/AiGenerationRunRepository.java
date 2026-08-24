package com.naroom.api.ai.domain.repository;

import com.naroom.api.ai.domain.entity.AiGenerationRun;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface AiGenerationRunRepository extends JpaRepository<AiGenerationRun, UUID> {

	List<AiGenerationRun> findByAiJob_Id(UUID aiJobId);

	Optional<AiGenerationRun> findByIdAndAiJob_Member_Id(UUID id, UUID memberId);

	// 관리자 AI 운영 화면의 "런타임 현황"(§15.1) latency·토큰 집계용. 완료된 호출만 대상으로 한다.
	@Query("""
			select gr.aiJob.featureType as featureType,
			       avg(gr.latencyMs) as avgLatencyMs,
			       avg(gr.inputTokens) as avgInputTokens,
			       avg(gr.outputTokens) as avgOutputTokens
			from AiGenerationRun gr
			where gr.requestedAt >= :since and gr.completedAt is not null
			group by gr.aiJob.featureType
			""")
	List<AiGenerationRunMetricsAggregate> aggregateMetricsSince(@Param("since") Instant since);

}
