package com.naroom.api.ai.domain;

import com.naroom.api.account.domain.entity.Member;
import com.naroom.api.account.domain.repository.MemberRepository;
import com.naroom.api.ai.domain.entity.AiConversation;
import com.naroom.api.ai.domain.entity.AiConversationSummary;
import com.naroom.api.ai.domain.entity.AiFeatureType;
import com.naroom.api.ai.domain.entity.AiFeedback;
import com.naroom.api.ai.domain.entity.AiFeedbackHelpfulness;
import com.naroom.api.ai.domain.entity.AiFeedbackReport;
import com.naroom.api.ai.domain.entity.AiGenerationRun;
import com.naroom.api.ai.domain.entity.AiJob;
import com.naroom.api.ai.domain.entity.AiJobStatus;
import com.naroom.api.ai.domain.entity.AiMessage;
import com.naroom.api.ai.domain.entity.AiPromptVersion;
import com.naroom.api.ai.domain.entity.AiReflection;
import com.naroom.api.ai.domain.entity.AiSafetyGrade;
import com.naroom.api.ai.domain.entity.MemberAiPreference;
import com.naroom.api.ai.domain.repository.AiConversationRepository;
import com.naroom.api.ai.domain.repository.AiConversationSummaryRepository;
import com.naroom.api.ai.domain.repository.AiFeedbackReportRepository;
import com.naroom.api.ai.domain.repository.AiFeedbackRepository;
import com.naroom.api.ai.domain.repository.AiGenerationRunRepository;
import com.naroom.api.ai.domain.repository.AiJobRepository;
import com.naroom.api.ai.domain.repository.AiMessageRepository;
import com.naroom.api.ai.domain.repository.AiPromptVersionRepository;
import com.naroom.api.ai.domain.repository.AiReflectionRepository;
import com.naroom.api.ai.domain.repository.AiUsageDailyRepository;
import com.naroom.api.ai.domain.repository.MemberAiPreferenceRepository;
import com.naroom.api.record.domain.entity.Entry;
import com.naroom.api.record.domain.entity.EntrySelfReflection;
import com.naroom.api.record.domain.entity.EntryType;
import com.naroom.api.record.domain.repository.EntryRepository;
import com.naroom.api.record.domain.repository.EntrySelfReflectionRepository;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 * Postgres native enum 5종, ai_prompt_versions의 부분 유니크 인덱스(scope별),
 * entry_self_reflections.ai_reflection_id에 새로 연결한 FK처럼 스키마 검증만으로는
 * 확인되지 않는 실제 저장/조회 왕복을 검증한다.
 */
@SpringBootTest
@Transactional
@DirtiesContext
class AiDomainEntityPersistenceTest {

	@Autowired
	private MemberRepository memberRepository;

	@Autowired
	private EntryRepository entryRepository;

	@Autowired
	private EntrySelfReflectionRepository entrySelfReflectionRepository;

	@Autowired
	private AiConversationRepository aiConversationRepository;

	@Autowired
	private AiPromptVersionRepository aiPromptVersionRepository;

	@Autowired
	private AiJobRepository aiJobRepository;

	@Autowired
	private AiGenerationRunRepository aiGenerationRunRepository;

	@Autowired
	private AiReflectionRepository aiReflectionRepository;

	@Autowired
	private AiMessageRepository aiMessageRepository;

	@Autowired
	private AiConversationSummaryRepository aiConversationSummaryRepository;

	@Autowired
	private AiFeedbackReportRepository aiFeedbackReportRepository;

	@Autowired
	private AiFeedbackRepository aiFeedbackRepository;

	@Autowired
	private MemberAiPreferenceRepository memberAiPreferenceRepository;

	@Autowired
	private AiUsageDailyRepository aiUsageDailyRepository;

	@Autowired
	private EntityManager entityManager;

