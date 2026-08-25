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

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
@DirtiesContext
class AdminExperimentProgramControllerTest {

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
	void fullLifecycle_createPublishReviseAndArchive() throws Exception {
		Cookie cookie = sessionCookie(Set.of(AdminRole.CONTENT_EDITOR));
		ExperimentTopic topic = newTopic();
		ExperimentMission m1 = newMission(topic, true);
		ExperimentMission m2 = newMission(topic, true);
		ExperimentMission m3 = newMission(topic, true);
		String code = "program-" + System.nanoTime();

		String createResponse = mockMvc.perform(post("/api/v1/admin/experiments/programs")
						.cookie(cookie).with(csrf()).contentType(MediaType.APPLICATION_JSON)
						.content(programRequestJson(code, topic, m1, m2, m3)))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.data.code").value(code))
				.andExpect(jsonPath("$.data.contentVersion").value(1))
				.andExpect(jsonPath("$.data.status").value("DRAFT"))
				.andExpect(jsonPath("$.data.days.length()").value(3))
				.andReturn().getResponse().getContentAsString();
		String draftId = com.jayway.jsonpath.JsonPath.read(createResponse, "$.data.id");

		mockMvc.perform(post("/api/v1/admin/experiments/programs/" + draftId + "/publish").cookie(cookie).with(csrf()))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.data.status").value("PUBLISHED"));

		String revisionResponse = mockMvc.perform(post("/api/v1/admin/experiments/programs/" + draftId + "/versions")
						.cookie(cookie).with(csrf()))
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.data.code").value(code))
				.andExpect(jsonPath("$.data.contentVersion").value(2))
				.andExpect(jsonPath("$.data.status").value("DRAFT"))
				.andExpect(jsonPath("$.data.supersedesProgramId").value(draftId))
				.andExpect(jsonPath("$.data.days.length()").value(3))
				.andReturn().getResponse().getContentAsString();
		String revisionId = com.jayway.jsonpath.JsonPath.read(revisionResponse, "$.data.id");

		// 새 버전을 발행하면 기존 발행본은 자동으로 ARCHIVED로 내려간다.
		mockMvc.perform(post("/api/v1/admin/experiments/programs/" + revisionId + "/publish").cookie(cookie).with(csrf()))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.data.status").value("PUBLISHED"));

		mockMvc.perform(post("/api/v1/admin/experiments/programs/" + draftId + "/archive").cookie(cookie).with(csrf()))
				.andExpect(status().isConflict())
				.andExpect(jsonPath("$.code").value("EXPERIMENT_PROGRAM_NOT_PUBLISHED"));
	}

	@Test
	void create_duplicateCode_returnsConflict() throws Exception {
		Cookie cookie = sessionCookie(Set.of(AdminRole.SUPER_ADMIN));
		ExperimentTopic topic = newTopic();
		ExperimentMission m1 = newMission(topic, true);
		ExperimentMission m2 = newMission(topic, true);
		ExperimentMission m3 = newMission(topic, true);
		String code = "dup-program-" + System.nanoTime();
		String body = programRequestJson(code, topic, m1, m2, m3);

		mockMvc.perform(post("/api/v1/admin/experiments/programs")
						.cookie(cookie).with(csrf()).contentType(MediaType.APPLICATION_JSON).content(body))
				.andExpect(status().isOk());

		mockMvc.perform(post("/api/v1/admin/experiments/programs")
						.cookie(cookie).with(csrf()).contentType(MediaType.APPLICATION_JSON).content(body))
				.andExpect(status().isConflict())
				.andExpect(jsonPath("$.code").value("EXPERIMENT_PROGRAM_CODE_DUPLICATE"));
	}

