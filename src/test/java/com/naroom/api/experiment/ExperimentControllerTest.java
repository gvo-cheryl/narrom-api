package com.naroom.api.experiment;

import com.naroom.api.account.domain.repository.AuthSessionRepository;
import com.naroom.api.ai.domain.entity.AiFeatureType;
import com.naroom.api.ai.domain.entity.AiJobStatus;
import com.naroom.api.auth.security.JwtTokenProvider;
import com.naroom.api.auth.security.MemberAuthentication;
import com.naroom.api.experiment.domain.entity.ExperimentAttemptStatus;
import com.naroom.api.experiment.domain.entity.ExperimentMissionType;
import com.naroom.api.experiment.domain.entity.ExperimentRecommendationSourceType;
import com.naroom.api.experiment.domain.entity.ExperimentRecommendationStatus;
import com.naroom.api.experiment.domain.entity.ExperimentSourceType;
import com.naroom.api.experiment.domain.entity.UserExperimentProgramStatus;
import com.naroom.api.experiment.dto.EstimatedMinutesRange;
import com.naroom.api.experiment.dto.ExperimentActiveProgramResponse;
import com.naroom.api.experiment.dto.ExperimentAiJobSummary;
import com.naroom.api.experiment.dto.ExperimentCourseReviewResponse;
import com.naroom.api.experiment.dto.ExperimentEndEarlyResponse;
import com.naroom.api.experiment.dto.ExperimentMissionCatalogResponse;
import com.naroom.api.experiment.dto.ExperimentMissionRecordResponse;
import com.naroom.api.experiment.dto.ExperimentMissionReplaceResponse;
import com.naroom.api.experiment.dto.ExperimentPastProgramResponse;
import com.naroom.api.experiment.dto.ExperimentProgramMissionResponse;
import com.naroom.api.experiment.dto.ExperimentProgramStartResponse;
import com.naroom.api.experiment.dto.ExperimentProgramSummaryResponse;
import com.naroom.api.experiment.dto.ExperimentRandomProgramResponse;
import com.naroom.api.experiment.dto.ExperimentRecommendationResponse;
import com.naroom.api.experiment.dto.ExperimentTopicResponse;
import com.naroom.api.experiment.dto.ExperimentUserComposedProgramResponse;
import com.naroom.api.experiment.dto.ExperimentUserProgramMissionResponse;
import com.naroom.api.global.config.SecurityConfig;
import com.naroom.api.global.error.response.ProblemDetailFactory;
import com.naroom.api.global.security.ApiAccessDeniedHandler;
import com.naroom.api.global.security.ApiAuthenticationEntryPoint;
import com.naroom.api.global.security.SecurityProblemWriter;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyShort;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(ExperimentController.class)
@Import({
		ProblemDetailFactory.class,
		SecurityConfig.class,
		ApiAuthenticationEntryPoint.class,
		ApiAccessDeniedHandler.class,
		SecurityProblemWriter.class
})
class ExperimentControllerTest {

	@Autowired
	private MockMvc mockMvc;

	@MockitoBean
	private ExperimentTopicService experimentTopicService;

	@MockitoBean
	private ExperimentProgramService experimentProgramService;

	@MockitoBean
	private ExperimentRandomProgramComposer experimentRandomProgramComposer;

	@MockitoBean
	private ExperimentEnrollmentService experimentEnrollmentService;

	@MockitoBean
	private ExperimentProgressService experimentProgressService;

	@MockitoBean
	private ExperimentReviewService experimentReviewService;

	@MockitoBean
	private ExperimentRecommendationService experimentRecommendationService;

	@MockitoBean
	private ExperimentMissionCatalogService experimentMissionCatalogService;

	@MockitoBean
	private JwtTokenProvider jwtTokenProvider;

	@MockitoBean
	private AuthSessionRepository authSessionRepository;

	@Test
	void getTopics_withoutAuthentication_returnsAuthRequired() throws Exception {
		mockMvc.perform(get("/api/v1/experiments/topics"))
				.andExpect(status().isUnauthorized())
				.andExpect(jsonPath("$.code").value("AUTH_REQUIRED"));
	}

	@Test
	void getTopics_authenticated_returnsList() throws Exception {
		when(experimentTopicService.listActive())
				.thenReturn(List.of(new ExperimentTopicResponse(UUID.randomUUID(), "EMOTION", "감정", "설명", 1)));

		mockMvc.perform(get("/api/v1/experiments/topics").with(authentication(memberAuthentication())))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.data[0].code").value("EMOTION"));
	}

