package com.naroom.api.ai.pipeline;

import com.naroom.api.account.domain.entity.Member;
import com.naroom.api.account.domain.repository.MemberRepository;
import com.naroom.api.ai.AiJobService;
import com.naroom.api.ai.domain.entity.AiConversation;
import com.naroom.api.ai.domain.entity.AiFeatureType;
import com.naroom.api.ai.domain.entity.AiJobStatus;
import com.naroom.api.ai.domain.entity.AiReflection;
import com.naroom.api.ai.domain.entity.AiSafetyGrade;
import com.naroom.api.ai.domain.repository.AiConversationRepository;
import com.naroom.api.ai.domain.repository.AiReflectionRepository;
import com.naroom.api.ai.dto.AiJobResponse;
import com.naroom.api.ai.infra.openai.OpenAiProperties;
import com.naroom.api.ai.outcome.EntryReflectionOutcomeService;
import com.naroom.api.ai.result.EntryReflectionResponseParser;
import com.naroom.api.ai.prompt.PromptAssembler;
import com.naroom.api.record.domain.entity.Entry;
import com.naroom.api.record.domain.entity.EntryType;
import com.naroom.api.record.domain.repository.EntryRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

// EntryReflectionJobProcessor는 이미 개별적으로 테스트된 조각들을 조립만 하므로, 여기서는 OpenAI를 호출하는
// 두 클라이언트만 Fake로 바꾸고 나머지(PromptAssembler/파서/OutcomeService/AiJobService)는 실제 빈을 그대로 쓴다.
@SpringBootTest
@Transactional
@DirtiesContext
class EntryReflectionJobProcessorTest {

	@Autowired
	private PromptAssembler promptAssembler;

	@Autowired
	private EntryReflectionResponseParser responseParser;

	@Autowired
	private EntryReflectionOutcomeService outcomeService;

	@Autowired
	private AiJobService aiJobService;

	@Autowired
	private EntryRepository entryRepository;

	@Autowired
	private MemberRepository memberRepository;

	@Autowired
	private AiConversationRepository aiConversationRepository;

	@Autowired
	private AiReflectionRepository aiReflectionRepository;

	@Test
	void process_normalPath_completesJobAndPersistsReflection() {
		Member member = memberRepository.save(Member.create("지연"));
		Entry entry = entryRepository.save(
				Entry.create(member, EntryType.FREE, null, "오늘은 산책을 하며 마음이 편안해졌다", LocalDate.now(), null, null, null));
		aiJobService.createForEntry(member.getId(), AiFeatureType.ENTRY_REFLECTION, entry.getId(), "key-" + System.nanoTime());
		AiJobResponse claimed = aiJobService.claimNextBatch(10).get(0);
		FakeAiResponseGenerationClient generationClient = new FakeAiResponseGenerationClient(validOutputJson(entry.getId()));
		EntryReflectionJobProcessor processor = newProcessor(new FakeAiModerationClient(AiSafetyGrade.NORMAL), generationClient);

		processor.process(claimed);

		assertEquals(AiJobStatus.COMPLETED, aiJobService.getJob(member.getId(), claimed.id()).status());
		AiReflection reflection = aiReflectionRepository.findByEntry_IdOrderByVersionNoDesc(entry.getId()).get(0);
		assertEquals(AiJobStatus.COMPLETED, reflection.getStatus());
		assertEquals("산책하며 편안해짐", reflection.getReflectionText());
	}

	@Test
	void process_inputBlocked_blocksJobWithoutCallingGeneration() {
		Member member = memberRepository.save(Member.create("지연"));
		Entry entry = entryRepository.save(
				Entry.create(member, EntryType.FREE, null, "본문", LocalDate.now(), null, null, null));
		aiJobService.createForEntry(member.getId(), AiFeatureType.ENTRY_REFLECTION, entry.getId(), "key-" + System.nanoTime());
		AiJobResponse claimed = aiJobService.claimNextBatch(10).get(0);
		FakeAiResponseGenerationClient generationClient = new FakeAiResponseGenerationClient(validOutputJson(entry.getId()));
		EntryReflectionJobProcessor processor = newProcessor(new FakeAiModerationClient(AiSafetyGrade.RESTRICTED), generationClient);

		processor.process(claimed);

		assertEquals(AiJobStatus.BLOCKED, aiJobService.getJob(member.getId(), claimed.id()).status());
		assertEquals(0, generationClient.callCount());
	}