	@Test
	void create_missingDay_returnsBadRequest() throws Exception {
		Cookie cookie = sessionCookie(Set.of(AdminRole.SUPER_ADMIN));
		ExperimentTopic topic = newTopic();
		ExperimentMission m1 = newMission(topic, true);
		ExperimentMission m2 = newMission(topic, true);
		String code = "gap-program-" + System.nanoTime();

		String body = """
				{"code":"%s","primaryTopicId":"%s","title":"제목","description":"설명","durationDays":3,
				"sourceType":"TEMPLATE","estimatedMinutesMin":5,"estimatedMinutesMax":10,"featured":false,
				"beginner":true,"displayOrder":0,
				"days":[{"dayNumber":1,"missionId":"%s","replaceable":false,"replacementGroup":null},
				{"dayNumber":2,"missionId":"%s","replaceable":false,"replacementGroup":null}]}
				""".formatted(code, topic.getId(), m1.getId(), m2.getId());

		mockMvc.perform(post("/api/v1/admin/experiments/programs")
						.cookie(cookie).with(csrf()).contentType(MediaType.APPLICATION_JSON).content(body))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.code").value("EXPERIMENT_INVALID_DAY_NUMBER"));
	}

	@Test
	void publish_withInactiveMission_returnsBadRequest() throws Exception {
		Cookie cookie = sessionCookie(Set.of(AdminRole.SUPER_ADMIN));
		ExperimentTopic topic = newTopic();
		ExperimentMission m1 = newMission(topic, true);
		ExperimentMission m2 = newMission(topic, true);
		ExperimentMission m3 = newMission(topic, false);
		String code = "inactive-program-" + System.nanoTime();

		String createResponse = mockMvc.perform(post("/api/v1/admin/experiments/programs")
						.cookie(cookie).with(csrf()).contentType(MediaType.APPLICATION_JSON)
						.content(programRequestJson(code, topic, m1, m2, m3)))
				.andExpect(status().isOk())
				.andReturn().getResponse().getContentAsString();
		String draftId = com.jayway.jsonpath.JsonPath.read(createResponse, "$.data.id");

		mockMvc.perform(post("/api/v1/admin/experiments/programs/" + draftId + "/publish").cookie(cookie).with(csrf()))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.code").value("EXPERIMENT_MISSION_INACTIVE"));
	}

	@Test
	void publish_withLoneReplacementGroup_returnsUnprocessableEntity() throws Exception {
		Cookie cookie = sessionCookie(Set.of(AdminRole.SUPER_ADMIN));
		ExperimentTopic topic = newTopic();
		ExperimentMission m1 = newMission(topic, true);
		ExperimentMission m2 = newMission(topic, true);
		ExperimentMission m3 = newMission(topic, true);
		String code = "lone-group-program-" + System.nanoTime();

		String body = """
				{"code":"%s","primaryTopicId":"%s","title":"제목","description":"설명","durationDays":3,
				"sourceType":"TEMPLATE","estimatedMinutesMin":5,"estimatedMinutesMax":10,"featured":false,
				"beginner":true,"displayOrder":0,
				"days":[{"dayNumber":1,"missionId":"%s","replaceable":true,"replacementGroup":"solo-group"},
				{"dayNumber":2,"missionId":"%s","replaceable":false,"replacementGroup":null},
				{"dayNumber":3,"missionId":"%s","replaceable":false,"replacementGroup":null}]}
				""".formatted(code, topic.getId(), m1.getId(), m2.getId(), m3.getId());

		String createResponse = mockMvc.perform(post("/api/v1/admin/experiments/programs")
						.cookie(cookie).with(csrf()).contentType(MediaType.APPLICATION_JSON).content(body))
				.andExpect(status().isOk())
				.andReturn().getResponse().getContentAsString();
		String draftId = com.jayway.jsonpath.JsonPath.read(createResponse, "$.data.id");

		mockMvc.perform(post("/api/v1/admin/experiments/programs/" + draftId + "/publish").cookie(cookie).with(csrf()))
				.andExpect(status().isUnprocessableEntity())
				.andExpect(jsonPath("$.code").value("EXPERIMENT_PROGRAM_REPLACEMENT_GROUP_INVALID"));
	}

