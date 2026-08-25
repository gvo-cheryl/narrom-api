package com.naroom.api.admin.content;

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
class AdminAppContentItemControllerTest {

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private AdminUserRepository adminUserRepository;

	@Autowired
	private AdminSessionService adminSessionService;

	@Test
	void fullLifecycle_createPublishReviseAndArchive() throws Exception {
		Cookie cookie = sessionCookie(Set.of(AdminRole.CONTENT_EDITOR));
		String key = "home.greeting-" + System.nanoTime();

		String createResponse = mockMvc.perform(post("/api/v1/admin/content/app-copy")
						.cookie(cookie).with(csrf()).contentType(MediaType.APPLICATION_JSON)
						.content("""
								{"contentKey":"%s","surface":"home","locale":null,"valueType":"TEXT",
								"valueText":"오늘도 여기까지 왔네요","valueJson":null,"schemaVersion":"v1",
								"activeFrom":null,"activeUntil":null,"fallbackRequired":true}
								""".formatted(key)))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.data.contentKey").value(key))
				.andExpect(jsonPath("$.data.locale").value("ko-KR"))
				.andExpect(jsonPath("$.data.versionNo").value(1))
				.andExpect(jsonPath("$.data.status").value("DRAFT"))
				.andReturn().getResponse().getContentAsString();
		String draftId = com.jayway.jsonpath.JsonPath.read(createResponse, "$.data.id");

		mockMvc.perform(put("/api/v1/admin/content/app-copy/" + draftId)
						.cookie(cookie).with(csrf()).contentType(MediaType.APPLICATION_JSON)
						.content("""
								{"surface":"home","valueType":"TEXT","valueText":"오늘도 여기까지 왔네요(수정)",
								"valueJson":null,"schemaVersion":"v1","activeFrom":null,"activeUntil":null,
								"fallbackRequired":true}
								"""))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.data.valueText").value("오늘도 여기까지 왔네요(수정)"));

		mockMvc.perform(post("/api/v1/admin/content/app-copy/" + draftId + "/publish").cookie(cookie).with(csrf()))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.data.status").value("PUBLISHED"));

		String revisionResponse = mockMvc.perform(post("/api/v1/admin/content/app-copy/" + draftId + "/revisions")
						.cookie(cookie).with(csrf()))
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.data.contentKey").value(key))
				.andExpect(jsonPath("$.data.versionNo").value(2))
				.andExpect(jsonPath("$.data.status").value("DRAFT"))
				.andExpect(jsonPath("$.data.supersedesItemId").value(draftId))
				.andReturn().getResponse().getContentAsString();
		String revisionId = com.jayway.jsonpath.JsonPath.read(revisionResponse, "$.data.id");

		// 새 버전을 발행하면 기존 발행본은 자동으로 ARCHIVED로 내려간다.
		mockMvc.perform(post("/api/v1/admin/content/app-copy/" + revisionId + "/publish").cookie(cookie).with(csrf()))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.data.status").value("PUBLISHED"));

		mockMvc.perform(post("/api/v1/admin/content/app-copy/" + draftId + "/archive").cookie(cookie).with(csrf()))
				.andExpect(status().isConflict())
				.andExpect(jsonPath("$.code").value("APP_CONTENT_ITEM_NOT_PUBLISHED"));
	}

	@Test
	void create_duplicateKeyAndLocale_returnsConflict() throws Exception {
		Cookie cookie = sessionCookie(Set.of(AdminRole.SUPER_ADMIN));
		String key = "record.free.placeholder-" + System.nanoTime();
		String body = """
				{"contentKey":"%s","surface":"record","locale":"ko-KR","valueType":"TEXT",
				"valueText":"오늘 하루는 어땠나요","valueJson":null,"schemaVersion":"v1",
				"activeFrom":null,"activeUntil":null,"fallbackRequired":true}
				""".formatted(key);

		mockMvc.perform(post("/api/v1/admin/content/app-copy")
						.cookie(cookie).with(csrf()).contentType(MediaType.APPLICATION_JSON).content(body))
				.andExpect(status().isOk());

		mockMvc.perform(post("/api/v1/admin/content/app-copy")
						.cookie(cookie).with(csrf()).contentType(MediaType.APPLICATION_JSON).content(body))
				.andExpect(status().isConflict())
				.andExpect(jsonPath("$.code").value("APP_CONTENT_ITEM_KEY_ALREADY_EXISTS"));
	}

	@Test
	void create_valueTypeMismatch_returnsUnprocessableEntity() throws Exception {
		Cookie cookie = sessionCookie(Set.of(AdminRole.SUPER_ADMIN));
		String key = "experiment.empty.title-" + System.nanoTime();
		String body = """
				{"contentKey":"%s","surface":"experiment","locale":"ko-KR","valueType":"TEXT",
				"valueText":null,"valueJson":null,"schemaVersion":"v1",
				"activeFrom":null,"activeUntil":null,"fallbackRequired":true}
				""".formatted(key);

		mockMvc.perform(post("/api/v1/admin/content/app-copy")
						.cookie(cookie).with(csrf()).contentType(MediaType.APPLICATION_JSON).content(body))
				.andExpect(status().isUnprocessableEntity())
				.andExpect(jsonPath("$.code").value("APP_CONTENT_VALUE_TYPE_MISMATCH"));
	}

	@Test
	void list_withQ_returnsOnlyMatchingItems() throws Exception {
		Cookie cookie = sessionCookie(Set.of(AdminRole.SUPER_ADMIN));
		String uniqueMarker = "찾아줘" + System.nanoTime();
		createItem(cookie, "ac-match-" + System.nanoTime(), uniqueMarker + " 문구");
		createItem(cookie, "ac-nomatch-" + System.nanoTime(), "관련 없는 문구");

		mockMvc.perform(get("/api/v1/admin/content/app-copy").cookie(cookie).param("q", uniqueMarker))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.data.length()").value(1))
				.andExpect(jsonPath("$.data[0].valueText").value(uniqueMarker + " 문구"));
	}

	@Test
	void list_withDisallowedSortField_returnsValidationError() throws Exception {
		Cookie cookie = sessionCookie(Set.of(AdminRole.SUPER_ADMIN));

		mockMvc.perform(get("/api/v1/admin/content/app-copy").cookie(cookie).param("sort", "valueText,asc"))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.code").value("COMMON_VALIDATION_FAILED"));
	}

	private void createItem(Cookie cookie, String key, String valueText) throws Exception {
		mockMvc.perform(post("/api/v1/admin/content/app-copy")
						.cookie(cookie).with(csrf()).contentType(MediaType.APPLICATION_JSON)
						.content("""
								{"contentKey":"%s","surface":"home","locale":null,"valueType":"TEXT",
								"valueText":"%s","valueJson":null,"schemaVersion":"v1",
								"activeFrom":null,"activeUntil":null,"fallbackRequired":true}
								""".formatted(key, valueText)))
				.andExpect(status().isOk());
	}

	private Cookie sessionCookie(Set<AdminRole> roles) {
		AdminUser adminUser = adminUserRepository.save(
				AdminUser.bootstrap("google-sub-" + System.nanoTime(), "admin@naroom.io", "관리자", roles));
		IssuedAdminSession issued = adminSessionService.issue(adminUser, "ip-hash", "ua-summary");
		return new Cookie("naroom_admin_session", issued.rawToken());
	}

}
