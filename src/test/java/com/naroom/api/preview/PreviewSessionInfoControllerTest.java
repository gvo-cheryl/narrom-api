package com.naroom.api.preview;

import com.naroom.api.admin.domain.entity.AdminRole;
import com.naroom.api.admin.domain.entity.AdminUser;
import com.naroom.api.admin.domain.repository.AdminUserRepository;
import com.naroom.api.admin.preview.IssuedPreviewSession;
import com.naroom.api.admin.preview.PreviewSessionService;
import com.naroom.api.preview.auth.PreviewTokenAuthenticationFilter;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;
import java.util.Set;
import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
@DirtiesContext
class PreviewSessionInfoControllerTest {

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private AdminUserRepository adminUserRepository;

	@Autowired
	private PreviewSessionService previewSessionService;

	@Test
	void get_withValidToken_returnsSessionInfo() throws Exception {
		UUID quoteVersionId = UUID.randomUUID();
		IssuedPreviewSession issued = issueSession(Map.of("quote", quoteVersionId), "first-visit");

		mockMvc.perform(get("/api/v1/preview/session")
						.header(PreviewTokenAuthenticationFilter.TOKEN_HEADER, issued.rawToken()))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.data.scenarioKey").value("first-visit"))
				.andExpect(jsonPath("$.data.selectedContentVersions.quote").value(quoteVersionId.toString()));
	}

	@Test
	void get_withoutToken_returnsAuthenticationFailed() throws Exception {
		mockMvc.perform(get("/api/v1/preview/session"))
				.andExpect(status().isUnauthorized())
				.andExpect(jsonPath("$.code").value("PREVIEW_AUTHENTICATION_FAILED"));
	}

	@Test
	void get_withBogusToken_returnsSessionExpired() throws Exception {
		mockMvc.perform(get("/api/v1/preview/session")
						.header(PreviewTokenAuthenticationFilter.TOKEN_HEADER, "bogus-token"))
				.andExpect(status().isUnauthorized())
				.andExpect(jsonPath("$.code").value("PREVIEW_SESSION_EXPIRED"));
	}

	private IssuedPreviewSession issueSession(Map<String, UUID> selectedContentVersions, String scenarioKey) {
		AdminUser adminUser = adminUserRepository.save(
				AdminUser.bootstrap("google-sub-" + System.nanoTime(), "admin@naroom.io", "관리자", Set.of(AdminRole.CONTENT_EDITOR)));
		return previewSessionService.issue(adminUser, selectedContentVersions, scenarioKey);
	}

}