	@Test
	void list_withQ_returnsOnlyMatchingPrograms() throws Exception {
		Cookie cookie = sessionCookie(Set.of(AdminRole.SUPER_ADMIN));
		ExperimentTopic topic = newTopic();
		ExperimentMission m1 = newMission(topic, true);
		ExperimentMission m2 = newMission(topic, true);
		ExperimentMission m3 = newMission(topic, true);
		String uniqueMarker = "찾아줘" + System.nanoTime();

		mockMvc.perform(post("/api/v1/admin/experiments/programs")
						.cookie(cookie).with(csrf()).contentType(MediaType.APPLICATION_JSON)
						.content(programRequestJson("q-match-" + System.nanoTime(), uniqueMarker + " 코스", topic, m1, m2, m3)))
				.andExpect(status().isOk());
		mockMvc.perform(post("/api/v1/admin/experiments/programs")
						.cookie(cookie).with(csrf()).contentType(MediaType.APPLICATION_JSON)
						.content(programRequestJson("q-nomatch-" + System.nanoTime(), "관련 없는 코스", topic, m1, m2, m3)))
				.andExpect(status().isOk());

		mockMvc.perform(get("/api/v1/admin/experiments/programs").cookie(cookie).param("q", uniqueMarker))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.data.length()").value(1))
				.andExpect(jsonPath("$.data[0].title").value(uniqueMarker + " 코스"));
	}

	@Test
	void list_withDisallowedSortField_returnsValidationError() throws Exception {
		Cookie cookie = sessionCookie(Set.of(AdminRole.SUPER_ADMIN));

		mockMvc.perform(get("/api/v1/admin/experiments/programs").cookie(cookie).param("sort", "description,asc"))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.code").value("COMMON_VALIDATION_FAILED"));
	}

	private String programRequestJson(
			String code, String title, ExperimentTopic topic, ExperimentMission m1, ExperimentMission m2, ExperimentMission m3) {
		return """
				{"code":"%s","primaryTopicId":"%s","title":"%s","description":"코스 설명","durationDays":3,
				"sourceType":"TEMPLATE","estimatedMinutesMin":5,"estimatedMinutesMax":10,"featured":false,
				"beginner":true,"displayOrder":0,
				"days":[{"dayNumber":1,"missionId":"%s","replaceable":false,"replacementGroup":null},
				{"dayNumber":2,"missionId":"%s","replaceable":false,"replacementGroup":null},
				{"dayNumber":3,"missionId":"%s","replaceable":false,"replacementGroup":null}]}
				""".formatted(code, topic.getId(), title, m1.getId(), m2.getId(), m3.getId());
	}

	private String programRequestJson(
			String code, ExperimentTopic topic, ExperimentMission m1, ExperimentMission m2, ExperimentMission m3) {
		return """
				{"code":"%s","primaryTopicId":"%s","title":"코스 제목","description":"코스 설명","durationDays":3,
				"sourceType":"TEMPLATE","estimatedMinutesMin":5,"estimatedMinutesMax":10,"featured":false,
				"beginner":true,"displayOrder":0,
				"days":[{"dayNumber":1,"missionId":"%s","replaceable":false,"replacementGroup":null},
				{"dayNumber":2,"missionId":"%s","replaceable":false,"replacementGroup":null},
				{"dayNumber":3,"missionId":"%s","replaceable":false,"replacementGroup":null}]}
				""".formatted(code, topic.getId(), m1.getId(), m2.getId(), m3.getId());
	}

	private ExperimentTopic newTopic() {
		return experimentTopicRepository.save(
				ExperimentTopic.create("topic-" + System.nanoTime(), "주제", null, 1, true));
	}

	private ExperimentMission newMission(ExperimentTopic topic, boolean active) {
		return experimentMissionRepository.save(ExperimentMission.create(
				"mission-" + System.nanoTime() + "-" + Math.random(), topic, "미션 제목", "설명", "지시문",
				ExperimentMissionType.OBSERVATION, "TEXT", (short) 5, ExperimentEmotionalLoad.LOW,
				"[]", "[]", "{}", null, active));
	}

	private Cookie sessionCookie(Set<AdminRole> roles) {
		AdminUser adminUser = adminUserRepository.save(
				AdminUser.bootstrap("google-sub-" + System.nanoTime(), "admin@naroom.io", "관리자", roles));
		IssuedAdminSession issued = adminSessionService.issue(adminUser, "ip-hash", "ua-summary");
		return new Cookie("naroom_admin_session", issued.rawToken());
	}

}
