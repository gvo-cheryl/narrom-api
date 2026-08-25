package com.naroom.api.admin.content;

import com.naroom.api.admin.auth.AdminSessionService;
import com.naroom.api.admin.auth.IssuedAdminSession;
import com.naroom.api.admin.domain.entity.AdminRole;
import com.naroom.api.admin.domain.entity.AdminUser;
import com.naroom.api.admin.domain.repository.AdminUserRepository;
import com.naroom.api.content.domain.entity.QuoteTopic;
import com.naroom.api.content.domain.repository.QuoteTopicRepository;
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
class AdminQuoteControllerTest {

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private AdminUserRepository adminUserRepository;

	@Autowired
	private AdminSessionService adminSessionService;

	@Autowired
	private QuoteTopicRepository quoteTopicRepository;

	@Test
	void fullLifecycle_createPublishReviseAndArchive() throws Exception {
		Cookie cookie = sessionCookie(Set.of(AdminRole.CONTENT_EDITOR));
		QuoteTopic topic = quoteTopicRepository.save(QuoteTopic.create("topic-" + System.nanoTime(), "쉼과 속도"));

		String createResponse = mockMvc.perform(post("/api/v1/admin/content/quotes")
						.cookie(cookie)
						.with(csrf())
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{"text":"오늘도 잘 하고 있어요","authorName":null,"sourceName":null,
								"sourceUrl":null,"topicIds":["%s"],"activeFrom":null,"activeUntil":null}
								""".formatted(topic.getId())))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.data.versionNo").value(1))
				.andExpect(jsonPath("$.data.status").value("DRAFT"))
				.andExpect(jsonPath("$.data.topicIds[0]").value(topic.getId().toString()))
				.andReturn().getResponse().getContentAsString();
		String draftId = com.jayway.jsonpath.JsonPath.read(createResponse, "$.data.id");
		String code = com.jayway.jsonpath.JsonPath.read(createResponse, "$.data.code");

		mockMvc.perform(put("/api/v1/admin/content/quotes/" + draftId)
						.cookie(cookie)
						.with(csrf())
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{"text":"오늘도 잘 하고 있어요(수정)","authorName":"나로움","sourceName":null,
								"sourceUrl":null,"topicIds":["%s"],"activeFrom":null,"activeUntil":null}
								""".formatted(topic.getId())))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.data.text").value("오늘도 잘 하고 있어요(수정)"))
				.andExpect(jsonPath("$.data.authorName").value("나로움"));

		mockMvc.perform(post("/api/v1/admin/content/quotes/" + draftId + "/publish").cookie(cookie).with(csrf()))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.data.status").value("PUBLISHED"));

		String revisionResponse = mockMvc.perform(post("/api/v1/admin/content/quotes/" + draftId + "/revisions")
						.cookie(cookie).with(csrf()))
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.data.code").value(code))
				.andExpect(jsonPath("$.data.versionNo").value(2))
				.andExpect(jsonPath("$.data.status").value("DRAFT"))
				.andExpect(jsonPath("$.data.supersedesQuoteId").value(draftId))
				.andExpect(jsonPath("$.data.topicIds[0]").value(topic.getId().toString()))
				.andReturn().getResponse().getContentAsString();
		String revisionId = com.jayway.jsonpath.JsonPath.read(revisionResponse, "$.data.id");

		// 새 버전을 발행하면 기존 발행본은 자동으로 ARCHIVED로 내려간다.
		mockMvc.perform(post("/api/v1/admin/content/quotes/" + revisionId + "/publish").cookie(cookie).with(csrf()))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.data.status").value("PUBLISHED"));