	@Test
	void process_outputCrisis_marksJobSafetySupport() {
		Member member = memberRepository.save(Member.create("지연"));
		Entry entry = entryRepository.save(
				Entry.create(member, EntryType.FREE, null, "본문", LocalDate.now(), null, null, null));
		aiJobService.createForEntry(member.getId(), AiFeatureType.ENTRY_REFLECTION, entry.getId(), "key-" + System.nanoTime());
		AiJobResponse claimed = aiJobService.claimNextBatch(10).get(0);
		EntryReflectionJobProcessor processor = newProcessor(
				new FakeAiModerationClient(AiSafetyGrade.NORMAL, AiSafetyGrade.CRISIS),
				new FakeAiResponseGenerationClient(validOutputJson(entry.getId())));

		processor.process(claimed);

		assertEquals(AiJobStatus.SAFETY_SUPPORT, aiJobService.getJob(member.getId(), claimed.id()).status());
	}

	@Test
	void process_malformedOutput_schedulesRetryWithBackoff() {
		Member member = memberRepository.save(Member.create("지연"));
		Entry entry = entryRepository.save(
				Entry.create(member, EntryType.FREE, null, "본문", LocalDate.now(), null, null, null));
		aiJobService.createForEntry(member.getId(), AiFeatureType.ENTRY_REFLECTION, entry.getId(), "key-" + System.nanoTime());
		AiJobResponse claimed = aiJobService.claimNextBatch(10).get(0);
		EntryReflectionJobProcessor processor = newProcessor(
				new FakeAiModerationClient(AiSafetyGrade.NORMAL),
				new FakeAiResponseGenerationClient("이건 JSON이 아니다"));

		processor.process(claimed);

		AiJobResponse result = aiJobService.getJob(member.getId(), claimed.id());
		assertEquals(AiJobStatus.FAILED, result.status());
		assertEquals(1, result.attemptCount());
		assertTrue(result.nextRetryAt().isAfter(Instant.now()));
	}

	@Test
	void process_unexpectedException_failsPermanently() {
		Member member = memberRepository.save(Member.create("지연"));
		Entry entry = entryRepository.save(
				Entry.create(member, EntryType.FREE, null, "본문", LocalDate.now(), null, null, null));
		aiJobService.createForEntry(member.getId(), AiFeatureType.ENTRY_REFLECTION, entry.getId(), "key-" + System.nanoTime());
		AiJobResponse claimed = aiJobService.claimNextBatch(10).get(0);
		EntryReflectionJobProcessor processor = newProcessor(
				new FakeAiModerationClient(AiSafetyGrade.NORMAL),
				new FakeAiResponseGenerationClient(new IllegalStateException("boom")));

		processor.process(claimed);

		AiJobResponse result = aiJobService.getJob(member.getId(), claimed.id());
		assertEquals(AiJobStatus.FAILED, result.status());
		assertEquals(result.maxAttempts(), result.attemptCount());
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
		EntryReflectionJobProcessor processor = newProcessor(new FakeAiModerationClient(AiSafetyGrade.NORMAL), generationClient);

		processor.process(claimed);

		AiJobResponse result = aiJobService.getJob(member.getId(), claimed.id());
		assertEquals(AiJobStatus.FAILED, result.status());
		assertEquals(result.maxAttempts(), result.attemptCount());
		assertEquals(0, generationClient.callCount());
	}

	private EntryReflectionJobProcessor newProcessor(FakeAiModerationClient moderationClient, FakeAiResponseGenerationClient generationClient) {
		return new EntryReflectionJobProcessor(
				promptAssembler,
				moderationClient,
				generationClient,
				responseParser,
				outcomeService,
				aiJobService,
				new OpenAiProperties("", "fake-model"));
	}

	private String validOutputJson(java.util.UUID entryId) {
		return """
				{
				  "summary": "산책하며 편안해짐",
				  "emotionCandidates": [],
				  "suggestedTagNames": [],
				  "reflectionQuestion": "그 산책에서 무엇이 가장 좋았나요?",
				  "evidenceEntryIds": [%s],
				  "safetyStatus": "NORMAL"
				}
				""".formatted("\"" + entryId + "\"");
	}

}
