package com.naroom.api.ai;

import com.naroom.api.ai.dto.AiFeedbackLongTermRequest;
import com.naroom.api.ai.dto.AiFeedbackResponse;
import com.naroom.api.ai.dto.AiFeedbackSubmitRequest;
import com.naroom.api.auth.security.MemberAuthentication;
import com.naroom.api.global.response.ApiResponse;
import jakarta.validation.Valid;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/ai")
public class AiController {

	private final AiFeedbackService aiFeedbackService;

	public AiController(AiFeedbackService aiFeedbackService) {
		this.aiFeedbackService = aiFeedbackService;
	}

	@PutMapping("/generation-runs/{generationRunId}/feedback")
	public ApiResponse<AiFeedbackResponse> submitFeedback(
			@PathVariable UUID generationRunId, @Valid @RequestBody AiFeedbackSubmitRequest request) {
		return ApiResponse.of(aiFeedbackService.submitFeedback(
				currentMemberId(), generationRunId, request.helpfulness(), request.reasonCode(), request.customReason()));
	}

	@PatchMapping("/generation-runs/{generationRunId}/feedback/long-term")
	public ApiResponse<AiFeedbackResponse> confirmLongTermApplication(
			@PathVariable UUID generationRunId, @Valid @RequestBody AiFeedbackLongTermRequest request) {
		return ApiResponse.of(aiFeedbackService.confirmLongTermApplication(
				currentMemberId(), generationRunId, request.applyLongTerm()));
	}

	// JwtAuthenticationFilter가 SecurityContextHolder에 직접 채워 넣는 방식이라 여기서도 직접 꺼낸다
	// (AccountController/ContentController/RecordController와 동일한 이유).
	private UUID currentMemberId() {
		MemberAuthentication authentication =
				(MemberAuthentication) SecurityContextHolder.getContext().getAuthentication();
		return authentication.getMemberId();
	}

}