	@Test
	void aiAggregate_roundTripsThroughAllTables() {
		Member member = memberRepository.save(Member.create("지연"));
		Entry entry = entryRepository.save(
				Entry.create(member, EntryType.FREE, "제목", "오늘의 기록", LocalDate.now(), null, null, null));

		AiPromptVersion commonPrompt =
				aiPromptVersionRepository.save(AiPromptVersion.forCommon("common-v1"));
		AiPromptVersion featurePrompt = aiPromptVersionRepository.save(
				AiPromptVersion.forFeature(AiFeatureType.ENTRY_REFLECTION, "entry-reflection-v1", "schema-v1"));

		AiJob entryJob = aiJobRepository.save(
				AiJob.forEntry(member, AiFeatureType.ENTRY_REFLECTION, entry, "idem-" + System.nanoTime()));
		entryJob.markCompleted(Instant.now());

		AiGenerationRun entryRun = aiGenerationRunRepository.save(
				AiGenerationRun.start(entryJob, "gpt-5.6-luna", commonPrompt, featurePrompt, "schema-v1"));
		entryRun.complete(1200, 300, AiSafetyGrade.NORMAL, AiSafetyGrade.NORMAL, 850, Instant.now());

		AiReflection reflection = aiReflectionRepository.save(AiReflection.request(entry, 1));
		reflection.complete(entryRun, "정리 결과", "질문", "{\"emotionCandidates\":[]}", "NORMAL", Instant.now());

		EntrySelfReflection selfReflection = entrySelfReflectionRepository.save(
				EntrySelfReflection.createFromAiReflection(entry, reflection, "내 생각 추가"));

		AiFeedbackReport report =
				aiFeedbackReportRepository.save(AiFeedbackReport.create(member, entryRun, "TOO_GENERIC", "너무 일반적이에요"));

		AiFeedback feedback = aiFeedbackRepository.save(AiFeedback.rate(member, entryRun, AiFeedbackHelpfulness.HELPFUL));
		feedback.confirmLongTermApplication(true);

		MemberAiPreference preference = memberAiPreferenceRepository.save(MemberAiPreference.createDefault(member));
		preference.update("DIRECT", "SHORT", true);

		var usage = aiUsageDailyRepository.save(
				com.naroom.api.ai.domain.entity.AiUsageDaily.start(member, LocalDate.now(), AiFeatureType.ENTRY_REFLECTION, "gpt-5.6-luna"));
		usage.addUsage(1200, 300, false);

		AiConversation conversation =
				aiConversationRepository.save(AiConversation.start(member, AiFeatureType.ENTRY_REFLECTION, entry));

		AiJob conversationJob = aiJobRepository.save(
				AiJob.forConversation(member, AiFeatureType.CONVERSATION_REPLY, conversation, "idem-" + System.nanoTime()));
		AiGenerationRun conversationRun = aiGenerationRunRepository.save(
				AiGenerationRun.start(conversationJob, "gpt-5.6-luna", commonPrompt, featurePrompt, "schema-v1"));

		AiMessage userMessage = aiMessageRepository.save(AiMessage.fromUser(conversation, "그때 왜 그랬을까요?"));
		AiMessage assistantMessage =
				aiMessageRepository.save(AiMessage.fromAssistant(conversation, conversationRun, "그 상황을 조금 더 들어볼게요."));
		conversation.touch(Instant.now());

		AiConversationSummary summary = aiConversationSummaryRepository.save(
				AiConversationSummary.create(conversation, "지금까지의 대화 요약", assistantMessage));

		entityManager.flush();
		entityManager.clear();

		AiReflection reloadedReflection = aiReflectionRepository.findById(reflection.getId()).orElseThrow();
		assertEquals(AiJobStatus.COMPLETED, reloadedReflection.getStatus());
		assertEquals("정리 결과", reloadedReflection.getReflectionText());
		assertEquals(entryRun.getId(), reloadedReflection.getGenerationRun().getId());

		EntrySelfReflection reloadedSelfReflection =
				entrySelfReflectionRepository.findById(selfReflection.getId()).orElseThrow();
		assertNotNull(reloadedSelfReflection.getAiReflection());
		assertEquals(reflection.getId(), reloadedSelfReflection.getAiReflection().getId());

		AiGenerationRun reloadedRun = aiGenerationRunRepository.findById(entryRun.getId()).orElseThrow();
		assertEquals(AiSafetyGrade.NORMAL, reloadedRun.getInputSafetyStatus());
		assertEquals(commonPrompt.getId(), reloadedRun.getCommonPromptVersion().getId());
		assertEquals(featurePrompt.getId(), reloadedRun.getFeaturePromptVersion().getId());

		AiFeedbackReport reloadedReport = aiFeedbackReportRepository.findById(report.getId()).orElseThrow();
		assertEquals("TOO_GENERIC", reloadedReport.getReasonCode());

		AiFeedback reloadedFeedback = aiFeedbackRepository.findById(feedback.getId()).orElseThrow();
		assertEquals(Boolean.TRUE, reloadedFeedback.getApplyLongTerm());

		MemberAiPreference reloadedPreference = memberAiPreferenceRepository.findByMember_Id(member.getId()).orElseThrow();
		assertEquals("DIRECT", reloadedPreference.getTone());

		AiConversationSummary reloadedSummary = aiConversationSummaryRepository.findById(summary.getId()).orElseThrow();
		assertEquals(assistantMessage.getId(), reloadedSummary.getCoversUntilMessage().getId());
		assertEquals(2, aiMessageRepository.findByConversation_IdOrderByCreatedAtAsc(conversation.getId()).size());

		assertEquals(1, aiUsageDailyRepository
				.findByMember_IdAndUsageDateAndFeatureTypeAndModelName(member.getId(), LocalDate.now(), AiFeatureType.ENTRY_REFLECTION, "gpt-5.6-luna")
				.orElseThrow()
				.getCallCount());
	}

}
