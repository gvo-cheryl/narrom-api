package com.naroom.api.ai.domain.repository;

import com.naroom.api.ai.domain.entity.AiJob;
import com.naroom.api.ai.domain.entity.AiJobStatus;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface AiJobRepository extends JpaRepository<AiJob, UUID> {

	Optional<AiJob> findByMember_IdAndIdempotencyKey(UUID memberId, String idempotencyKey);

	// 완료 처리 직전 단일 행을 잠가 lease(startedAt) 재검증에 쓴다. SKIP LOCKED 특수 타임아웃 값에 의존하지 않는
	// 일반적인 PESSIMISTIC_WRITE라서, 같은 작업을 동시에 완료 처리하려는 요청끼리는 안전하게 직렬화된다.
	@Lock(LockModeType.PESSIMISTIC_WRITE)
	@Query("select j from AiJob j where j.id = :id")
	Optional<AiJob> findByIdForUpdate(@Param("id") UUID id);

	// 큐 선점: 재시도 가능한(attempt_count < max_attempts) PENDING/FAILED 작업 중 지금 재시도 가능한 것만
	// FOR UPDATE SKIP LOCKED로 골라온다. 다른 워커가 이미 잠근 행은 건너뛰므로 폴링 인스턴스를 늘려도 중복 선점이 없다.
	// now() 대신 clock_timestamp()를 쓴다: now()는 트랜잭션 시작 시각에 고정되어, 트랜잭션이 오래 열려 있으면
	// 그 사이 갱신된 next_retry_at을 지나간 값으로 잘못 비교하게 된다.
	@Query(value = """
			SELECT id FROM ai_jobs
			WHERE status IN ('PENDING', 'FAILED')
			  AND attempt_count < max_attempts
			  AND (next_retry_at IS NULL OR next_retry_at <= clock_timestamp())
			ORDER BY created_at
			LIMIT :batchSize
			FOR UPDATE SKIP LOCKED
			""", nativeQuery = true)
	List<UUID> selectClaimableIds(@Param("batchSize") int batchSize);

	@Modifying(clearAutomatically = true)
	@Query("update AiJob j set j.status = :status, j.startedAt = :startedAt where j.id in :ids")
	int markClaimed(@Param("ids") List<UUID> ids, @Param("status") AiJobStatus status, @Param("startedAt") Instant startedAt);

	// 만료된 임대(오래 멈춘 PROCESSING) 회수. 회수도 재시도 횟수에 포함하므로, max_attempts를 넘기면
	// selectClaimableIds의 attempt_count < max_attempts 조건에 걸려 더 이상 선점되지 않는다.
	@Modifying(clearAutomatically = true)
	@Query("""
			update AiJob j
			set j.status = :failedStatus, j.errorCode = :errorCode,
			    j.attemptCount = j.attemptCount + 1, j.nextRetryAt = :nextRetryAt
			where j.status = :processingStatus and j.startedAt < :expiredBefore
			""")
	int reclaimExpiredLeases(
			@Param("processingStatus") AiJobStatus processingStatus,
			@Param("failedStatus") AiJobStatus failedStatus,
			@Param("errorCode") String errorCode,
			@Param("nextRetryAt") Instant nextRetryAt,
			@Param("expiredBefore") Instant expiredBefore);

}
