package com.naroom.api.admin.ai;

import com.naroom.api.admin.auth.AdminSessionService;
import com.naroom.api.admin.auth.IssuedAdminSession;
import com.naroom.api.admin.domain.entity.AdminRole;
import com.naroom.api.admin.domain.entity.AdminUser;
import com.naroom.api.admin.domain.repository.AdminUserRepository;
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
class AdminAiPromptControllerTest {

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private AdminUserRepository adminUserRepository;

	@Autowired
	private AdminSessionService adminSessionService;

	@Test
	void fullLifecycle_createUpdatePublishReviseAndArchive() throws Exception {
		Cookie cookie = sessionCookie(Set.of(AdminRole.AI_OPERATOR));
		String label = "er-test-" + (System.nanoTime() % 100000);

		String createResponse = mockMvc.perform(post("/api/v1/admin/ai/prompts")
						.cookie(cookie)
						.with(csrf())
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{"scope":"FEATURE","featureType":"ENTRY_REFLECTION","versionLabel":"%s",
								"content":"초안 지침","modelName":"gpt-4o-mini","outputMaxLength":400}
								""".formatted(label)))
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.data.status").value("DRAFT"))
				.andExpect(jsonPath("$.data.content").value("초안 지침"))
				.andReturn().getResponse().getContentAsString();
		String id = com.jayway.jsonpath.JsonPath.read(createResponse, "$.data.id");

		mockMvc.perform(put("/api/v1/admin/ai/prompts/" + id)
						.cookie(cookie)
						.with(csrf())
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{"versionLabel":"%s","content":"수정된 지침","modelName":"gpt-4o","outputMaxLength":500}
								""".formatted(label)))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.data.content").value("수정된 지침"))
				.andExpect(jsonPath("$.data.modelName").value("gpt-4o"));

		mockMvc.perform(post("/api/v1/admin/ai/prompts/" + id + "/publish").cookie(cookie).with(csrf()))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.data.status").value("PUBLISHED"));

		String revisionResponse = mockMvc.perform(post("/api/v1/admin/ai/prompts/" + id + "/revisions")
						.cookie(cookie)
						.with(csrf())
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{"versionLabel":"%s-rev2"}
								""".formatted(label)))
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.data.status").value("DRAFT"))
				.andExpect(jsonPath("$.data.content").value("수정된 지침"))
				.andExpect(jsonPath("$.data.supersedesVersionId").value(id))
				.andReturn().getResponse().getContentAsString();
		String revisionId = com.jayway.jsonpath.JsonPath.read(revisionResponse, "$.data.id");

		// 같은 슬롯에 새 버전을 발행하면 이전 발행본은 자동으로 ARCHIVED된다.
		mockMvc.perform(post("/api/v1/admin/ai/prompts/" + revisionId + "/publish").cookie(cookie).with(csrf()))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.data.status").value("PUBLISHED"));

		mockMvc.perform(get("/api/v1/admin/ai/prompts/" + id).cookie(cookie))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.data.status").value("ARCHIVED"));
	}

	@Test
	void create_duplicateLabelInSameScope_returnsConflict() throws Exception {
		Cookie cookie = sessionCookie(Set.of(AdminRole.AI_OPERATOR));
		String label = "wr-test-" + (System.nanoTime() % 100000);
		String body = """
				{"scope":"FEATURE","featureType":"WEEKLY_REFLECTION","versionLabel":"%s","content":"지침"}
				""".formatted(label);

		mockMvc.perform(post("/api/v1/admin/ai/prompts").cookie(cookie).with(csrf())
						.contentType(MediaType.APPLICATION_JSON).content(body))
				.andExpect(status().isCreated());

		mockMvc.perform(post("/api/v1/admin/ai/prompts").cookie(cookie).with(csrf())
						.contentType(MediaType.APPLICATION_JSON).content(body))
				.andExpect(status().isConflict())
				.andExpect(jsonPath("$.code").value("AI_PROMPT_VERSION_LABEL_ALREADY_EXISTS"));
	}

	@Test
	void publish_notDraft_returnsConflict() throws Exception {
		Cookie cookie = sessionCookie(Set.of(AdminRole.AI_OPERATOR));
		String label = "3d-test-" + (System.nanoTime() % 100000);
		String createResponse = mockMvc.perform(post("/api/v1/admin/ai/prompts").cookie(cookie).with(csrf())
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{"scope":"FEATURE","featureType":"THREE_DAY_REFLECTION","versionLabel":"%s","content":"지침"}
								""".formatted(label)))
				.andExpect(status().isCreated())
				.andReturn().getResponse().getContentAsString();
		String id = com.jayway.jsonpath.JsonPath.read(createResponse, "$.data.id");
		mockMvc.perform(post("/api/v1/admin/ai/prompts/" + id + "/publish").cookie(cookie).with(csrf()))
				.andExpect(status().isOk());

		mockMvc.perform(post("/api/v1/admin/ai/prompts/" + id + "/publish").cookie(cookie).with(csrf()))
				.andExpect(status().isConflict())
				.andExpect(jsonPath("$.code").value("AI_PROMPT_VERSION_NOT_DRAFT"));
	}

	@Test
	void list_withContentEditorRole_returnsAccessDenied() throws Exception {
		Cookie cookie = sessionCookie(Set.of(AdminRole.CONTENT_EDITOR));

		mockMvc.perform(get("/api/v1/admin/ai/prompts").cookie(cookie))
				.andExpect(status().isForbidden())
				.andExpect(jsonPath("$.code").value("ADMIN_ACCESS_DENIED"));
	}

	private Cookie sessionCookie(Set<AdminRole> roles) {
		AdminUser adminUser = adminUserRepository.save(
				AdminUser.bootstrap("google-sub-" + System.nanoTime(), "admin@naroom.io", "관리자", roles));
		IssuedAdminSession issued = adminSessionService.issue(adminUser, "ip-hash", "ua-summary");
		return new Cookie("naroom_admin_session", issued.rawToken());
	}

}
