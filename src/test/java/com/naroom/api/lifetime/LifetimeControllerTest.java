package com.naroom.api.lifetime;

import com.naroom.api.account.domain.entity.Member;
import com.naroom.api.account.domain.repository.AuthSessionRepository;
import com.naroom.api.ai.domain.entity.AiFeatureType;
import com.naroom.api.auth.security.JwtTokenProvider;
import com.naroom.api.auth.security.MemberAuthentication;
import com.naroom.api.global.config.SecurityConfig;
import com.naroom.api.global.error.exception.BusinessException;
import com.naroom.api.global.error.response.ProblemDetailFactory;
import com.naroom.api.global.security.ApiAccessDeniedHandler;
import com.naroom.api.global.security.ApiAuthenticationEntryPoint;
import com.naroom.api.global.security.SecurityProblemWriter;
import com.naroom.api.lifetime.dto.CalendarDayResponse;
import com.naroom.api.lifetime.domain.entity.PeriodReflection;
import com.naroom.api.lifetime.domain.error.LifetimeErrorCode;
import com.naroom.api.lifetime.dto.EmotionEnergyPointResponse;
import com.naroom.api.lifetime.dto.PersonalSummaryResponse;
import com.naroom.api.lifetime.dto.TagDistributionResponse;
import com.naroom.api.record.domain.entity.Entry;
import com.naroom.api.record.domain.entity.EntryType;
import com.naroom.api.record.domain.entity.TagCategory;
import com.naroom.api.record.domain.error.RecordErrorCode;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
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
	private PeriodReflectionService periodReflectionService;

	@MockitoBean
	private PersonalSummaryService personalSummaryService;

	@MockitoBean
	private EmotionEnergyService emotionEnergyService;

	@MockitoBean
	private TagExplorationService tagExplorationService;

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

	@Test
	void createPeriodReflection_withoutAuthentication_returnsAuthRequired() throws Exception {
		mockMvc.perform(post("/api/v1/lifetime/period-reflections")
						.contentType("application/json")
						.content("""
								{ "featureType": "WEEKLY_REFLECTION" }
								"""))
				.andExpect(status().isUnauthorized())
				.andExpect(jsonPath("$.code").value("AUTH_REQUIRED"));
	}

	@Test
	void createPeriodReflection_validRequest_returnsReflection() throws Exception {
		when(periodReflectionService.generate(any(), any())).thenReturn(samplePendingReflection(AiFeatureType.WEEKLY_REFLECTION));

		mockMvc.perform(post("/api/v1/lifetime/period-reflections")
						.with(authentication(memberAuthentication()))
						.contentType("application/json")
						.content("""
								{ "featureType": "WEEKLY_REFLECTION" }
								"""))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.data.featureType").value("WEEKLY_REFLECTION"))
				.andExpect(jsonPath("$.data.status").value("PENDING"));
	}

	@Test
	void createPeriodReflection_missingFeatureType_returnsValidationFailed() throws Exception {
		mockMvc.perform(post("/api/v1/lifetime/period-reflections")
						.with(authentication(memberAuthentication()))
						.contentType("application/json")
						.content("""
								{}
								"""))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.code").value("COMMON_VALIDATION_FAILED"));
	}

	@Test
	void createPeriodReflection_insufficientRecords_returnsProblemDetail() throws Exception {
		when(periodReflectionService.generate(any(), any()))
				.thenThrow(new BusinessException(LifetimeErrorCode.PERIOD_REFLECTION_INSUFFICIENT_RECORDS));

		mockMvc.perform(post("/api/v1/lifetime/period-reflections")
						.with(authentication(memberAuthentication()))
						.contentType("application/json")
						.content("""
								{ "featureType": "WEEKLY_REFLECTION" }
								"""))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.code").value("LIFETIME_PERIOD_REFLECTION_INSUFFICIENT_RECORDS"));
	}

	@Test
	void getPeriodReflection_authenticated_returnsReflection() throws Exception {
		when(periodReflectionService.getOwnedOrThrow(any(), any()))
				.thenReturn(samplePendingReflection(AiFeatureType.THREE_DAY_REFLECTION));

		mockMvc.perform(get("/api/v1/lifetime/period-reflections/" + UUID.randomUUID())
						.with(authentication(memberAuthentication())))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.data.featureType").value("THREE_DAY_REFLECTION"));
	}

	@Test
	void getPeriodReflection_notFound_returnsProblemDetail() throws Exception {
		when(periodReflectionService.getOwnedOrThrow(any(), any()))
				.thenThrow(new BusinessException(LifetimeErrorCode.PERIOD_REFLECTION_NOT_FOUND));

		mockMvc.perform(get("/api/v1/lifetime/period-reflections/" + UUID.randomUUID())
						.with(authentication(memberAuthentication())))
				.andExpect(status().isNotFound())
				.andExpect(jsonPath("$.code").value("LIFETIME_PERIOD_REFLECTION_NOT_FOUND"));
	}

	@Test
	void getCurrentPersonalSummary_noneYet_returnsNullData() throws Exception {
		when(personalSummaryService.getCurrent(any())).thenReturn(Optional.empty());

		mockMvc.perform(get("/api/v1/lifetime/personal-summaries/current").with(authentication(memberAuthentication())))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.data").doesNotExist());
	}

	@Test
	void getCurrentPersonalSummary_existing_returnsSummary() throws Exception {
		when(personalSummaryService.getCurrent(any())).thenReturn(Optional.of(sampleSummary("요즘의 나", false)));

		mockMvc.perform(get("/api/v1/lifetime/personal-summaries/current").with(authentication(memberAuthentication())))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.data.content").value("요즘의 나"));
	}

	@Test
	void updateCurrentPersonalSummary_validRequest_returnsUpdatedSummary() throws Exception {
		when(personalSummaryService.updateCurrent(any(), any())).thenReturn(sampleSummary("새로 쓴 정리", false));

		mockMvc.perform(put("/api/v1/lifetime/personal-summaries/current")
						.with(authentication(memberAuthentication()))
						.contentType("application/json")
						.content("""
								{ "content": "새로 쓴 정리" }
								"""))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.data.content").value("새로 쓴 정리"));
	}

	@Test
	void updateCurrentPersonalSummary_contentExceedsLimit_returnsValidationFailed() throws Exception {
		String tooLong = "가".repeat(1001);

		mockMvc.perform(put("/api/v1/lifetime/personal-summaries/current")
						.with(authentication(memberAuthentication()))
						.contentType("application/json")
						.content("""
								{ "content": "%s" }
								""".formatted(tooLong)))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.code").value("COMMON_VALIDATION_FAILED"));
	}

	@Test
	void getPersonalSummaryHistory_authenticated_returnsList() throws Exception {
		when(personalSummaryService.getHistory(any()))
				.thenReturn(List.of(sampleSummary("이전 정리", true), sampleSummary("지금 정리", false)));

		mockMvc.perform(get("/api/v1/lifetime/personal-summaries").with(authentication(memberAuthentication())))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.data.length()").value(2));
	}

	@Test
	void getEmotionEnergyTrend_authenticated_returnsPoints() throws Exception {
		when(emotionEnergyService.getTrend(any(), anyInt()))
				.thenReturn(List.of(new EmotionEnergyPointResponse(LocalDate.of(2026, 7, 20), (short) 3, (short) 4)));

		mockMvc.perform(get("/api/v1/lifetime/analytics/emotion-energy?range=7").with(authentication(memberAuthentication())))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.data[0].emotionIntensity").value(3));
	}

	@Test
	void getEmotionEnergyTrend_invalidRange_returnsProblemDetail() throws Exception {
		when(emotionEnergyService.getTrend(any(), anyInt()))
				.thenThrow(new BusinessException(LifetimeErrorCode.ANALYTICS_RANGE_INVALID));

		mockMvc.perform(get("/api/v1/lifetime/analytics/emotion-energy?range=10").with(authentication(memberAuthentication())))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.code").value("LIFETIME_ANALYTICS_RANGE_INVALID"));
	}

	@Test
	void getTagDistribution_authenticated_returnsList() throws Exception {
		when(tagExplorationService.getDistribution(any(), any()))
				.thenReturn(List.of(new TagDistributionResponse(UUID.randomUUID(), "서운함", TagCategory.EMOTION, 3)));

		mockMvc.perform(get("/api/v1/lifetime/analytics/tags").with(authentication(memberAuthentication())))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.data[0].tagName").value("서운함"));
	}

	@Test
	void getEntriesByTag_authenticated_returnsList() throws Exception {
		when(entryTimelineService.getByTag(any(), any())).thenReturn(List.of());

		mockMvc.perform(get("/api/v1/lifetime/analytics/tags/" + UUID.randomUUID() + "/entries")
						.with(authentication(memberAuthentication())))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.data").isArray());
	}

	private PersonalSummaryResponse sampleSummary(String content, boolean archived) {
		Instant now = Instant.now();
		return new PersonalSummaryResponse(
				UUID.randomUUID(), com.naroom.api.lifetime.domain.entity.SummaryScope.CURRENT_SELF,
				content, archived, archived ? now : null, now, now);
	}

	private PeriodReflection samplePendingReflection(AiFeatureType featureType) {
		Member member = Member.create("지연");
		Entry envelope = Entry.create(member, EntryType.WEEKLY_REFLECTION, null, null, LocalDate.now(), null, null, null);
		return PeriodReflection.request(member, envelope, featureType, LocalDate.now().minusDays(6), LocalDate.now());
	}

	private MemberAuthentication memberAuthentication() {
		return new MemberAuthentication(java.util.UUID.randomUUID(), java.util.UUID.randomUUID());
	}

}
