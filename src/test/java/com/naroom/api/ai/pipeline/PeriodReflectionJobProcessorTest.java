package com.naroom.api.ai.pipeline;

import com.naroom.api.account.domain.entity.Member;
import com.naroom.api.account.domain.repository.MemberRepository;
import com.naroom.api.ai.AiJobService;
import com.naroom.api.ai.domain.entity.AiConversation;
import com.naroom.api.ai.domain.entity.AiFeatureType;
import com.naroom.api.ai.domain.entity.AiJobStatus;
import com.naroom.api.ai.domain.entity.AiSafetyGrade;
import com.naroom.api.ai.domain.repository.AiConversationRepository;
import com.naroom.api.ai.dto.AiJobResponse;
import com.naroom.api.ai.infra.openai.OpenAiProperties;
import com.naroom.api.ai.outcome.PeriodReflectionOutcomeService;
import com.naroom.api.ai.prompt.PromptAssembler;
import com.naroom.api.ai.result.PeriodReflectionResponseParser;
import com.naroom.api.lifetime.PeriodReflectionService;
import com.naroom.api.lifetime.domain.entity.PeriodReflection;
import com.naroom.api.lifetime.domain.entity.PeriodReflectionEntry;
import com.naroom.api.lifetime.domain.repository.PeriodReflectionEntryRepository;
import com.naroom.api.lifetime.domain.repository.PeriodReflectionRepository;
import com.naroom.api.record.domain.entity.Entry;
import com.naroom.api.record.domain.entity.EntryType;
import com.naroom.api.record.domain.repository.EntryRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

// EntryReflectionJobProcessorTest와 같은 방식: OpenAI를 호출하는 두 클라이언트만 Fake로 바꾸고
// 나머지(PromptAssembler/파서/OutcomeService/AiJobService)는 실제 빈을 그대로 쓴다.
@SpringBootTest
@Transactional
@DirtiesContext
class PeriodReflectionJobProcessorTest {

	@Autowired
	private PromptAssembler promptAssembler;

	@Autowired
	private PeriodReflectionResponseParser responseParser;

	@Autowired
	private PeriodReflectionOutcomeService outcomeService;

	@Autowired
	private AiJobService aiJobService;

	@Autowired
	private EntryRepository entryRepository;

	@Autowired
	private MemberRepository memberRepository;

	@Autowired
	private PeriodReflectionRepository periodReflectionRepository;

	@Autowired
	private PeriodReflectionEntryRepository periodReflectionEntryRepository;

	@Autowired
	private PeriodReflectionService periodReflectionService;

	@Autowired
	private AiConversationRepository aiConversationRepository;

	@PersistenceContext
	private EntityManager entityManager;

	@Test
	void process_normalPath_completesJobAndPersistsReflection() {
		Member member = memberRepository.save(Member.create("지연"));
		Entry evidenceEntry = entryRepository.save(publishedEntry(member, "이번 주 있었던 일"));
		Entry envelope = entryRepository.save(envelopeEntry(member));
		PeriodReflection reflection = periodReflectionRepository.save(PeriodReflection.request(
				member, envelope, AiFeatureType.WEEKLY_REFLECTION, LocalDate.now().minusDays(6), LocalDate.now()));
		periodReflectionEntryRepository.save(PeriodReflectionEntry.link(reflection, evidenceEntry, null));
		aiJobService.createForEntry(member.getId(), AiFeatureType.WEEKLY_REFLECTION, envelope.getId(), "key-" + System.nanoTime());
		AiJobResponse claimed = aiJobService.claimNextBatch(10).get(0);
		FakeAiResponseGenerationClient generationClient =
				new FakeAiResponseGenerationClient(validOutputJson(evidenceEntry.getId()));
		PeriodReflectionJobProcessor processor = newProcessor(new FakeAiModerationClient(AiSafetyGrade.NORMAL), generationClient);

		processor.process(claimed);

		assertEquals(AiJobStatus.COMPLETED, aiJobService.getJob(member.getId(), claimed.id()).status());
		PeriodReflection reloaded = periodReflectionRepository.findById(reflection.getId()).orElseThrow();
		assertEquals(AiJobStatus.COMPLETED, reloaded.getStatus());
		assertEquals("이번 주 요약", reloaded.getSummaryText());
	}

