package com.naroom.api.ai;

import com.naroom.api.account.domain.repository.AuthSessionRepository;
import com.naroom.api.ai.domain.entity.AiFeedbackHelpfulness;
import com.naroom.api.ai.domain.error.AiErrorCode;
import com.naroom.api.ai.dto.AiFeedbackReportResponse;
import com.naroom.api.ai.dto.AiFeedbackResponse;
import com.naroom.api.auth.security.JwtTokenProvider;
import com.naroom.api.auth.security.MemberAuthentication;
import com.naroom.api.global.config.SecurityConfig;
import com.naroom.api.global.error.exception.BusinessException;
import com.naroom.api.global.error.response.ProblemDetailFactory;
import com.naroom.api.global.security.ApiAccessDeniedHandler;
import com.naroom.api.global.security.ApiAuthenticationEntryPoint;
import com.naroom.api.global.security.SecurityProblemWriter;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(AiController.class)
@Import({
		ProblemDetailFactory.class,
		SecurityConfig.class,
		ApiAuthenticationEntryPoint.class,
		ApiAccessDeniedHandler.class,
		SecurityProblemWriter.class
})
class AiControllerTest {

	@Autowired
	private MockMvc mockMvc;

	@MockitoBean
	private AiFeedbackService aiFeedbackService;

	@MockitoBean
	private AiFeedbackReportService aiFeedbackReportService;

	@MockitoBean
	private JwtTokenProvider jwtTokenProvider;

	@MockitoBean
	private AuthSessionRepository authSessionRepository;

	@Test
	void submitFeedback_withoutAuthentication_returnsAuthRequired() throws Exception {
		mockMvc.perform(put("/api/v1/ai/generation-runs/" + UUID.randomUUID() + "/feedback")
						.contentType("application/json")
						.content("""
								{ "helpfulness": "HELPFUL" }
								"""))
				.andExpect(status().isUnauthorized())
				.andExpect(jsonPath("$.code").value("AUTH_REQUIRED"));
	}

	@Test
	void submitFeedback_validRequest_returnsFeedback() throws Exception {
		UUID generationRunId = UUID.randomUUID();
		when(aiFeedbackService.submitFeedback(any(), any(), eq(AiFeedbackHelpfulness.HELPFUL), any(), any()))
				.thenReturn(sampleFeedback(generationRunId, AiFeedbackHelpfulness.HELPFUL, null, null));

		mockMvc.perform(put("/api/v1/ai/generation-runs/" + generationRunId + "/feedback")
						.with(authentication(memberAuthentication()))
						.contentType("application/json")
						.content("""
								{ "helpfulness": "HELPFUL" }
								"""))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.data.helpfulness").value("HELPFUL"))
				.andExpect(jsonPath("$.data.generationRunId").value(generationRunId.toString()));
	}

	@Test
	void submitFeedback_missingHelpfulness_returnsValidationFailed() throws Exception {
		mockMvc.perform(put("/api/v1/ai/generation-runs/" + UUID.randomUUID() + "/feedback")
						.with(authentication(memberAuthentication()))
						.contentType("application/json")
						.content("""
								{}
								"""))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.code").value("COMMON_VALIDATION_FAILED"));
	}

	@Test
	void submitFeedback_generationRunNotFound_returnsProblemDetail() throws Exception {
		when(aiFeedbackService.submitFeedback(any(), any(), any(), any(), any()))
				.thenThrow(new BusinessException(AiErrorCode.GENERATION_RUN_NOT_FOUND));

		mockMvc.perform(put("/api/v1/ai/generation-runs/" + UUID.randomUUID() + "/feedback")
						.with(authentication(memberAuthentication()))
						.contentType("application/json")
						.content("""
								{ "helpfulness": "HELPFUL" }
								"""))
				.andExpect(status().isNotFound())
				.andExpect(jsonPath("$.code").value("AI_GENERATION_RUN_NOT_FOUND"));
	}

