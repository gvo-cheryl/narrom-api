package com.naroom.api.ai;

import com.naroom.api.account.domain.entity.Member;
import com.naroom.api.account.domain.repository.MemberRepository;
import com.naroom.api.ai.domain.entity.AiFeatureType;
import com.naroom.api.ai.domain.entity.AiFeedbackHelpfulness;
import com.naroom.api.ai.domain.entity.AiSafetyGrade;
import com.naroom.api.ai.domain.error.AiErrorCode;
import com.naroom.api.ai.dto.AiFeedbackResponse;
import com.naroom.api.ai.dto.AiJobResponse;
import com.naroom.api.ai.outcome.EntryReflectionGenerationContext;
import com.naroom.api.ai.outcome.EntryReflectionOutcomeService;
import com.naroom.api.ai.result.EntryReflectionResult;
import com.naroom.api.global.error.exception.BusinessException;
import com.naroom.api.record.domain.entity.Entry;
import com.naroom.api.record.domain.entity.EntryType;
import com.naroom.api.record.domain.repository.EntryRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest
@Transactional
@DirtiesContext
class AiFeedbackServiceTest {

	@Autowired
	private AiFeedbackService aiFeedbackService;

	@Autowired
	private AiJobService aiJobService;

	@Autowired
	private EntryReflectionOutcomeService outcomeService;

	@Autowired
	private EntryRepository entryRepository;

	@Autowired
	private MemberRepository memberRepository;

	@Test
	void submitFeedback_helpful_createsFeedback() {
		Member member = memberRepository.save(Member.create("지연"));
		UUID generationRunId = createCompletedGenerationRun(member);

		AiFeedbackResponse response =
				aiFeedbackService.submitFeedback(member.getId(), generationRunId, AiFeedbackHelpfulness.HELPFUL, null, null);

		assertEquals(AiFeedbackHelpfulness.HELPFUL, response.helpfulness());
		assertEquals(generationRunId, response.generationRunId());
		assertNull(response.reasonCode());
	}

	@Test
	void submitFeedback_unhelpfulWithReason_savesReason() {
		Member member = memberRepository.save(Member.create("지연"));
		UUID generationRunId = createCompletedGenerationRun(member);

		AiFeedbackResponse response = aiFeedbackService.submitFeedback(
				member.getId(), generationRunId, AiFeedbackHelpfulness.UNHELPFUL, "TOO_GENERIC", "직접입력 사유");

		assertEquals(AiFeedbackHelpfulness.UNHELPFUL, response.helpfulness());
		assertEquals("TOO_GENERIC", response.reasonCode());
		assertEquals("직접입력 사유", response.customReason());
	}

	@Test
	void submitFeedback_calledTwice_updatesExistingRowInstead() {
		Member member = memberRepository.save(Member.create("지연"));
		UUID generationRunId = createCompletedGenerationRun(member);

		AiFeedbackResponse first = aiFeedbackService.submitFeedback(
				member.getId(), generationRunId, AiFeedbackHelpfulness.UNHELPFUL, "TOO_GENERIC", null);
		AiFeedbackResponse second = aiFeedbackService.submitFeedback(
				member.getId(), generationRunId, AiFeedbackHelpfulness.HELPFUL, null, null);

		assertEquals(first.id(), second.id());
		assertEquals(AiFeedbackHelpfulness.HELPFUL, second.helpfulness());
		assertNull(second.reasonCode());
	}

	@Test
	void submitFeedback_generationRunNotOwnedByMember_throwsGenerationRunNotFound() {
		Member owner = memberRepository.save(Member.create("소유자"));
		Member stranger = memberRepository.save(Member.create("타인"));
		UUID generationRunId = createCompletedGenerationRun(owner);

		BusinessException exception = assertThrows(
				BusinessException.class,
				() -> aiFeedbackService.submitFeedback(
						stranger.getId(), generationRunId, AiFeedbackHelpfulness.HELPFUL, null, null));
		assertEquals(AiErrorCode.GENERATION_RUN_NOT_FOUND, exception.errorCode());
	}

	@Test
	void confirmLongTermApplication_existingFeedback_updatesFlag() {
		Member member = memberRepository.save(Member.create("지연"));
		UUID generationRunId = createCompletedGenerationRun(member);
		aiFeedbackService.submitFeedback(member.getId(), generationRunId, AiFeedbackHelpfulness.HELPFUL, null, null);

		AiFeedbackResponse response = aiFeedbackService.confirmLongTermApplication(member.getId(), generationRunId, true);

		assertTrue(response.applyLongTerm());
	}

	@Test
	void confirmLongTermApplication_noFeedbackYet_throwsFeedbackNotFound() {
		Member member = memberRepository.save(Member.create("지연"));
		UUID generationRunId = createCompletedGenerationRun(member);

		BusinessException exception = assertThrows(
				BusinessException.class,
				() -> aiFeedbackService.confirmLongTermApplication(member.getId(), generationRunId, true));
		assertEquals(AiErrorCode.FEEDBACK_NOT_FOUND, exception.errorCode());
	}

	private UUID createCompletedGenerationRun(Member member) {
		Entry entry = entryRepository.save(
				Entry.create(member, EntryType.FREE, null, "본문", LocalDate.now(), null, null, null));
		aiJobService.createForEntry(member.getId(), AiFeatureType.ENTRY_REFLECTION, entry.getId(), "key-" + System.nanoTime());
		AiJobResponse claimed = aiJobService.claimNextBatch(10).get(0);
		EntryReflectionResult parsedResult = new EntryReflectionResult(
				"요약", List.of(), List.of(), List.of(), "질문", List.of(entry.getId()), AiSafetyGrade.NORMAL);
		GenerationResult generationResult = new GenerationResult("{\"summary\":\"요약\"}", 120, 40);
		EntryReflectionGenerationContext context = new EntryReflectionGenerationContext(
				claimed.id(), claimed.startedAt(), entry.getId(), 1, "gpt-5.6-luna",
				"v-" + System.nanoTime() + "-common", "v-feature", "v-schema",
				AiSafetyGrade.NORMAL, AiSafetyGrade.NORMAL,
				generationResult, parsedResult, 850);
		return outcomeService.persist(context).generationRunId();
	}

}