	// 실제 운영에서는 AiJobClaimScheduler가 별도 스레드(Executor)에서 트랜잭션 없이 process()를 호출한다.
	// 클래스 레벨 @Transactional은 이 테스트에서만 예외적으로 꺼서(NOT_SUPPORTED) 그 조건을 재현한다 -
	// 감싸는 트랜잭션이 있으면 지연 로딩 관련 버그(LazyInitializationException)가 가려져 통과해버린다.
	@Test
	@Transactional(propagation = Propagation.NOT_SUPPORTED)
	void process_withoutAmbientTransaction_doesNotThrowLazyInitializationException() {
		Member member = memberRepository.save(Member.create("지연"));
		Entry evidenceEntry = entryRepository.save(publishedEntry(member, "이번 주 있었던 일"));
		Entry envelope = entryRepository.save(envelopeEntry(member));
		PeriodReflection reflection = periodReflectionRepository.save(PeriodReflection.request(
				member, envelope, AiFeatureType.WEEKLY_REFLECTION, LocalDate.now().minusDays(6), LocalDate.now()));
		periodReflectionEntryRepository.save(PeriodReflectionEntry.link(reflection, evidenceEntry, null));
		aiJobService.createForEntry(member.getId(), AiFeatureType.WEEKLY_REFLECTION, envelope.getId(), "key-" + System.nanoTime());
		AiJobResponse claimed = aiJobService.claimNextBatch(10).get(0);
		FakeAiResponseGenerationClient generationClient =
				new FakeAiResponseGenerationClient(validOutputJson(evidenceEntry.getId()));
		PeriodReflectionJobProcessor processor = newProcessor(new FakeAiModerationClient(AiSafetyGrade.NORMAL), generationClient);

		processor.process(claimed);

		assertEquals(AiJobStatus.COMPLETED, aiJobService.getJob(member.getId(), claimed.id()).status());
		PeriodReflection reloaded = periodReflectionRepository.findById(reflection.getId()).orElseThrow();
		assertEquals(AiJobStatus.COMPLETED, reloaded.getStatus());
	}

	@Test
	void process_inputBlocked_blocksJobWithoutCallingGeneration() {
		Member member = memberRepository.save(Member.create("지연"));
		Entry evidenceEntry = entryRepository.save(publishedEntry(member, "본문"));
		Entry envelope = entryRepository.save(envelopeEntry(member));
		PeriodReflection reflection = periodReflectionRepository.save(PeriodReflection.request(
				member, envelope, AiFeatureType.WEEKLY_REFLECTION, LocalDate.now().minusDays(6), LocalDate.now()));
		periodReflectionEntryRepository.save(PeriodReflectionEntry.link(reflection, evidenceEntry, null));
		aiJobService.createForEntry(member.getId(), AiFeatureType.WEEKLY_REFLECTION, envelope.getId(), "key-" + System.nanoTime());
		AiJobResponse claimed = aiJobService.claimNextBatch(10).get(0);
		FakeAiResponseGenerationClient generationClient =
				new FakeAiResponseGenerationClient(validOutputJson(evidenceEntry.getId()));
		PeriodReflectionJobProcessor processor = newProcessor(new FakeAiModerationClient(AiSafetyGrade.RESTRICTED), generationClient);

		processor.process(claimed);

		assertEquals(AiJobStatus.BLOCKED, aiJobService.getJob(member.getId(), claimed.id()).status());
		assertEquals(0, generationClient.callCount());
	}

