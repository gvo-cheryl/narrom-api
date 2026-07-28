package com.naroom.api.ai;

import com.naroom.api.account.domain.entity.Member;
import com.naroom.api.account.domain.repository.MemberRepository;
import com.naroom.api.ai.domain.entity.AiFeedback;
import com.naroom.api.ai.domain.entity.AiFeedbackHelpfulness;
import com.naroom.api.ai.domain.entity.AiGenerationRun;
import com.naroom.api.ai.domain.error.AiErrorCode;
import com.naroom.api.ai.domain.repository.AiFeedbackRepository;
import com.naroom.api.ai.domain.repository.AiGenerationRunRepository;
import com.naroom.api.ai.dto.AiFeedbackResponse;
import com.naroom.api.global.error.exception.BusinessException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

// 5-D(§24.5 "만족도와 장기 반영 선택"): §15.4 기준으로 1차 평가(+부정 평가일 때만 2차 사유)와 장기 반영 확인을
// 별도 API로 분리한다 - "현재 응답에 대한 평가는 곧바로 장기 선호도 변경을 의미하지 않는다"는 정책 그대로다.
@Service
@Transactional(readOnly = true)
public class AiFeedbackService {

	private final AiGenerationRunRepository aiGenerationRunRepository;
	private final AiFeedbackRepository aiFeedbackRepository;
	private final MemberRepository memberRepository;

	public AiFeedbackService(
			AiGenerationRunRepository aiGenerationRunRepository,
			AiFeedbackRepository aiFeedbackRepository,
			MemberRepository memberRepository) {
		this.aiGenerationRunRepository = aiGenerationRunRepository;
		this.aiFeedbackRepository = aiFeedbackRepository;
		this.memberRepository = memberRepository;
	}

	// ai_feedback은 (member_id, ai_generation_run_id) UNIQUE라 이미 평가가 있으면 새로 만들지 않고 고친다
	// (사용자가 평가를 제출한 뒤 마음이 바뀌어도 같은 API로 다시 제출할 수 있어야 한다).
	@Transactional
	public AiFeedbackResponse submitFeedback(
			UUID memberId, UUID generationRunId, AiFeedbackHelpfulness helpfulness, String reasonCode, String customReason) {
		AiGenerationRun generationRun = getOwnedGenerationRunOrThrow(memberId, generationRunId);
		AiFeedback feedback = aiFeedbackRepository.findByMember_IdAndGenerationRun_Id(memberId, generationRunId)
				.orElse(null);
		if (feedback == null) {
			Member member = memberRepository.getReferenceById(memberId);
			feedback = AiFeedback.rate(member, generationRun, helpfulness);
			if (helpfulness != AiFeedbackHelpfulness.HELPFUL) {
				feedback.addReason(reasonCode, customReason);
			}
			feedback = aiFeedbackRepository.save(feedback);
		} else {
			feedback.updateRating(helpfulness, reasonCode, customReason);
		}
		return AiFeedbackResponse.from(feedback);
	}

	@Transactional
	public AiFeedbackResponse confirmLongTermApplication(UUID memberId, UUID generationRunId, boolean applyLongTerm) {
		getOwnedGenerationRunOrThrow(memberId, generationRunId);
		AiFeedback feedback = aiFeedbackRepository.findByMember_IdAndGenerationRun_Id(memberId, generationRunId)
				.orElseThrow(() -> new BusinessException(AiErrorCode.FEEDBACK_NOT_FOUND));
		feedback.confirmLongTermApplication(applyLongTerm);
		return AiFeedbackResponse.from(feedback);
	}

	private AiGenerationRun getOwnedGenerationRunOrThrow(UUID memberId, UUID generationRunId) {
		return aiGenerationRunRepository.findByIdAndAiJob_Member_Id(generationRunId, memberId)
				.orElseThrow(() -> new BusinessException(AiErrorCode.GENERATION_RUN_NOT_FOUND));
	}

}
