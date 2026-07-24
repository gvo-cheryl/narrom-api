package com.naroom.api.content;

import com.naroom.api.account.domain.repository.AuthSessionRepository;
import com.naroom.api.auth.security.JwtTokenProvider;
import com.naroom.api.auth.security.MemberAuthentication;
import com.naroom.api.content.dto.QuoteResponse;
import com.naroom.api.content.dto.QuoteTopicResponse;
import com.naroom.api.global.config.SecurityConfig;
import com.naroom.api.global.error.exception.BusinessException;
import com.naroom.api.global.error.response.ProblemDetailFactory;
import com.naroom.api.content.domain.error.ContentErrorCode;
import com.naroom.api.global.security.ApiAccessDeniedHandler;
import com.naroom.api.global.security.ApiAuthenticationEntryPoint;
import com.naroom.api.global.security.SecurityProblemWriter;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(ContentController.class)
@Import({
		ProblemDetailFactory.class,
		SecurityConfig.class,
		ApiAuthenticationEntryPoint.class,
		ApiAccessDeniedHandler.class,
		SecurityProblemWriter.class
})
class ContentControllerTest {

	@Autowired
	private MockMvc mockMvc;

	@MockitoBean
	private QuoteService quoteService;

	@MockitoBean
	private JwtTokenProvider jwtTokenProvider;

	@MockitoBean
	private AuthSessionRepository authSessionRepository;

	@Test
	void getTodayQuote_withoutAuthentication_returnsAuthRequired() throws Exception {
		mockMvc.perform(get("/api/v1/content/quotes/today"))
				.andExpect(status().isUnauthorized())
				.andExpect(jsonPath("$.code").value("AUTH_REQUIRED"));
	}

	@Test
	void getTodayQuote_authenticated_returnsQuote() throws Exception {
		UUID quoteId = UUID.randomUUID();
		when(quoteService.getTodayQuote(any())).thenReturn(sampleQuote(quoteId));

		mockMvc.perform(get("/api/v1/content/quotes/today").with(authentication(memberAuthentication())))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.data.id").value(quoteId.toString()));
	}

	@Test
	void getQuote_notFound_returnsProblemDetail() throws Exception {
		when(quoteService.getQuote(any(), any())).thenThrow(new BusinessException(ContentErrorCode.QUOTE_NOT_FOUND));

		mockMvc.perform(get("/api/v1/content/quotes/" + UUID.randomUUID()).with(authentication(memberAuthentication())))
				.andExpect(status().isNotFound())
				.andExpect(jsonPath("$.code").value("CONTENT_QUOTE_NOT_FOUND"));
	}

	@Test
	void getTopics_authenticated_returnsTopics() throws Exception {
		when(quoteService.getActiveTopics()).thenReturn(List.of(new QuoteTopicResponse(UUID.randomUUID(), "A", "주제")));

		mockMvc.perform(get("/api/v1/content/topics").with(authentication(memberAuthentication())))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.data[0].code").value("A"));
	}

	@Test
	void getQuotesByTopic_topicNotFound_returnsProblemDetail() throws Exception {
		when(quoteService.getQuotesByTopic(any(), any()))
				.thenThrow(new BusinessException(ContentErrorCode.QUOTE_TOPIC_NOT_FOUND));

		mockMvc.perform(get("/api/v1/content/topics/" + UUID.randomUUID() + "/quotes")
						.with(authentication(memberAuthentication())))
				.andExpect(status().isNotFound())
				.andExpect(jsonPath("$.code").value("CONTENT_QUOTE_TOPIC_NOT_FOUND"));
	}

	@Test
	void saveQuote_authenticated_returnsNoContent() throws Exception {
		mockMvc.perform(post("/api/v1/content/quotes/" + UUID.randomUUID() + "/save")
						.with(authentication(memberAuthentication())))
				.andExpect(status().isNoContent());
	}

	@Test
	void unsaveQuote_authenticated_returnsNoContent() throws Exception {
		mockMvc.perform(delete("/api/v1/content/quotes/" + UUID.randomUUID() + "/save")
						.with(authentication(memberAuthentication())))
				.andExpect(status().isNoContent());
	}

	@Test
	void getSavedQuotes_withoutAuthentication_returnsAuthRequired() throws Exception {
		mockMvc.perform(get("/api/v1/content/quotes/saved"))
				.andExpect(status().isUnauthorized())
				.andExpect(jsonPath("$.code").value("AUTH_REQUIRED"));
	}

	private QuoteResponse sampleQuote(UUID quoteId) {
		return new QuoteResponse(quoteId, "문장", "작가", "출처", "https://example.com", List.of(), false);
	}

	private MemberAuthentication memberAuthentication() {
		return new MemberAuthentication(UUID.randomUUID(), UUID.randomUUID());
	}

}
