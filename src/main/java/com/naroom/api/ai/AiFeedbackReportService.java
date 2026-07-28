package com.naroom.api.ai;

import com.naroom.api.account.domain.entity.Member;
import com.naroom.api.account.domain.repository.MemberRepository;
import com.naroom.api.ai.domain.entity.AiFeedbackReport;
import com.naroom.api.ai.domain.entity.AiGenerationRun;
import com.naroom.api.ai.domain.error.AiErrorCode;
import com.naroom.api.ai.domain.repository.AiFeedbackReportRepository;
import com.naroom.api.ai.domain.repository.AiGenerationRunRepository;
import com.naroom.api.ai.dto.AiFeedbackReportResponse;
import com.naroom.api.global.error.exception.BusinessException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

// 5-E(§24.5 "AI 응답 신고"): ai_feedback_reports는 (member_id, ai_generation_run_id) UNIQUE고 모든 컬럼이
// updatable=false다 - 신고는 한 번 남기면 고치는 게 아니라 그대로 유지되는 기록이라는 뜻이다. 그래서 같은
// 대상을 다시 신고해도 새로 만들거나 값을 덮어쓰지 않고 이미 있는 신고를 그대로 반환한다(멱등).
@Service
@Transactional(readOnly = true)
public class AiFeedbackReportService {

	private final AiGenerationRunRepository aiGenerationRunRepository;
	private final AiFeedbackReportRepository aiFeedbackReportRepository;
	private final MemberRepository memberRepository;

	public AiFeedbackReportService(
			AiGenerationRunRepository aiGenerationRunRepository,
			AiFeedbackReportRepository aiFeedbackReportRepository,
			MemberRepository memberRepository) {
		this.aiGenerationRunRepository = aiGenerationRunRepository;
		this.aiFeedbackReportRepository = aiFeedbackReportRepository;
		this.memberRepository = memberRepository;
	}

	@Transactional
	public AiFeedbackReportResponse report(UUID memberId, UUID generationRunId, String reasonCode, String comment) {
		AiGenerationRun generationRun = aiGenerationRunRepository.findByIdAndAiJob_Member_Id(generationRunId, memberId)
				.orElseThrow(() -> new BusinessException(AiErrorCode.GENERATION_RUN_NOT_FOUND));
		AiFeedbackReport report = aiFeedbackReportRepository.findByMember_IdAndGenerationRun_Id(memberId, generationRunId)
				.orElseGet(() -> {
					Member member = memberRepository.getReferenceById(memberId);
					return aiFeedbackReportRepository.save(AiFeedbackReport.create(member, generationRun, reasonCode, comment));
				});
		return AiFeedbackReportResponse.from(report);
	}

}