	@Test
	void getMissions_authenticated_returnsList() throws Exception {
		when(experimentMissionCatalogService.list(any(), eq("EMOTION"))).thenReturn(List.of(new ExperimentMissionCatalogResponse(
				UUID.randomUUID(), "EMOTION_WORD", "감정 알아차리기", "지금 느껴지는 감정에 가볍게 이름을 붙여봅니다.", "EMOTION",
				ExperimentMissionType.OBSERVATION, (short) 3)));

		mockMvc.perform(get("/api/v1/experiments/missions?topicCode=EMOTION").with(authentication(memberAuthentication())))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.data[0].code").value("EMOTION_WORD"))
				.andExpect(jsonPath("$.data[0].topicCode").value("EMOTION"));
	}

	@Test
	void getPrograms_authenticated_returnsList() throws Exception {
		when(experimentProgramService.list(any(), any(), any(), any())).thenReturn(List.of(
				new ExperimentProgramSummaryResponse(
						UUID.randomUUID(), "NOW_MIND_3", "지금의 마음 알아보기", (short) 3, "EMOTION",
						"설명", new EstimatedMinutesRange((short) 3, (short) 3), 3)));

		mockMvc.perform(get("/api/v1/experiments/programs?durationDays=3").with(authentication(memberAuthentication())))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.data[0].code").value("NOW_MIND_3"));
	}

	@Test
	void getRandomProgram_authenticated_returnsComposedMissions() throws Exception {
		when(experimentRandomProgramComposer.compose(any(), anyShort())).thenReturn(new ExperimentRandomProgramResponse(
				(short) 3,
				List.of(new ExperimentProgramMissionResponse(
						(short) 1, UUID.randomUUID(), "EMOTION_WORD", "감정 알아차리기", ExperimentMissionType.OBSERVATION, (short) 3))));

		mockMvc.perform(get("/api/v1/experiments/programs/random?days=3").with(authentication(memberAuthentication())))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.data.durationDays").value(3))
				.andExpect(jsonPath("$.data.missions[0].missionCode").value("EMOTION_WORD"));
	}

