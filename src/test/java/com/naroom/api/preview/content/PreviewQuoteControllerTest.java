package com.naroom.api.preview.content;

import com.naroom.api.admin.domain.entity.AdminRole;
import com.naroom.api.admin.domain.entity.AdminUser;
import com.naroom.api.admin.domain.repository.AdminUserRepository;
import com.naroom.api.admin.preview.IssuedPreviewSession;
import com.naroom.api.admin.preview.PreviewSessionService;
import com.naroom.api.content.domain.entity.Quote;
import com.naroom.api.content.domain.repository.QuoteRepository;
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
class PreviewQuoteControllerTest {

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private AdminUserRepository adminUserRepository;

	@Autowired
	private PreviewSessionService previewSessionService;

	@Autowired
	private QuoteRepository quoteRepository;

	@Test
	void getTodayQuote_withSelectedDraftQuote_returnsThatQuote() throws Exception {
		Quote draftQuote = quoteRepository.save(Quote.create("미리보기 문장", "작자 미상", null, null));
		IssuedPreviewSession issued = issueSession(Map.of("quote", draftQuote.getId()));

		mockMvc.perform(get("/api/v1/preview/content/quotes/today")
						.header(PreviewTokenAuthenticationFilter.TOKEN_HEADER, issued.rawToken()))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.data.id").value(draftQuote.getId().toString()))
				.andExpect(jsonPath("$.data.text").value("미리보기 문장"))
				.andExpect(jsonPath("$.data.saved").value(false));
	}

	@Test
	void getTodayQuote_withoutSelectedQuote_returnsPreviewContentNotSelected() throws Exception {
		IssuedPreviewSession issued = issueSession(Map.of());

		mockMvc.perform(get("/api/v1/preview/content/quotes/today")
						.header(PreviewTokenAuthenticationFilter.TOKEN_HEADER, issued.rawToken()))
				.andExpect(status().isNotFound())
				.andExpect(jsonPath("$.code").value("PREVIEW_CONTENT_NOT_SELECTED"));
	}

	@Test
	void getTodayQuote_withoutToken_returnsAuthenticationFailed() throws Exception {
		mockMvc.perform(get("/api/v1/preview/content/quotes/today"))
				.andExpect(status().isUnauthorized())
				.andExpect(jsonPath("$.code").value("PREVIEW_AUTHENTICATION_FAILED"));
	}

	private IssuedPreviewSession issueSession(Map<String, UUID> selectedContentVersions) {
		AdminUser adminUser = adminUserRepository.save(
				AdminUser.bootstrap("google-sub-" + System.nanoTime(), "admin@naroom.io", "관리자", Set.of(AdminRole.CONTENT_EDITOR)));
		return previewSessionService.issue(adminUser, selectedContentVersions, null);
	}

}