	@Test
	void confirmLongTermApplication_validRequest_returnsFeedback() throws Exception {
		UUID generationRunId = UUID.randomUUID();
		when(aiFeedbackService.confirmLongTermApplication(any(), any(), eq(true)))
				.thenReturn(sampleFeedback(generationRunId, AiFeedbackHelpfulness.HELPFUL, null, true));

		mockMvc.perform(patch("/api/v1/ai/generation-runs/" + generationRunId + "/feedback/long-term")
						.with(authentication(memberAuthentication()))
						.contentType("application/json")
						.content("""
								{ "applyLongTerm": true }
								"""))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.data.applyLongTerm").value(true));
	}

	@Test
	void confirmLongTermApplication_noFeedbackYet_returnsProblemDetail() throws Exception {
		when(aiFeedbackService.confirmLongTermApplication(any(), any(), anyBoolean()))
				.thenThrow(new BusinessException(AiErrorCode.FEEDBACK_NOT_FOUND));

		mockMvc.perform(patch("/api/v1/ai/generation-runs/" + UUID.randomUUID() + "/feedback/long-term")
						.with(authentication(memberAuthentication()))
						.contentType("application/json")
						.content("""
								{ "applyLongTerm": true }
								"""))
				.andExpect(status().isNotFound())
				.andExpect(jsonPath("$.code").value("AI_FEEDBACK_NOT_FOUND"));
	}

	@Test
	void reportGenerationRun_validRequest_returnsReport() throws Exception {
		UUID generationRunId = UUID.randomUUID();
		when(aiFeedbackReportService.report(any(), any(), eq("INAPPROPRIATE"), any()))
				.thenReturn(new AiFeedbackReportResponse(
						UUID.randomUUID(), generationRunId, "INAPPROPRIATE", "이상해요", Instant.now()));

		mockMvc.perform(post("/api/v1/ai/generation-runs/" + generationRunId + "/reports")
						.with(authentication(memberAuthentication()))
						.contentType("application/json")
						.content("""
								{ "reasonCode": "INAPPROPRIATE", "comment": "이상해요" }
								"""))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.data.reasonCode").value("INAPPROPRIATE"))
				.andExpect(jsonPath("$.data.generationRunId").value(generationRunId.toString()));
	}

	@Test
	void reportGenerationRun_missingReasonCode_returnsValidationFailed() throws Exception {
		mockMvc.perform(post("/api/v1/ai/generation-runs/" + UUID.randomUUID() + "/reports")
						.with(authentication(memberAuthentication()))
						.contentType("application/json")
						.content("""
								{}
								"""))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.code").value("COMMON_VALIDATION_FAILED"));
	}

	@Test
	void reportGenerationRun_generationRunNotFound_returnsProblemDetail() throws Exception {
		when(aiFeedbackReportService.report(any(), any(), any(), any()))
				.thenThrow(new BusinessException(AiErrorCode.GENERATION_RUN_NOT_FOUND));

		mockMvc.perform(post("/api/v1/ai/generation-runs/" + UUID.randomUUID() + "/reports")
						.with(authentication(memberAuthentication()))
						.contentType("application/json")
						.content("""
								{ "reasonCode": "INAPPROPRIATE" }
								"""))
				.andExpect(status().isNotFound())
				.andExpect(jsonPath("$.code").value("AI_GENERATION_RUN_NOT_FOUND"));
	}

	private AiFeedbackResponse sampleFeedback(
			UUID generationRunId, AiFeedbackHelpfulness helpfulness, String reasonCode, Boolean applyLongTerm) {
		return new AiFeedbackResponse(
				UUID.randomUUID(), generationRunId, helpfulness, reasonCode, null, applyLongTerm, Instant.now(), Instant.now());
	}

	private MemberAuthentication memberAuthentication() {
		return new MemberAuthentication(UUID.randomUUID(), UUID.randomUUID());
	}

}