	@Test
	void startProgram_authenticated_returnsInProgressCourse() throws Exception {
		UUID programId = UUID.randomUUID();
		UUID userExperimentProgramId = UUID.randomUUID();
		when(experimentEnrollmentService.startFromTemplate(any(), eq(programId), any())).thenReturn(new ExperimentProgramStartResponse(
				userExperimentProgramId, UserExperimentProgramStatus.IN_PROGRESS, "지금의 마음 알아보기", (short) 3, (short) 1, 0, 0,
				new ExperimentUserProgramMissionResponse(
						(short) 1, UUID.randomUUID(), "EMOTION_WORD", "감정 알아차리기", "지금의 감정을 한두 단어로 적어보세요.",
						ExperimentMissionType.OBSERVATION, (short) 3, java.util.List.of("그 감정은 언제부터 느껴졌나요?"), UUID.randomUUID())));

		mockMvc.perform(post("/api/v1/experiments/programs/{programId}/start", programId)
						.with(authentication(memberAuthentication()))
						.contentType(MediaType.APPLICATION_JSON)
						.content("{}"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.data.status").value("IN_PROGRESS"))
				.andExpect(jsonPath("$.data.userExperimentProgramId").value(userExperimentProgramId.toString()));
	}

	@Test
	void createUserComposedProgram_authenticated_returnsReadyCourse() throws Exception {
		UUID userExperimentProgramId = UUID.randomUUID();
		when(experimentEnrollmentService.createUserComposed(any(), any())).thenReturn(new ExperimentUserComposedProgramResponse(
				userExperimentProgramId, null, ExperimentSourceType.USER_COMPOSED, UserExperimentProgramStatus.READY, (short) 3, 3));

		mockMvc.perform(post("/api/v1/experiments/programs/user-composed")
						.with(authentication(memberAuthentication()))
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{
								  "title": "퇴근 뒤 마음 정리해보기",
								  "durationDays": 3,
								  "missions": [
								    { "dayNumber": 1, "title": "제목", "instruction": "안내문", "missionType": "OBSERVATION", "estimatedMinutes": 3 }
								  ]
								}
								"""))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.data.status").value("READY"))
				.andExpect(jsonPath("$.data.configurationSource").value("USER_COMPOSED"));
	}

	@Test
	void activateSavedProgram_authenticated_returnsInProgressCourse() throws Exception {
		UUID userExperimentProgramId = UUID.randomUUID();
		when(experimentEnrollmentService.activateSaved(any(), eq(userExperimentProgramId), anyBoolean()))
				.thenReturn(new ExperimentProgramStartResponse(
						userExperimentProgramId, UserExperimentProgramStatus.IN_PROGRESS, "지금의 마음 알아보기", (short) 3, (short) 1, 0, 0,
						new ExperimentUserProgramMissionResponse(
								(short) 1, UUID.randomUUID(), "EMOTION_WORD", "감정 알아차리기", "지금의 감정을 한두 단어로 적어보세요.",
								ExperimentMissionType.OBSERVATION, (short) 3, java.util.List.of("그 감정은 언제부터 느껴졌나요?"), UUID.randomUUID())));

		mockMvc.perform(post("/api/v1/experiments/user-programs/{id}/activate", userExperimentProgramId)
						.with(authentication(memberAuthentication())))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.data.status").value("IN_PROGRESS"));
	}

	@Test
	void getActiveProgram_authenticated_returnsTodayMission() throws Exception {
		UUID userExperimentProgramId = UUID.randomUUID();
		when(experimentProgressService.getActive(any())).thenReturn(java.util.Optional.of(new ExperimentActiveProgramResponse(
				userExperimentProgramId, UserExperimentProgramStatus.IN_PROGRESS, "지금의 마음 알아보기", (short) 3, (short) 1, 0, 0,
				new ExperimentUserProgramMissionResponse(
						(short) 1, UUID.randomUUID(), "EMOTION_WORD", "감정 알아차리기", "지금의 감정을 한두 단어로 적어보세요.",
						ExperimentMissionType.OBSERVATION, (short) 3, java.util.List.of("그 감정은 언제부터 느껴졌나요?"), UUID.randomUUID()))));

		mockMvc.perform(get("/api/v1/experiments/user-programs/active").with(authentication(memberAuthentication())))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.data.userExperimentProgramId").value(userExperimentProgramId.toString()));
	}

	@Test
	void getActiveProgram_noActiveCourse_returnsNullData() throws Exception {
		when(experimentProgressService.getActive(any())).thenReturn(java.util.Optional.empty());

		mockMvc.perform(get("/api/v1/experiments/user-programs/active").with(authentication(memberAuthentication())))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.data").doesNotExist());
	}

	@Test
	void recordMission_authenticated_returnsRecordResult() throws Exception {
		UUID userExperimentProgramId = UUID.randomUUID();
		UUID userProgramMissionId = UUID.randomUUID();
		when(experimentProgressService.recordMission(any(), eq(userExperimentProgramId), eq(userProgramMissionId), any()))
				.thenReturn(new ExperimentMissionRecordResponse(
						ExperimentAttemptStatus.DONE, true, UserExperimentProgramStatus.IN_PROGRESS, (short) 2, false, 1, 0));

		mockMvc.perform(post(
						"/api/v1/experiments/user-programs/{userExperimentProgramId}/missions/{userProgramMissionId}/record",
						userExperimentProgramId, userProgramMissionId)
						.with(authentication(memberAuthentication()))
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{ "attemptStatus": "DONE", "recordDate": "2026-07-31" }
								"""))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.data.missionConsumed").value(true))
				.andExpect(jsonPath("$.data.currentDay").value(2));
	}

	@Test
	void replaceMission_authenticated_returnsReplaceResult() throws Exception {
		UUID userExperimentProgramId = UUID.randomUUID();
		UUID userProgramMissionId = UUID.randomUUID();
		UUID replacementMissionId = UUID.randomUUID();
		when(experimentProgressService.replaceMission(any(), eq(userExperimentProgramId), eq(userProgramMissionId), any()))
				.thenReturn(new ExperimentMissionReplaceResponse(
						userProgramMissionId, (short) 2, UUID.randomUUID(), replacementMissionId, 1));

		mockMvc.perform(post(
						"/api/v1/experiments/user-programs/{userExperimentProgramId}/missions/{userProgramMissionId}/replace",
						userExperimentProgramId, userProgramMissionId)
						.with(authentication(memberAuthentication()))
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{ "replacementMissionId": "%s", "reasonCode": "WANT_LIGHTER" }
								""".formatted(replacementMissionId)))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.data.replacementCount").value(1))
				.andExpect(jsonPath("$.data.missionId").value(replacementMissionId.toString()));
	}

	@Test
	void completeReview_authenticated_returnsCompletedWithAiJob() throws Exception {
		UUID userExperimentProgramId = UUID.randomUUID();
		when(experimentReviewService.completeReview(any(), eq(userExperimentProgramId), any()))
				.thenReturn(new ExperimentCourseReviewResponse(
						UserExperimentProgramStatus.COMPLETED, true,
						new ExperimentAiJobSummary(AiFeatureType.THREE_DAY_REFLECTION, AiJobStatus.PENDING, null)));

		mockMvc.perform(post("/api/v1/experiments/user-programs/{id}/review", userExperimentProgramId)
						.with(authentication(memberAuthentication()))
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{ "discovery": "발견한 것", "requestAiReflection": true }
								"""))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.data.status").value("COMPLETED"))
				.andExpect(jsonPath("$.data.aiJob.featureType").value("THREE_DAY_REFLECTION"));
	}

