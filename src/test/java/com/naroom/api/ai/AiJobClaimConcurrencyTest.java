package com.naroom.api.ai;

import com.naroom.api.account.domain.entity.Member;
import com.naroom.api.account.domain.repository.MemberRepository;
import com.naroom.api.ai.domain.entity.AiFeatureType;
import com.naroom.api.ai.domain.repository.AiJobRepository;
import com.naroom.api.ai.dto.AiJobResponse;
import com.naroom.api.record.domain.entity.Entry;
import com.naroom.api.record.domain.entity.EntryType;
import com.naroom.api.record.domain.repository.EntryRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

// claimNextBatch가 실제로 여러 워커(스레드=별도 트랜잭션)에서 동시에 호출돼도 같은 작업을 중복 선점하지 않는지 검증한다.
// 클래스 단위 @Transactional로 롤백하면 워커 스레드들이 서로 다른 커넥션/트랜잭션에서 미커밋 데이터를 볼 수 없으므로
// 이 테스트만 트랜잭션 없이 실행하고, 끝나면 만든 데이터를 직접 정리한다.
@SpringBootTest
class AiJobClaimConcurrencyTest {

	@Autowired
	private AiJobService aiJobService;

	@Autowired
	private AiJobRepository aiJobRepository;

	@Autowired
	private EntryRepository entryRepository;

	@Autowired
	private MemberRepository memberRepository;

	private final List<UUID> createdJobIds = new ArrayList<>();
	private Entry createdEntry;
	private Member createdMember;

	@AfterEach
	void cleanUp() {
		aiJobRepository.deleteAllByIdInBatch(createdJobIds);
		if (createdEntry != null) {
			entryRepository.deleteById(createdEntry.getId());
		}
		if (createdMember != null) {
			memberRepository.deleteById(createdMember.getId());
		}
	}

	@Test
	void claimNextBatch_calledConcurrentlyByMultipleWorkers_neverDoubleClaimsAJob() throws Exception {
		createdMember = memberRepository.save(Member.create("동시성"));
		createdEntry = entryRepository.save(
				Entry.create(createdMember, EntryType.FREE, null, "본문", LocalDate.now(), null, null, null));

		int totalJobs = 30;
		for (int i = 0; i < totalJobs; i++) {
			AiJobResponse job = aiJobService.createForEntry(
					createdMember.getId(), AiFeatureType.ENTRY_REFLECTION, createdEntry.getId(), "concurrency-key-" + i);
			createdJobIds.add(job.id());
		}

		int workerCount = 6;
		int batchSize = 10;
		ExecutorService executor = Executors.newFixedThreadPool(workerCount);
		CountDownLatch startLatch = new CountDownLatch(1);
		List<Future<List<AiJobResponse>>> futures = new ArrayList<>();
		try {
			for (int i = 0; i < workerCount; i++) {
				futures.add(executor.submit(() -> {
					startLatch.await();
					return aiJobService.claimNextBatch(batchSize);
				}));
			}
			startLatch.countDown();

			List<UUID> claimedIds = new ArrayList<>();
			for (Future<List<AiJobResponse>> future : futures) {
				for (AiJobResponse job : future.get()) {
					claimedIds.add(job.id());
				}
			}

			Set<UUID> distinctClaimedIds = new HashSet<>(claimedIds);
			assertEquals(claimedIds.size(), distinctClaimedIds.size(), "같은 작업이 두 워커에게 동시에 선점되면 안 된다");
			assertEquals(totalJobs, claimedIds.size(), "워커들의 배치 용량 합이 총 작업 수 이상이므로 전부 선점되어야 한다");
			assertTrue(createdJobIds.containsAll(claimedIds));
		} finally {
			executor.shutdown();
		}
	}

}
