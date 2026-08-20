package com.naroom.api.admin.experiment;

import com.naroom.api.admin.auth.AdminSessionService;
import com.naroom.api.admin.auth.IssuedAdminSession;
import com.naroom.api.admin.domain.entity.AdminRole;
import com.naroom.api.admin.domain.entity.AdminUser;
import com.naroom.api.admin.domain.repository.AdminUserRepository;
import com.naroom.api.experiment.domain.entity.ExperimentTopic;
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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
@DirtiesContext
class AdminExperimentTopicControllerTest {

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private AdminUserRepository adminUserRepository;

	@Autowired
	private AdminSessionService adminSessionService;

	@Autowired
	private ExperimentTopicRepository experimentTopicRepository;

	@Test
	void create_withContentEditorRole_returnsCreatedTopic() throws Exception {
		Cookie cookie = sessionCookie(Set.of(AdminRole.CONTENT_EDITOR));

		mockMvc.perform(post("/api/v1/admin/experiments/topics")
						.cookie(cookie)
						.with(csrf())
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{"code":"emotion","name":"감정","description":"감정 관찰","displayOrder":1,"active":true}
								"""))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.data.code").value("emotion"))
				.andExpect(jsonPath("$.data.name").value("감정"))
				.andExpect(jsonPath("$.data.active").value(true));
	}

	@Test
	void create_duplicateCode_returnsConflict() throws Exception {
		experimentTopicRepository.save(ExperimentTopic.create("relationship", "관계", null, 1, true));
		Cookie cookie = sessionCookie(Set.of(AdminRole.SUPER_ADMIN));

		mockMvc.perform(post("/api/v1/admin/experiments/topics")
						.cookie(cookie)
						.with(csrf())
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{"code":"relationship","name":"관계2","displayOrder":2,"active":true}
								"""))
				.andExpect(status().isConflict())
				.andExpect(jsonPath("$.code").value("EXPERIMENT_TOPIC_CODE_DUPLICATE"));
	}

	@Test
	void create_withoutContentPermissionRole_returnsAccessDenied() throws Exception {
		Cookie cookie = sessionCookie(Set.of(AdminRole.SUPPORT_READ_ONLY));

		mockMvc.perform(post("/api/v1/admin/experiments/topics")
						.cookie(cookie)
						.with(csrf())
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{"code":"thought","name":"생각","displayOrder":1,"active":true}
								"""))
				.andExpect(status().isForbidden())
				.andExpect(jsonPath("$.code").value("ADMIN_ACCESS_DENIED"));
	}

	@Test
	void update_changesEditableFieldsButNotCode() throws Exception {
		ExperimentTopic topic = experimentTopicRepository.save(ExperimentTopic.create("value", "가치관", null, 3, true));
		Cookie cookie = sessionCookie(Set.of(AdminRole.SUPER_ADMIN));

		mockMvc.perform(put("/api/v1/admin/experiments/topics/" + topic.getId())
						.cookie(cookie)
						.with(csrf())
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{"name":"가치관(수정)","description":"수정된 설명","displayOrder":9,"active":false}
								"""))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.data.code").value("value"))
				.andExpect(jsonPath("$.data.name").value("가치관(수정)"))
				.andExpect(jsonPath("$.data.displayOrder").value(9))
				.andExpect(jsonPath("$.data.active").value(false));
	}

	@Test
	void get_unknownId_returnsNotFound() throws Exception {
		Cookie cookie = sessionCookie(Set.of(AdminRole.SUPER_ADMIN));

		mockMvc.perform(get("/api/v1/admin/experiments/topics/" + java.util.UUID.randomUUID())
						.cookie(cookie))
				.andExpect(status().isNotFound())
				.andExpect(jsonPath("$.code").value("EXPERIMENT_TOPIC_NOT_FOUND"));
	}

	private Cookie sessionCookie(Set<AdminRole> roles) {
		AdminUser adminUser = adminUserRepository.save(
				AdminUser.bootstrap("google-sub-" + System.nanoTime(), "admin@naroom.io", "관리자", roles));
		IssuedAdminSession issued = adminSessionService.issue(adminUser, "ip-hash", "ua-summary");
		return new Cookie("naroom_admin_session", issued.rawToken());
	}

}