	// SummaryOriginalityValidator("따라 읽기" 금지 재검증)가 IllegalArgumentException을 던지면 스키마 파싱
	// 실패와 같은 재시도 경로를 타야 한다.
	@Test
	void process_verbatimSummary_schedulesRetryWithBackoff() {
		Member member = memberRepository.save(Member.create("지연"));
		String body = "오늘은 회사에서 팀장님과 의견이 부딪혀서 마음이 많이 답답하고 속상했다";
		Entry evidenceEntry = entryRepository.save(publishedEntry(member, body));
		Entry envelope = entryRepository.save(envelopeEntry(member));
		PeriodReflection reflection = periodReflectionRepository.save(PeriodReflection.request(
				member, envelope, AiFeatureType.WEEKLY_REFLECTION, LocalDate.now().minusDays(6), LocalDate.now()));
		periodReflectionEntryRepository.save(PeriodReflectionEntry.link(reflection, evidenceEntry, null));
		aiJobService.createForEntry(member.getId(), AiFeatureType.WEEKLY_REFLECTION, envelope.getId(), "key-" + System.nanoTime());
		AiJobResponse claimed = aiJobService.claimNextBatch(10).get(0);
		PeriodReflectionJobProcessor processor = newProcessor(
				new FakeAiModerationClient(AiSafetyGrade.NORMAL),
				new FakeAiResponseGenerationClient(verbatimOutputJson(evidenceEntry.getId(), body)));

		processor.process(claimed);

		AiJobResponse result = aiJobService.getJob(member.getId(), claimed.id());
		assertEquals(AiJobStatus.FAILED, result.status());
		assertEquals(1, result.attemptCount());
		assertTrue(result.nextRetryAt().isAfter(Instant.now()));
	}

	// 안전 분류가 "따라 읽기" 재검증보다 항상 먼저 확정돼야 한다 - 순서가 바뀌면 위기 상황의 응답이
	// CRISIS 라우팅에 닿기도 전에 재검증 예외로 재시도되어 버려질 수 있다(리뷰에서 발견된 버그).
	@Test
	void process_crisisOutputWithVerbatimSummary_stillRoutesToSafetySupport() {
		Member member = memberRepository.save(Member.create("지연"));
		String body = "오늘은 회사에서 팀장님과 의견이 부딪혀서 마음이 많이 답답하고 속상했다";
		Entry evidenceEntry = entryRepository.save(publishedEntry(member, body));
		Entry envelope = entryRepository.save(envelopeEntry(member));
		PeriodReflection reflection = periodReflectionRepository.save(PeriodReflection.request(
				member, envelope, AiFeatureType.WEEKLY_REFLECTION, LocalDate.now().minusDays(6), LocalDate.now()));
		periodReflectionEntryRepository.save(PeriodReflectionEntry.link(reflection, evidenceEntry, null));
		aiJobService.createForEntry(member.getId(), AiFeatureType.WEEKLY_REFLECTION, envelope.getId(), "key-" + System.nanoTime());
		AiJobResponse claimed = aiJobService.claimNextBatch(10).get(0);
		PeriodReflectionJobProcessor processor = newProcessor(
				new FakeAiModerationClient(AiSafetyGrade.NORMAL, AiSafetyGrade.CRISIS),
				new FakeAiResponseGenerationClient(verbatimOutputJson(evidenceEntry.getId(), body)));

		processor.process(claimed);

		assertEquals(AiJobStatus.SAFETY_SUPPORT, aiJobService.getJob(member.getId(), claimed.id()).status());
	}

