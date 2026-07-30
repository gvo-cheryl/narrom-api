package com.naroom.api.lifetime;

import com.naroom.api.ai.domain.entity.AiFeatureType;
import com.naroom.api.auth.security.MemberAuthentication;
import com.naroom.api.global.response.ApiResponse;
import com.naroom.api.lifetime.dto.CalendarDayResponse;
import com.naroom.api.lifetime.dto.EmotionEnergyPointResponse;
import com.naroom.api.lifetime.dto.EntryTimelineResponse;
import com.naroom.api.lifetime.dto.PeriodReflectionCreateRequest;
import com.naroom.api.lifetime.dto.PeriodReflectionResponse;
import com.naroom.api.lifetime.dto.PersonalSummaryResponse;
import com.naroom.api.lifetime.dto.PersonalSummaryUpdateRequest;
import com.naroom.api.lifetime.dto.TagDistributionResponse;
import com.naroom.api.record.domain.entity.EntryType;
import com.naroom.api.record.domain.entity.TagCategory;
import jakarta.validation.Valid;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/lifetime")
public class LifetimeController {

	private final EntryTimelineService entryTimelineService;
	private final CalendarService calendarService;
	private final PeriodReflectionService periodReflectionService;
	private final PersonalSummaryService personalSummaryService;
	private final EmotionEnergyService emotionEnergyService;
	private final TagExplorationService tagExplorationService;

	public LifetimeController(
			EntryTimelineService entryTimelineService,
			CalendarService calendarService,
			PeriodReflectionService periodReflectionService,
			PersonalSummaryService personalSummaryService,
			EmotionEnergyService emotionEnergyService,
			TagExplorationService tagExplorationService) {
		this.entryTimelineService = entryTimelineService;
		this.calendarService = calendarService;
		this.periodReflectionService = periodReflectionService;
		this.personalSummaryService = personalSummaryService;
		this.emotionEnergyService = emotionEnergyService;
		this.tagExplorationService = tagExplorationService;
	}

	@GetMapping("/timeline")
	public ApiResponse<List<EntryTimelineResponse>> getTimeline(
			@RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
			@RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
			@RequestParam(required = false) EntryType entryType) {
		return ApiResponse.of(entryTimelineService.getTimeline(currentMemberId(), from, to, entryType));
	}

	@GetMapping("/calendar")
	public ApiResponse<List<CalendarDayResponse>> getCalendar(
			@RequestParam int year, @RequestParam int month) {
		return ApiResponse.of(calendarService.getMonth(currentMemberId(), year, month));
	}

	@PostMapping("/period-reflections")
	public ApiResponse<PeriodReflectionResponse> createPeriodReflection(@Valid @RequestBody PeriodReflectionCreateRequest request) {
		return ApiResponse.of(
				PeriodReflectionResponse.from(periodReflectionService.generate(currentMemberId(), request.featureType())));
	}

	@GetMapping("/period-reflections/{periodReflectionId}")
	public ApiResponse<PeriodReflectionResponse> getPeriodReflection(@PathVariable UUID periodReflectionId) {
		return ApiResponse.of(
				PeriodReflectionResponse.from(periodReflectionService.getOwnedOrThrow(currentMemberId(), periodReflectionId)));
	}

	@GetMapping("/period-reflections")
	public ApiResponse<List<PeriodReflectionResponse>> getPeriodReflections(
			@RequestParam(required = false) AiFeatureType featureType) {
		return ApiResponse.of(periodReflectionService.listRecent(currentMemberId(), featureType).stream()
				.map(PeriodReflectionResponse::from)
				.toList());
	}

	@GetMapping("/personal-summaries/current")
	public ApiResponse<PersonalSummaryResponse> getCurrentPersonalSummary() {
		return ApiResponse.of(personalSummaryService.getCurrent(currentMemberId()).orElse(null));
	}

	@PutMapping("/personal-summaries/current")
	public ApiResponse<PersonalSummaryResponse> updateCurrentPersonalSummary(@Valid @RequestBody PersonalSummaryUpdateRequest request) {
		return ApiResponse.of(personalSummaryService.updateCurrent(currentMemberId(), request.content()));
	}

	@GetMapping("/personal-summaries")
	public ApiResponse<List<PersonalSummaryResponse>> getPersonalSummaryHistory() {
		return ApiResponse.of(personalSummaryService.getHistory(currentMemberId()));
	}

	@GetMapping("/analytics/emotion-energy")
	public ApiResponse<List<EmotionEnergyPointResponse>> getEmotionEnergyTrend(@RequestParam int range) {
		return ApiResponse.of(emotionEnergyService.getTrend(currentMemberId(), range));
	}

	@GetMapping("/analytics/tags")
	public ApiResponse<List<TagDistributionResponse>> getTagDistribution(
			@RequestParam(required = false) TagCategory category,
			@RequestParam(required = false) Integer range) {
		return ApiResponse.of(tagExplorationService.getDistribution(currentMemberId(), category, range));
	}

	@GetMapping("/analytics/tags/{tagId}/entries")
	public ApiResponse<List<EntryTimelineResponse>> getEntriesByTag(@PathVariable UUID tagId) {
		return ApiResponse.of(entryTimelineService.getByTag(currentMemberId(), tagId));
	}

	// JwtAuthenticationFilter가 SecurityContextHolder에 직접 채워 넣는 방식이라 여기서도 직접 꺼낸다
	// (AccountController/ContentController/RecordController/AiController와 동일한 이유).
	private UUID currentMemberId() {
		MemberAuthentication authentication =
				(MemberAuthentication) SecurityContextHolder.getContext().getAuthentication();
		return authentication.getMemberId();
	}

}
