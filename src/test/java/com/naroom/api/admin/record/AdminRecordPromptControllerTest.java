package com.naroom.api.admin.record;

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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
@DirtiesContext
class AdminRecordPromptControllerTest {

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private AdminUserRepository adminUserRepository;

	@Autowired
	private AdminSessionService adminSessionService;

	@Test
	void fullLifecycle_createPublishReviseAndArchive() throws Exception {
		Cookie cookie = sessionCookie(Set.of(AdminRole.CONTENT_EDITOR));
		String code = "today-feeling-" + System.nanoTime();

		String createResponse = mockMvc.perform(post("/api/v1/admin/content/record-prompts")
						.cookie(cookie)
						.with(csrf())
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{"code":"%s","questionText":"오늘 기분은 어땠나요?","helperText":"편하게 적어보세요",
								"entryType":"PROMPT","displayOrder":1,"activeFrom":null,"activeUntil":null}
								""".formatted(code)))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.data.code").value(code))
				.andExpect(jsonPath("$.data.versionNo").value(1))
				.andExpect(jsonPath("$.data.status").value("DRAFT"))
				.andReturn().getResponse().getContentAsString();
		String draftId = com.jayway.jsonpath.JsonPath.read(createResponse, "$.data.id");

		mockMvc.perform(put("/api/v1/admin/content/record-prompts/" + draftId)
						.cookie(cookie)
						.with(csrf())
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{"questionText":"오늘 기분은 어땠나요?(수정)","helperText":"편하게 적어보세요",
								"entryType":"PROMPT","displayOrder":2,"activeFrom":null,"activeUntil":null}
								"""))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.data.questionText").value("오늘 기분은 어땠나요?(수정)"))
				.andExpect(jsonPath("$.data.displayOrder").value(2));

		mockMvc.perform(post("/api/v1/admin/content/record-prompts/" + draftId + "/publish")
						.cookie(cookie)
						.with(csrf()))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.data.status").value("PUBLISHED"));

		String revisionResponse = mockMvc.perform(post("/api/v1/admin/content/record-prompts/" + draftId + "/revisions")
						.cookie(cookie)
						.with(csrf()))
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.data.code").value(code))
				.andExpect(jsonPath("$.data.versionNo").value(2))
				.andExpect(jsonPath("$.data.status").value("DRAFT"))
				.andExpect(jsonPath("$.data.supersedesPromptId").value(draftId))
				.andReturn().getResponse().getContentAsString();
		String revisionId = com.jayway.jsonpath.JsonPath.read(revisionResponse, "$.data.id");

		// 새 버전을 발행하면 기존 발행본은 자동으로 ARCHIVED로 내려간다.
		mockMvc.perform(post("/api/v1/admin/content/record-prompts/" + revisionId + "/publish")
						.cookie(cookie)
						.with(csrf()))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.data.status").value("PUBLISHED"));

		// v1이 자동 ARCHIVED 되었으므로 다시 archive를 시도하면 상태 불일치로 거부된다.
		mockMvc.perform(post("/api/v1/admin/content/record-prompts/" + draftId + "/archive")
						.cookie(cookie)
						.with(csrf()))
				.andExpect(status().isConflict())
				.andExpect(jsonPath("$.code").value("RECORD_PROMPT_NOT_PUBLISHED"));
	}

	@Test
	void create_duplicateCode_returnsConflict() throws Exception {
		Cookie cookie = sessionCookie(Set.of(AdminRole.SUPER_ADMIN));
		String code = "dup-prompt-" + System.nanoTime();
		String body = """
				{"code":"%s","questionText":"질문","entryType":"PROMPT","displayOrder":1}
				""".formatted(code);

		mockMvc.perform(post("/api/v1/admin/content/record-prompts")
						.cookie(cookie).with(csrf()).contentType(MediaType.APPLICATION_JSON).content(body))
				.andExpect(status().isOk());

		mockMvc.perform(post("/api/v1/admin/content/record-prompts")
						.cookie(cookie).with(csrf()).contentType(MediaType.APPLICATION_JSON).content(body))
				.andExpect(status().isConflict())
				.andExpect(jsonPath("$.code").value("RECORD_PROMPT_CODE_ALREADY_EXISTS"));
	}

	@Test
	void archive_onlyPublishedPrompt_isBlocked() throws Exception {
		Cookie cookie = sessionCookie(Set.of(AdminRole.SUPER_ADMIN));
		String code = "solo-prompt-" + System.nanoTime();

		String createResponse = mockMvc.perform(post("/api/v1/admin/content/record-prompts")
						.cookie(cookie).with(csrf()).contentType(MediaType.APPLICATION_JSON)
						.content("""
								{"code":"%s","questionText":"질문","entryType":"PROMPT","displayOrder":1}
								""".formatted(code)))
				.andExpect(status().isOk())
				.andReturn().getResponse().getContentAsString();
		String id = com.jayway.jsonpath.JsonPath.read(createResponse, "$.data.id");

		mockMvc.perform(post("/api/v1/admin/content/record-prompts/" + id + "/publish").cookie(cookie).with(csrf()))
				.andExpect(status().isOk());

		mockMvc.perform(post("/api/v1/admin/content/record-prompts/" + id + "/archive").cookie(cookie).with(csrf()))
				.andExpect(status().isConflict())
				.andExpect(jsonPath("$.code").value("RECORD_PROMPT_LAST_PUBLISHED_CANNOT_BE_ARCHIVED"));
	}

	private Cookie sessionCookie(Set<AdminRole> roles) {
		AdminUser adminUser = adminUserRepository.save(
				AdminUser.bootstrap("google-sub-" + System.nanoTime(), "admin@naroom.io", "관리자", roles));
		IssuedAdminSession issued = adminSessionService.issue(adminUser, "ip-hash", "ua-summary");
		return new Cookie("naroom_admin_session", issued.rawToken());
	}

}