	@Test
	void process_malformedOutput_schedulesRetryWithBackoff() {
		Member member = memberRepository.save(Member.create("지연"));
		Entry evidenceEntry = entryRepository.save(publishedEntry(member, "본문"));
		Entry envelope = entryRepository.save(envelopeEntry(member));
		PeriodReflection reflection = periodReflectionRepository.save(PeriodReflection.request(
				member, envelope, AiFeatureType.WEEKLY_REFLECTION, LocalDate.now().minusDays(6), LocalDate.now()));
		periodReflectionEntryRepository.save(PeriodReflectionEntry.link(reflection, evidenceEntry, null));
		aiJobService.createForEntry(member.getId(), AiFeatureType.WEEKLY_REFLECTION, envelope.getId(), "key-" + System.nanoTime());
		AiJobResponse claimed = aiJobService.claimNextBatch(10).get(0);
		PeriodReflectionJobProcessor processor = newProcessor(
				new FakeAiModerationClient(AiSafetyGrade.NORMAL),
				new FakeAiResponseGenerationClient("이건 JSON이 아니다"));

		processor.process(claimed);

		AiJobResponse result = aiJobService.getJob(member.getId(), claimed.id());
		assertEquals(AiJobStatus.FAILED, result.status());
		assertEquals(1, result.attemptCount());
		assertTrue(result.nextRetryAt().isAfter(Instant.now()));
	}

	// 재시도 가능한 예외(IllegalArgumentException 등)라도 이번이 마지막 허용 시도라면(attempt_count가
	// max_attempts에 도달) ai_jobs는 조용히 재시도 불가 상태가 된다 - 이때도 period_reflections.status를
	// FAILED로 맞추지 않으면 영원히 PENDING으로 남아 generate()가 재시도를 만들지 못한다(실사용 중 발견된 버그).
	@Test
	void process_retryableFailureExhaustsAttempts_marksPeriodReflectionFailedToo() {
		Member member = memberRepository.save(Member.create("지연"));
		Entry evidenceEntry = entryRepository.save(publishedEntry(member, "본문"));
		Entry envelope = entryRepository.save(envelopeEntry(member));
		PeriodReflection reflection = periodReflectionRepository.save(PeriodReflection.request(
				member, envelope, AiFeatureType.WEEKLY_REFLECTION, LocalDate.now().minusDays(6), LocalDate.now()));
		periodReflectionEntryRepository.save(PeriodReflectionEntry.link(reflection, evidenceEntry, null));
		aiJobService.createForEntry(member.getId(), AiFeatureType.WEEKLY_REFLECTION, envelope.getId(), "key-" + System.nanoTime());
		AiJobResponse claimed = aiJobService.claimNextBatch(10).get(0);
		// 이미 2번(= maxAttempts-1) 실패해 이번이 마지막 시도인 상태를 재현한다.
		entityManager.createNativeQuery("update ai_jobs set attempt_count = 2 where id = :id")
				.setParameter("id", claimed.id())
				.executeUpdate();
		entityManager.clear();
		AiJobResponse lastAttemptJob = aiJobService.getJob(member.getId(), claimed.id());
		PeriodReflectionJobProcessor processor = newProcessor(
				new FakeAiModerationClient(AiSafetyGrade.NORMAL),
				new FakeAiResponseGenerationClient("이건 JSON이 아니다"));

		processor.process(lastAttemptJob);

		AiJobResponse result = aiJobService.getJob(member.getId(), claimed.id());
		assertEquals(AiJobStatus.FAILED, result.status());
		assertEquals(result.maxAttempts(), result.attemptCount());
		PeriodReflection reloaded = periodReflectionRepository.findById(reflection.getId()).orElseThrow();
		assertEquals(AiJobStatus.FAILED, reloaded.getStatus());
	}

