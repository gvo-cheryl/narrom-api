package com.naroom.api.lifetime;

import com.naroom.api.auth.security.MemberAuthentication;
import com.naroom.api.global.response.ApiResponse;
import com.naroom.api.lifetime.dto.CalendarDayResponse;
import com.naroom.api.lifetime.dto.EntryTimelineResponse;
import com.naroom.api.record.domain.entity.EntryType;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.GetMapping;
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

	public LifetimeController(EntryTimelineService entryTimelineService, CalendarService calendarService) {
		this.entryTimelineService = entryTimelineService;
		this.calendarService = calendarService;
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

	// JwtAuthenticationFilter가 SecurityContextHolder에 직접 채워 넣는 방식이라 여기서도 직접 꺼낸다
	// (AccountController/ContentController/RecordController/AiController와 동일한 이유).
	private UUID currentMemberId() {
		MemberAuthentication authentication =
				(MemberAuthentication) SecurityContextHolder.getContext().getAuthentication();
		return authentication.getMemberId();
	}

}
