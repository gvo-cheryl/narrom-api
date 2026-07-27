package com.naroom.api.record;

import com.naroom.api.account.domain.entity.Member;
import com.naroom.api.account.domain.repository.MemberRepository;
import com.naroom.api.ai.domain.entity.AiFeatureType;
import com.naroom.api.ai.domain.entity.AiJob;
import com.naroom.api.ai.domain.entity.AiJobStatus;
import com.naroom.api.ai.domain.repository.AiJobRepository;
import com.naroom.api.record.domain.entity.Entry;
import com.naroom.api.record.domain.entity.EntryType;
import com.naroom.api.record.domain.repository.EntryRepository;
import com.naroom.api.record.dto.EntryCreateRequest;
import com.naroom.api.record.dto.EntryResponse;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.time.LocalDate;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

// afterCommit 콜백은 트랜잭션이 실제로 커밋돼야 실행되므로, 이 스위트의 다른 테스트처럼
// 클래스 단위 @Transactional로 감싸면(끝나고 롤백) 콜백이 아예 실행되지 않는다. 그래서 여기만
// 트랜잭션 없이 실제로 커밋하고, 끝나면 만든 데이터를 직접 정리한다.
@SpringBootTest
class EntryServiceAiJobTriggerTest {

	@Autowired
	private EntryService entryService;

	@Autowired
	private EntryRepository entryRepository;

	@Autowired
	private MemberRepository memberRepository;

	@Autowired
	private AiJobRepository aiJobRepository;

	private Member createdMember;
	private Entry createdEntry;

	@AfterEach
	void cleanUp() {
		if (createdEntry != null) {
			aiJobRepository.findByMember_IdAndIdempotencyKey(createdMember.getId(), "entry-reflection-" + createdEntry.getId())
					.ifPresent(job -> aiJobRepository.deleteById(job.getId()));
			entryRepository.deleteById(createdEntry.getId());
		}
		if (createdMember != null) {
			memberRepository.deleteById(createdMember.getId());
		}
	}

	@Test
	void createEntry_userCreatableType_createsPendingAiJobAfterCommit() {
		createdMember = memberRepository.save(Member.create("지연"));
		EntryCreateRequest request = new EntryCreateRequest(EntryType.FREE, null, "본문", LocalDate.now(), null, null, null);

		EntryResponse response = entryService.createEntry(createdMember.getId(), request);
		createdEntry = entryRepository.findById(response.id()).orElseThrow();

		Optional<AiJob> job = aiJobRepository.findByMember_IdAndIdempotencyKey(
				createdMember.getId(), "entry-reflection-" + response.id());
		assertTrue(job.isPresent(), "기록 저장 후 AI 작업이 생성돼 있어야 한다");
		assertEquals(AiFeatureType.ENTRY_REFLECTION, job.get().getFeatureType());
		assertEquals(AiJobStatus.PENDING, job.get().getStatus());
		assertEquals(response.id(), job.get().getEntry().getId());
	}

}