	@Test
	void endEarly_authenticated_returnsEndedEarly() throws Exception {
		UUID userExperimentProgramId = UUID.randomUUID();
		when(experimentReviewService.endEarly(any(), eq(userExperimentProgramId)))
				.thenReturn(new ExperimentEndEarlyResponse(userExperimentProgramId, UserExperimentProgramStatus.ENDED_EARLY));

		mockMvc.perform(post("/api/v1/experiments/user-programs/{id}/end-early", userExperimentProgramId)
						.with(authentication(memberAuthentication())))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.data.status").value("ENDED_EARLY"));
	}

	@Test
	void getPastPrograms_authenticated_returnsList() throws Exception {
		when(experimentReviewService.listPast(any())).thenReturn(List.of(new ExperimentPastProgramResponse(
				UUID.randomUUID(), UserExperimentProgramStatus.COMPLETED, "지금의 마음 알아보기", (short) 3, (short) 3,
				null, null, null)));

		mockMvc.perform(get("/api/v1/experiments/user-programs/past").with(authentication(memberAuthentication())))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.data[0].status").value("COMPLETED"));
	}

	@Test
	void getRecommendations_authenticated_returnsList() throws Exception {
		when(experimentRecommendationService.listActive(any())).thenReturn(List.of(new ExperimentRecommendationResponse(
				UUID.randomUUID(),
				new ExperimentProgramSummaryResponse(
						UUID.randomUUID(), "NOW_MIND_3", "지금의 마음 알아보기", (short) 3, "EMOTION",
						"설명", new EstimatedMinutesRange((short) 3, (short) 3), 3),
				ExperimentRecommendationSourceType.RULE, "작은 실험이 처음이라면 이 코스로 가볍게 시작해볼 수 있어요.",
				ExperimentRecommendationStatus.SHOWN, Instant.now())));

		mockMvc.perform(get("/api/v1/experiments/recommendations").with(authentication(memberAuthentication())))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.data[0].program.code").value("NOW_MIND_3"))
				.andExpect(jsonPath("$.data[0].status").value("SHOWN"));
	}

	@Test
	void viewRecommendation_authenticated_returnsViewed() throws Exception {
		UUID recommendationId = UUID.randomUUID();
		when(experimentRecommendationService.markViewed(any(), eq(recommendationId))).thenReturn(new ExperimentRecommendationResponse(
				recommendationId,
				new ExperimentProgramSummaryResponse(
						UUID.randomUUID(), "NOW_MIND_3", "지금의 마음 알아보기", (short) 3, "EMOTION",
						"설명", new EstimatedMinutesRange((short) 3, (short) 3), 3),
				ExperimentRecommendationSourceType.RULE, "작은 실험이 처음이라면 이 코스로 가볍게 시작해볼 수 있어요.",
				ExperimentRecommendationStatus.VIEWED, Instant.now()));

		mockMvc.perform(post("/api/v1/experiments/recommendations/{id}/view", recommendationId)
						.with(authentication(memberAuthentication())))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.data.status").value("VIEWED"));
	}

	@Test
	void dismissRecommendation_authenticated_returnsDismissed() throws Exception {
		UUID recommendationId = UUID.randomUUID();
		when(experimentRecommendationService.dismiss(any(), eq(recommendationId))).thenReturn(new ExperimentRecommendationResponse(
				recommendationId,
				new ExperimentProgramSummaryResponse(
						UUID.randomUUID(), "NOW_MIND_3", "지금의 마음 알아보기", (short) 3, "EMOTION",
						"설명", new EstimatedMinutesRange((short) 3, (short) 3), 3),
				ExperimentRecommendationSourceType.RULE, "작은 실험이 처음이라면 이 코스로 가볍게 시작해볼 수 있어요.",
				ExperimentRecommendationStatus.DISMISSED, Instant.now()));

		mockMvc.perform(post("/api/v1/experiments/recommendations/{id}/dismiss", recommendationId)
						.with(authentication(memberAuthentication())))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.data.status").value("DISMISSED"));
	}

	private MemberAuthentication memberAuthentication() {
		return new MemberAuthentication(UUID.randomUUID(), UUID.randomUUID());
	}

}