	// ai_jobs만 FAILED가 되고 period_reflections.status는 그대로 남는 회귀를 막는다 - 그렇게 되면 프론트가
	// 이 회고를 폴링할 때마다 영원히 PENDING으로 보이고, generate() 재시도도 만들어지지 않는다.
	@Test
	void process_nonRetryableFailure_marksPeriodReflectionFailedToo() {
		Member member = memberRepository.save(Member.create("지연"));
		Entry evidenceEntry = entryRepository.save(publishedEntry(member, "본문"));
		Entry envelope = entryRepository.save(envelopeEntry(member));
		PeriodReflection reflection = periodReflectionRepository.save(PeriodReflection.request(
				member, envelope, AiFeatureType.WEEKLY_REFLECTION, LocalDate.now().minusDays(6), LocalDate.now()));
		periodReflectionEntryRepository.save(PeriodReflectionEntry.link(reflection, evidenceEntry, null));
		aiJobService.createForEntry(member.getId(), AiFeatureType.WEEKLY_REFLECTION, envelope.getId(), "key-" + System.nanoTime());
		AiJobResponse claimed = aiJobService.claimNextBatch(10).get(0);
		PeriodReflectionJobProcessor processor = newProcessor(
				new FakeAiModerationClient(AiSafetyGrade.NORMAL),
				new FakeAiResponseGenerationClient(new IllegalStateException("boom")));

		processor.process(claimed);

		AiJobResponse result = aiJobService.getJob(member.getId(), claimed.id());
		assertEquals(AiJobStatus.FAILED, result.status());
		assertEquals(result.maxAttempts(), result.attemptCount());
		PeriodReflection reloaded = periodReflectionRepository.findById(reflection.getId()).orElseThrow();
		assertEquals(AiJobStatus.FAILED, reloaded.getStatus());
	}

	@Test
	void process_unsupportedFeatureType_failsPermanentlyWithoutCallingAnything() {
		Member member = memberRepository.save(Member.create("지연"));
		AiConversation conversation = aiConversationRepository.save(
				AiConversation.start(member, AiFeatureType.CONVERSATION_REPLY, null));
		aiJobService.createForConversation(
				member.getId(), AiFeatureType.CONVERSATION_REPLY, conversation.getId(), "key-" + System.nanoTime());
		AiJobResponse claimed = aiJobService.claimNextBatch(10).get(0);
		FakeAiResponseGenerationClient generationClient = new FakeAiResponseGenerationClient("{}");
		PeriodReflectionJobProcessor processor = newProcessor(new FakeAiModerationClient(AiSafetyGrade.NORMAL), generationClient);

		processor.process(claimed);

		AiJobResponse result = aiJobService.getJob(member.getId(), claimed.id());
		assertEquals(AiJobStatus.FAILED, result.status());
		assertEquals(result.maxAttempts(), result.attemptCount());
		assertEquals(0, generationClient.callCount());
	}

	private PeriodReflectionJobProcessor newProcessor(
			FakeAiModerationClient moderationClient, FakeAiResponseGenerationClient generationClient) {
		return new PeriodReflectionJobProcessor(
				periodReflectionService,
				promptAssembler,
				moderationClient,
				generationClient,
				responseParser,
				outcomeService,
				aiJobService,
				new OpenAiProperties("", "fake-model"));
	}

	private Entry publishedEntry(Member member, String body) {
		Entry entry = Entry.create(member, EntryType.FREE, null, body, LocalDate.now(), null, null, null);
		entry.publish();
		return entry;
	}

	private Entry envelopeEntry(Member member) {
		Entry entry = Entry.create(member, EntryType.WEEKLY_REFLECTION, null, null, LocalDate.now(), null, null, null);
		entry.publish();
		return entry;
	}

	private String verbatimOutputJson(java.util.UUID evidenceEntryId, String body) {
		return """
				{
				  "summary": "%s",
				  "repeatedEmotionsAndSituations": [],
				  "difficultMoments": [],
				  "gratefulMoments": [],
				  "triedResponses": [],
				  "helpfulConditions": [],
				  "reflectionQuestion": "이번 주 무엇이 가장 힘들었나요?",
				  "evidenceEntryIds": ["%s"],
				  "safetyStatus": "NORMAL"
				}
				""".formatted(body, evidenceEntryId);
	}

	private String validOutputJson(java.util.UUID evidenceEntryId) {
		return """
				{
				  "summary": "이번 주 요약",
				  "repeatedEmotionsAndSituations": [],
				  "difficultMoments": [],
				  "gratefulMoments": [],
				  "triedResponses": [],
				  "helpfulConditions": [],
				  "reflectionQuestion": "이번 주 무엇이 가장 힘들었나요?",
				  "evidenceEntryIds": ["%s"],
				  "safetyStatus": "NORMAL"
				}
				""".formatted(evidenceEntryId);
	}

}
