package com.naroom.api.experiment;

import com.naroom.api.account.domain.repository.AuthSessionRepository;
import com.naroom.api.auth.security.JwtTokenProvider;
import com.naroom.api.auth.security.MemberAuthentication;
import com.naroom.api.experiment.domain.entity.ExperimentMissionType;
import com.naroom.api.experiment.domain.entity.ExperimentSourceType;
import com.naroom.api.experiment.domain.entity.UserExperimentProgramStatus;
import com.naroom.api.experiment.dto.EstimatedMinutesRange;
import com.naroom.api.experiment.dto.ExperimentProgramMissionResponse;
import com.naroom.api.experiment.dto.ExperimentProgramStartResponse;
import com.naroom.api.experiment.dto.ExperimentProgramSummaryResponse;
import com.naroom.api.experiment.dto.ExperimentRandomProgramResponse;
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
						(short) 1, UUID.randomUUID(), "EMOTION_WORD", "감정 알아차리기",
						ExperimentMissionType.OBSERVATION, (short) 3, UUID.randomUUID())));

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
								(short) 1, UUID.randomUUID(), "EMOTION_WORD", "감정 알아차리기",
								ExperimentMissionType.OBSERVATION, (short) 3, UUID.randomUUID())));

		mockMvc.perform(post("/api/v1/experiments/user-programs/{id}/activate", userExperimentProgramId)
						.with(authentication(memberAuthentication())))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.data.status").value("IN_PROGRESS"));
	}

	private MemberAuthentication memberAuthentication() {
		return new MemberAuthentication(UUID.randomUUID(), UUID.randomUUID());
	}

}
