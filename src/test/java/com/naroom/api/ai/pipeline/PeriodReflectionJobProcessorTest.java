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
import com.naroom.api.lifetime.domain.entity.PeriodReflection;
import com.naroom.api.lifetime.domain.entity.PeriodReflectionEntry;
import com.naroom.api.lifetime.domain.repository.PeriodReflectionEntryRepository;
import com.naroom.api.lifetime.domain.repository.PeriodReflectionRepository;
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
	private AiConversationRepository aiConversationRepository;

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
				periodReflectionRepository,
				periodReflectionEntryRepository,
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