		mockMvc.perform(post("/api/v1/admin/content/quotes/" + draftId + "/archive").cookie(cookie).with(csrf()))
				.andExpect(status().isConflict())
				.andExpect(jsonPath("$.code").value("CONTENT_QUOTE_NOT_PUBLISHED"));
	}

	@Test
	void archive_publishedQuote_succeeds() throws Exception {
		Cookie cookie = sessionCookie(Set.of(AdminRole.SUPER_ADMIN));

		String createResponse = mockMvc.perform(post("/api/v1/admin/content/quotes")
						.cookie(cookie).with(csrf()).contentType(MediaType.APPLICATION_JSON)
						.content("""
								{"text":"문장","topicIds":[]}
								"""))
				.andExpect(status().isOk())
				.andReturn().getResponse().getContentAsString();
		String id = com.jayway.jsonpath.JsonPath.read(createResponse, "$.data.id");

		mockMvc.perform(post("/api/v1/admin/content/quotes/" + id + "/publish").cookie(cookie).with(csrf()))
				.andExpect(status().isOk());

		mockMvc.perform(post("/api/v1/admin/content/quotes/" + id + "/archive").cookie(cookie).with(csrf()))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.data.status").value("ARCHIVED"));
	}

	@Test
	void list_withoutParams_returnsAll() throws Exception {
		Cookie cookie = sessionCookie(Set.of(AdminRole.SUPER_ADMIN));
		createQuote(cookie, "문장");

		mockMvc.perform(get("/api/v1/admin/content/quotes").cookie(cookie))
				.andExpect(status().isOk());
	}

	@Test
	void list_withQ_returnsOnlyMatchingQuotes() throws Exception {
		Cookie cookie = sessionCookie(Set.of(AdminRole.SUPER_ADMIN));
		String uniqueMarker = "찾아줘" + System.nanoTime();
		createQuote(cookie, uniqueMarker + " 문장");
		createQuote(cookie, "관련 없는 문장");

		mockMvc.perform(get("/api/v1/admin/content/quotes").cookie(cookie).param("q", uniqueMarker))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.data.length()").value(1))
				.andExpect(jsonPath("$.data[0].text").value(uniqueMarker + " 문장"));
	}

	@Test
	void list_withSort_ordersByRequestedField() throws Exception {
		Cookie cookie = sessionCookie(Set.of(AdminRole.SUPER_ADMIN));
		String marker = "sort-" + System.nanoTime();
		createQuoteWithActiveFrom(cookie, marker + " b", "2026-02-01T00:00:00Z");
		createQuoteWithActiveFrom(cookie, marker + " a", "2026-01-01T00:00:00Z");

		mockMvc.perform(get("/api/v1/admin/content/quotes")
						.cookie(cookie).param("q", marker).param("sort", "activeFrom,asc"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.data[0].text").value(marker + " a"))
				.andExpect(jsonPath("$.data[1].text").value(marker + " b"));
	}

	@Test
	void list_withDisallowedSortField_returnsValidationError() throws Exception {
		Cookie cookie = sessionCookie(Set.of(AdminRole.SUPER_ADMIN));

		mockMvc.perform(get("/api/v1/admin/content/quotes").cookie(cookie).param("sort", "authorName,asc"))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.code").value("COMMON_VALIDATION_FAILED"));
	}

	private void createQuote(Cookie cookie, String text) throws Exception {
		mockMvc.perform(post("/api/v1/admin/content/quotes")
						.cookie(cookie).with(csrf()).contentType(MediaType.APPLICATION_JSON)
						.content("""
								{"text":"%s","topicIds":[]}
								""".formatted(text)))
				.andExpect(status().isOk());
	}

	private void createQuoteWithActiveFrom(Cookie cookie, String text, String activeFrom) throws Exception {
		mockMvc.perform(post("/api/v1/admin/content/quotes")
						.cookie(cookie).with(csrf()).contentType(MediaType.APPLICATION_JSON)
						.content("""
								{"text":"%s","topicIds":[],"activeFrom":"%s"}
								""".formatted(text, activeFrom)))
				.andExpect(status().isOk());
	}

	private Cookie sessionCookie(Set<AdminRole> roles) {
		AdminUser adminUser = adminUserRepository.save(
				AdminUser.bootstrap("google-sub-" + System.nanoTime(), "admin@naroom.io", "관리자", roles));
		IssuedAdminSession issued = adminSessionService.issue(adminUser, "ip-hash", "ua-summary");
		return new Cookie("naroom_admin_session", issued.rawToken());
	}

}
