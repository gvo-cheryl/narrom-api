package com.naroom.api.checkin;

import com.naroom.api.account.domain.repository.AuthSessionRepository;
import com.naroom.api.auth.security.JwtTokenProvider;
import com.naroom.api.auth.security.MemberAuthentication;
import com.naroom.api.checkin.domain.error.CheckInErrorCode;
import com.naroom.api.checkin.dto.CheckInResponse;
import com.naroom.api.global.config.SecurityConfig;
import com.naroom.api.global.error.exception.BusinessException;
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

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(CheckInController.class)
@Import({
		ProblemDetailFactory.class,
		SecurityConfig.class,
		ApiAuthenticationEntryPoint.class,
		ApiAccessDeniedHandler.class,
		SecurityProblemWriter.class
})
class CheckInControllerTest {

	@Autowired
	private MockMvc mockMvc;

	@MockitoBean
	private CheckInService checkInService;

	@MockitoBean
	private JwtTokenProvider jwtTokenProvider;

	@MockitoBean
	private AuthSessionRepository authSessionRepository;

	@Test
	void getTodayCheckIn_withoutAuthentication_returnsAuthRequired() throws Exception {
		mockMvc.perform(get("/api/v1/checkin/today"))
				.andExpect(status().isUnauthorized())
				.andExpect(jsonPath("$.code").value("AUTH_REQUIRED"));
	}

	@Test
	void getTodayCheckIn_noCheckInYet_returnsNullData() throws Exception {
		when(checkInService.getTodayCheckIn(any())).thenReturn(Optional.empty());

		mockMvc.perform(get("/api/v1/checkin/today").with(authentication(memberAuthentication())))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.data").doesNotExist());
	}

	@Test
	void getTodayCheckIn_existing_returnsCheckIn() throws Exception {
		UUID checkInId = UUID.randomUUID();
		when(checkInService.getTodayCheckIn(any())).thenReturn(Optional.of(sampleCheckIn(checkInId)));

		mockMvc.perform(get("/api/v1/checkin/today").with(authentication(memberAuthentication())))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.data.id").value(checkInId.toString()));
	}

	@Test
	void getCheckInByDate_authenticated_returnsCheckIn() throws Exception {
		UUID checkInId = UUID.randomUUID();
		when(checkInService.getCheckIn(any(), any())).thenReturn(Optional.of(sampleCheckIn(checkInId)));

		mockMvc.perform(get("/api/v1/checkin?date=2026-07-25").with(authentication(memberAuthentication())))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.data.id").value(checkInId.toString()));
	}

	@Test
	void upsertCheckIn_nonEmotionTag_returnsProblemDetail() throws Exception {
		when(checkInService.upsertCheckIn(any(), any()))
				.thenThrow(new BusinessException(CheckInErrorCode.CHECK_IN_EMOTION_TAG_INVALID));

		mockMvc.perform(put("/api/v1/checkin")
						.with(authentication(memberAuthentication()))
						.contentType("application/json")
						.content("""
								{ "checkInDate": "2026-07-25", "emotionIntensity": 3, "energyLevel": 3 }
								"""))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.code").value("CHECKIN_EMOTION_TAG_INVALID"));
	}

	@Test
	void upsertCheckIn_validRequest_returnsUpsertedCheckIn() throws Exception {
		UUID checkInId = UUID.randomUUID();
		when(checkInService.upsertCheckIn(any(), any())).thenReturn(sampleCheckIn(checkInId));

		mockMvc.perform(put("/api/v1/checkin")
						.with(authentication(memberAuthentication()))
						.contentType("application/json")
						.content("""
								{ "checkInDate": "2026-07-25", "emotionIntensity": 3, "energyLevel": 3 }
								"""))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.data.id").value(checkInId.toString()));
	}

	private CheckInResponse sampleCheckIn(UUID checkInId) {
		return new CheckInResponse(
				checkInId, UUID.randomUUID(), LocalDate.of(2026, 7, 25), (short) 3, (short) 3,
				null, null, null, null, List.of(), null, null, 0L);
	}

	private MemberAuthentication memberAuthentication() {
		return new MemberAuthentication(UUID.randomUUID(), UUID.randomUUID());
	}

}
