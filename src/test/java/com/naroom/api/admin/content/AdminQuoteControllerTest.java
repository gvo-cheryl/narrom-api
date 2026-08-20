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
		String code = "quote-" + System.nanoTime();

		String createResponse = mockMvc.perform(post("/api/v1/admin/content/quotes")
						.cookie(cookie)
						.with(csrf())
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{"code":"%s","text":"오늘도 잘 하고 있어요","authorName":null,"sourceName":null,
								"sourceUrl":null,"topicIds":["%s"],"activeFrom":null,"activeUntil":null}
								""".formatted(code, topic.getId())))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.data.code").value(code))
				.andExpect(jsonPath("$.data.versionNo").value(1))
				.andExpect(jsonPath("$.data.status").value("DRAFT"))
				.andExpect(jsonPath("$.data.topicIds[0]").value(topic.getId().toString()))
				.andReturn().getResponse().getContentAsString();
		String draftId = com.jayway.jsonpath.JsonPath.read(createResponse, "$.data.id");

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
	void create_duplicateCode_returnsConflict() throws Exception {
		Cookie cookie = sessionCookie(Set.of(AdminRole.SUPER_ADMIN));
		String code = "dup-quote-" + System.nanoTime();
		String body = """
				{"code":"%s","text":"문장","topicIds":[]}
				""".formatted(code);

		mockMvc.perform(post("/api/v1/admin/content/quotes")
						.cookie(cookie).with(csrf()).contentType(MediaType.APPLICATION_JSON).content(body))
				.andExpect(status().isOk());

		mockMvc.perform(post("/api/v1/admin/content/quotes")
						.cookie(cookie).with(csrf()).contentType(MediaType.APPLICATION_JSON).content(body))
				.andExpect(status().isConflict())
				.andExpect(jsonPath("$.code").value("CONTENT_QUOTE_CODE_ALREADY_EXISTS"));
	}

	@Test
	void archive_publishedQuote_succeeds() throws Exception {
		Cookie cookie = sessionCookie(Set.of(AdminRole.SUPER_ADMIN));
		String code = "solo-quote-" + System.nanoTime();

		String createResponse = mockMvc.perform(post("/api/v1/admin/content/quotes")
						.cookie(cookie).with(csrf()).contentType(MediaType.APPLICATION_JSON)
						.content("""
								{"code":"%s","text":"문장","topicIds":[]}
								""".formatted(code)))
				.andExpect(status().isOk())
				.andReturn().getResponse().getContentAsString();
		String id = com.jayway.jsonpath.JsonPath.read(createResponse, "$.data.id");

		mockMvc.perform(post("/api/v1/admin/content/quotes/" + id + "/publish").cookie(cookie).with(csrf()))
				.andExpect(status().isOk());

		mockMvc.perform(post("/api/v1/admin/content/quotes/" + id + "/archive").cookie(cookie).with(csrf()))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.data.status").value("ARCHIVED"));
	}

	private Cookie sessionCookie(Set<AdminRole> roles) {
		AdminUser adminUser = adminUserRepository.save(
				AdminUser.bootstrap("google-sub-" + System.nanoTime(), "admin@naroom.io", "관리자", roles));
		IssuedAdminSession issued = adminSessionService.issue(adminUser, "ip-hash", "ua-summary");
		return new Cookie("naroom_admin_session", issued.rawToken());
	}

}
