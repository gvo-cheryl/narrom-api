package com.naroom.api.ai;

import com.naroom.api.account.domain.entity.Member;
import com.naroom.api.account.domain.repository.MemberRepository;
import com.naroom.api.ai.domain.entity.AiFeatureType;
import com.naroom.api.ai.domain.entity.AiSafetyGrade;
import com.naroom.api.ai.domain.error.AiErrorCode;
import com.naroom.api.ai.dto.AiFeedbackReportResponse;
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
import static org.junit.jupiter.api.Assertions.assertThrows;

@SpringBootTest
@Transactional
@DirtiesContext
class AiFeedbackReportServiceTest {

	@Autowired
	private AiFeedbackReportService aiFeedbackReportService;

	@Autowired
	private AiJobService aiJobService;

	@Autowired
	private EntryReflectionOutcomeService outcomeService;

	@Autowired
	private EntryRepository entryRepository;

	@Autowired
	private MemberRepository memberRepository;

	@Test
	void report_validRequest_createsReport() {
		Member member = memberRepository.save(Member.create("지연"));
		UUID generationRunId = createCompletedGenerationRun(member);

		AiFeedbackReportResponse response =
				aiFeedbackReportService.report(member.getId(), generationRunId, "INAPPROPRIATE", "이상한 응답이에요");

		assertEquals("INAPPROPRIATE", response.reasonCode());
		assertEquals("이상한 응답이에요", response.comment());
		assertEquals(generationRunId, response.generationRunId());
	}

	@Test
	void report_calledTwice_returnsSameReportInsteadOfDuplicating() {
		Member member = memberRepository.save(Member.create("지연"));
		UUID generationRunId = createCompletedGenerationRun(member);

		AiFeedbackReportResponse first = aiFeedbackReportService.report(member.getId(), generationRunId, "INAPPROPRIATE", "첫 신고");
		AiFeedbackReportResponse second = aiFeedbackReportService.report(member.getId(), generationRunId, "DANGEROUS", "두번째 신고");

		assertEquals(first.id(), second.id());
		assertEquals("INAPPROPRIATE", second.reasonCode());
		assertEquals("첫 신고", second.comment());
	}

	@Test
	void report_generationRunNotOwnedByMember_throwsGenerationRunNotFound() {
		Member owner = memberRepository.save(Member.create("소유자"));
		Member stranger = memberRepository.save(Member.create("타인"));
		UUID generationRunId = createCompletedGenerationRun(owner);

		BusinessException exception = assertThrows(
				BusinessException.class,
				() -> aiFeedbackReportService.report(stranger.getId(), generationRunId, "INAPPROPRIATE", null));
		assertEquals(AiErrorCode.GENERATION_RUN_NOT_FOUND, exception.errorCode());
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
