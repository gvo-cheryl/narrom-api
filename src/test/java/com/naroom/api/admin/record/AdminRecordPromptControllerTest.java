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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
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

		String createResponse = mockMvc.perform(post("/api/v1/admin/content/record-prompts")
						.cookie(cookie)
						.with(csrf())
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{"questionText":"오늘 기분은 어땠나요?","helperText":"편하게 적어보세요",
								"entryType":"PROMPT","displayOrder":1,"activeFrom":null,"activeUntil":null}
								"""))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.data.versionNo").value(1))
				.andExpect(jsonPath("$.data.status").value("DRAFT"))
				.andReturn().getResponse().getContentAsString();
		String draftId = com.jayway.jsonpath.JsonPath.read(createResponse, "$.data.id");
		String code = com.jayway.jsonpath.JsonPath.read(createResponse, "$.data.code");

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
	void archive_onlyPublishedPrompt_isBlocked() throws Exception {
		Cookie cookie = sessionCookie(Set.of(AdminRole.SUPER_ADMIN));

		String createResponse = mockMvc.perform(post("/api/v1/admin/content/record-prompts")
						.cookie(cookie).with(csrf()).contentType(MediaType.APPLICATION_JSON)
						.content("""
								{"questionText":"질문","entryType":"PROMPT","displayOrder":1}
								"""))
				.andExpect(status().isOk())
				.andReturn().getResponse().getContentAsString();
		String id = com.jayway.jsonpath.JsonPath.read(createResponse, "$.data.id");

		mockMvc.perform(post("/api/v1/admin/content/record-prompts/" + id + "/publish").cookie(cookie).with(csrf()))
				.andExpect(status().isOk());

		mockMvc.perform(post("/api/v1/admin/content/record-prompts/" + id + "/archive").cookie(cookie).with(csrf()))
				.andExpect(status().isConflict())
				.andExpect(jsonPath("$.code").value("RECORD_PROMPT_LAST_PUBLISHED_CANNOT_BE_ARCHIVED"));
	}

	@Test
	void list_withQ_returnsOnlyMatchingPrompts() throws Exception {
		Cookie cookie = sessionCookie(Set.of(AdminRole.SUPER_ADMIN));
		String uniqueMarker = "찾아줘" + System.nanoTime();
		createPrompt(cookie, uniqueMarker + " 질문");
		createPrompt(cookie, "관련 없는 질문");

		mockMvc.perform(get("/api/v1/admin/content/record-prompts").cookie(cookie).param("q", uniqueMarker))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.data.length()").value(1))
				.andExpect(jsonPath("$.data[0].questionText").value(uniqueMarker + " 질문"));
	}

	@Test
	void list_withDisallowedSortField_returnsValidationError() throws Exception {
		Cookie cookie = sessionCookie(Set.of(AdminRole.SUPER_ADMIN));

		mockMvc.perform(get("/api/v1/admin/content/record-prompts").cookie(cookie).param("sort", "helperText,asc"))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.code").value("COMMON_VALIDATION_FAILED"));
	}

	private void createPrompt(Cookie cookie, String questionText) throws Exception {
		mockMvc.perform(post("/api/v1/admin/content/record-prompts")
						.cookie(cookie).with(csrf()).contentType(MediaType.APPLICATION_JSON)
						.content("""
								{"questionText":"%s","entryType":"PROMPT","displayOrder":1}
								""".formatted(questionText)))
				.andExpect(status().isOk());
	}

	private Cookie sessionCookie(Set<AdminRole> roles) {
		AdminUser adminUser = adminUserRepository.save(
				AdminUser.bootstrap("google-sub-" + System.nanoTime(), "admin@naroom.io", "관리자", roles));
		IssuedAdminSession issued = adminSessionService.issue(adminUser, "ip-hash", "ua-summary");
		return new Cookie("naroom_admin_session", issued.rawToken());
	}

}
