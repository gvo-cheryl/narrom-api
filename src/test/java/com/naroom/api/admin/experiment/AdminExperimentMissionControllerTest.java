package com.naroom.api.admin.experiment;

import com.naroom.api.admin.auth.AdminSessionService;
import com.naroom.api.admin.auth.IssuedAdminSession;
import com.naroom.api.admin.domain.entity.AdminRole;
import com.naroom.api.admin.domain.entity.AdminUser;
import com.naroom.api.admin.domain.repository.AdminUserRepository;
import com.naroom.api.experiment.domain.entity.ExperimentEmotionalLoad;
import com.naroom.api.experiment.domain.entity.ExperimentMission;
import com.naroom.api.experiment.domain.entity.ExperimentMissionType;
import com.naroom.api.experiment.domain.entity.ExperimentTopic;
import com.naroom.api.experiment.domain.repository.ExperimentMissionRepository;
import com.naroom.api.experiment.domain.repository.ExperimentTopicRepository;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.util.Set;
import java.util.UUID;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
@DirtiesContext
class AdminExperimentMissionControllerTest {

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private AdminUserRepository adminUserRepository;

	@Autowired
	private AdminSessionService adminSessionService;

	@Autowired
	private ExperimentTopicRepository experimentTopicRepository;

	@Autowired
	private ExperimentMissionRepository experimentMissionRepository;

	@Test
	void create_thenUpdate_bumpsContentVersionAndKeepsCode() throws Exception {
		ExperimentTopic topic = experimentTopicRepository.save(
				ExperimentTopic.create("emotion-" + System.nanoTime(), "감정", null, 1, true));
		Cookie cookie = sessionCookie(Set.of(AdminRole.CONTENT_EDITOR));

		String createBody = """
				{
				  "topicId": "%s",
				  "title": "오늘의 감정 관찰",
				  "description": "설명",
				  "instruction": "지시문",
				  "missionType": "OBSERVATION",
				  "responseType": "TEXT",
				  "estimatedMinutes": 5,
				  "emotionalLoad": "LOW",
				  "reflectionQuestions": "[]",
				  "examples": "[]",
				  "responseSchema": "{}",
				  "safetyNote": null,
				  "active": true
				}
				""".formatted(topic.getId());

		String createResponse = mockMvc.perform(post("/api/v1/admin/experiments/missions")
						.cookie(cookie)
						.with(csrf())
						.contentType(MediaType.APPLICATION_JSON)
						.content(createBody))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.data.contentVersion").value(1))
				.andReturn()
				.getResponse()
				.getContentAsString();

		String missionId = com.jayway.jsonpath.JsonPath.read(createResponse, "$.data.id");
		String code = com.jayway.jsonpath.JsonPath.read(createResponse, "$.data.code");

		String updateBody = """
				{
				  "topicId": "%s",
				  "title": "오늘의 감정 관찰(수정)",
				  "description": "수정된 설명",
				  "instruction": "수정된 지시문",
				  "missionType": "QUESTION",
				  "responseType": "TEXT",
				  "estimatedMinutes": 7,
				  "emotionalLoad": "MEDIUM",
				  "reflectionQuestions": "[]",
				  "examples": "[]",
				  "responseSchema": "{}",
				  "active": true
				}
				""".formatted(topic.getId());

		mockMvc.perform(put("/api/v1/admin/experiments/missions/" + missionId)
						.cookie(cookie)
						.with(csrf())
						.contentType(MediaType.APPLICATION_JSON)
						.content(updateBody))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.data.code").value(code))
				.andExpect(jsonPath("$.data.contentVersion").value(2))
				.andExpect(jsonPath("$.data.title").value("오늘의 감정 관찰(수정)"))
				.andExpect(jsonPath("$.data.missionType").value("QUESTION"));
	}

	@Test
	void create_unknownTopic_returnsTopicNotFound() throws Exception {
		Cookie cookie = sessionCookie(Set.of(AdminRole.SUPER_ADMIN));

		String body = """
				{
				  "topicId": "%s",
				  "title": "제목",
				  "description": "설명",
				  "instruction": "지시문",
				  "missionType": "ACTION",
				  "responseType": "TEXT",
				  "estimatedMinutes": 5,
				  "emotionalLoad": "LOW",
				  "reflectionQuestions": "[]",
				  "examples": "[]",
				  "responseSchema": "{}",
				  "active": true
				}
				""".formatted(UUID.randomUUID());

		mockMvc.perform(post("/api/v1/admin/experiments/missions")
						.cookie(cookie)
						.with(csrf())
						.contentType(MediaType.APPLICATION_JSON)
						.content(body))
				.andExpect(status().isNotFound())
				.andExpect(jsonPath("$.code").value("EXPERIMENT_TOPIC_NOT_FOUND"));
	}

	@Test
	void list_withQ_returnsOnlyMatchingMissions() throws Exception {
		ExperimentTopic topic = experimentTopicRepository.save(
				ExperimentTopic.create("topic-" + System.nanoTime(), "주제", null, 1, true));
		String uniqueMarker = "찾아줘" + System.nanoTime();
		experimentMissionRepository.save(ExperimentMission.create(
				"mission-match-" + System.nanoTime(), topic, uniqueMarker + " 미션", "설명", "지시문",
				ExperimentMissionType.OBSERVATION, "TEXT", (short) 5, ExperimentEmotionalLoad.LOW,
				"[]", "[]", "{}", null, true));
		experimentMissionRepository.save(ExperimentMission.create(
				"mission-nomatch-" + System.nanoTime(), topic, "관련 없는 미션", "설명", "지시문",
				ExperimentMissionType.OBSERVATION, "TEXT", (short) 5, ExperimentEmotionalLoad.LOW,
				"[]", "[]", "{}", null, true));
		Cookie cookie = sessionCookie(Set.of(AdminRole.SUPER_ADMIN));

		mockMvc.perform(get("/api/v1/admin/experiments/missions").cookie(cookie).param("q", uniqueMarker))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.data.length()").value(1))
				.andExpect(jsonPath("$.data[0].title").value(uniqueMarker + " 미션"));
	}

	@Test
	void list_withDisallowedSortField_returnsValidationError() throws Exception {
		Cookie cookie = sessionCookie(Set.of(AdminRole.SUPER_ADMIN));

		mockMvc.perform(get("/api/v1/admin/experiments/missions").cookie(cookie).param("sort", "description,asc"))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.code").value("COMMON_VALIDATION_FAILED"));
	}

	private Cookie sessionCookie(Set<AdminRole> roles) {
		AdminUser adminUser = adminUserRepository.save(
				AdminUser.bootstrap("google-sub-" + System.nanoTime(), "admin@naroom.io", "관리자", roles));
		IssuedAdminSession issued = adminSessionService.issue(adminUser, "ip-hash", "ua-summary");
		return new Cookie("naroom_admin_session", issued.rawToken());
	}

}
