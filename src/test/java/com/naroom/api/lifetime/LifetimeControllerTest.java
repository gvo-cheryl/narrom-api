package com.naroom.api.lifetime;

import com.naroom.api.account.domain.repository.AuthSessionRepository;
import com.naroom.api.auth.security.JwtTokenProvider;
import com.naroom.api.auth.security.MemberAuthentication;
import com.naroom.api.global.config.SecurityConfig;
import com.naroom.api.global.error.exception.BusinessException;
import com.naroom.api.global.error.response.ProblemDetailFactory;
import com.naroom.api.global.security.ApiAccessDeniedHandler;
import com.naroom.api.global.security.ApiAuthenticationEntryPoint;
import com.naroom.api.global.security.SecurityProblemWriter;
import com.naroom.api.lifetime.dto.CalendarDayResponse;
import com.naroom.api.record.domain.error.RecordErrorCode;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(LifetimeController.class)
@Import({
		ProblemDetailFactory.class,
		SecurityConfig.class,
		ApiAuthenticationEntryPoint.class,
		ApiAccessDeniedHandler.class,
		SecurityProblemWriter.class
})
class LifetimeControllerTest {

	@Autowired
	private MockMvc mockMvc;

	@MockitoBean
	private EntryTimelineService entryTimelineService;

	@MockitoBean
	private CalendarService calendarService;

	@MockitoBean
	private JwtTokenProvider jwtTokenProvider;

	@MockitoBean
	private AuthSessionRepository authSessionRepository;

	@Test
	void getTimeline_withoutAuthentication_returnsAuthRequired() throws Exception {
		mockMvc.perform(get("/api/v1/lifetime/timeline"))
				.andExpect(status().isUnauthorized())
				.andExpect(jsonPath("$.code").value("AUTH_REQUIRED"));
	}

	@Test
	void getTimeline_authenticated_returnsList() throws Exception {
		when(entryTimelineService.getTimeline(any(), any(), any(), any())).thenReturn(List.of());

		mockMvc.perform(get("/api/v1/lifetime/timeline").with(authentication(memberAuthentication())))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.data").isArray());
	}

	@Test
	void getCalendar_authenticated_returnsDays() throws Exception {
		when(calendarService.getMonth(any(), anyInt(), anyInt()))
				.thenReturn(List.of(new CalendarDayResponse(LocalDate.of(2026, 7, 1), true, false)));

		mockMvc.perform(get("/api/v1/lifetime/calendar?year=2026&month=7").with(authentication(memberAuthentication())))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.data[0].hasEntry").value(true));
	}

	@Test
	void getCalendar_invalidMonth_returnsProblemDetail() throws Exception {
		when(calendarService.getMonth(any(), anyInt(), anyInt()))
				.thenThrow(new BusinessException(RecordErrorCode.CALENDAR_MONTH_INVALID));

		mockMvc.perform(get("/api/v1/lifetime/calendar?year=2026&month=13").with(authentication(memberAuthentication())))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.code").value("RECORD_CALENDAR_MONTH_INVALID"));
	}

	private MemberAuthentication memberAuthentication() {
		return new MemberAuthentication(java.util.UUID.randomUUID(), java.util.UUID.randomUUID());
	}

}
