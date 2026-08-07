package com.naroom.api.badge;

import com.naroom.api.account.domain.repository.AuthSessionRepository;
import com.naroom.api.auth.security.JwtTokenProvider;
import com.naroom.api.auth.security.MemberAuthentication;
import com.naroom.api.badge.domain.entity.BadgeCategory;
import com.naroom.api.badge.domain.entity.BadgeCode;
import com.naroom.api.badge.dto.MemberBadgeResponse;
import com.naroom.api.global.config.SecurityConfig;
import com.naroom.api.global.error.response.ProblemDetailFactory;
import com.naroom.api.global.security.ApiAccessDeniedHandler;
import com.naroom.api.global.security.ApiAuthenticationEntryPoint;
import com.naroom.api.global.security.SecurityProblemWriter;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(BadgeController.class)
@Import({
		ProblemDetailFactory.class,
		SecurityConfig.class,
		ApiAuthenticationEntryPoint.class,
		ApiAccessDeniedHandler.class,
		SecurityProblemWriter.class
})
class BadgeControllerTest {

	@Autowired
	private MockMvc mockMvc;

	@MockitoBean
	private BadgeQueryService badgeQueryService;

	@MockitoBean
	private JwtTokenProvider jwtTokenProvider;

	@MockitoBean
	private AuthSessionRepository authSessionRepository;

	@Test
	void getEarnedBadges_withoutAuthentication_returnsAuthRequired() throws Exception {
		mockMvc.perform(get("/api/v1/badges"))
				.andExpect(status().isUnauthorized())
				.andExpect(jsonPath("$.code").value("AUTH_REQUIRED"));
	}

	@Test
	void getEarnedBadges_authenticated_returnsList() throws Exception {
		when(badgeQueryService.listEarned(any())).thenReturn(List.of(new MemberBadgeResponse(
				UUID.randomUUID(), BadgeCode.FIRST_ENTRY, BadgeCategory.TRIAL, "첫 기록",
				"나로움에 처음으로 마음을 기록했어요.", Instant.now())));

		mockMvc.perform(get("/api/v1/badges").with(authentication(memberAuthentication())))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.data[0].code").value("FIRST_ENTRY"))
				.andExpect(jsonPath("$.data[0].category").value("TRIAL"));
	}

	private MemberAuthentication memberAuthentication() {
		return new MemberAuthentication(UUID.randomUUID(), UUID.randomUUID());
	}

}
