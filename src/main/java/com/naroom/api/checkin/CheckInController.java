package com.naroom.api.checkin;

import com.naroom.api.auth.security.MemberAuthentication;
import com.naroom.api.checkin.dto.CheckInResponse;
import com.naroom.api.checkin.dto.CheckInUpsertRequest;
import com.naroom.api.global.response.ApiResponse;
import jakarta.validation.Valid;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/checkin")
public class CheckInController {

	private final CheckInService checkInService;

	public CheckInController(CheckInService checkInService) {
		this.checkInService = checkInService;
	}

	@GetMapping("/today")
	public ApiResponse<CheckInResponse> getTodayCheckIn() {
		return ApiResponse.of(checkInService.getTodayCheckIn(currentMemberId()).orElse(null));
	}

	@GetMapping
	public ApiResponse<CheckInResponse> getCheckIn(
			@RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
		return ApiResponse.of(checkInService.getCheckIn(currentMemberId(), date).orElse(null));
	}

	@PutMapping
	public ApiResponse<CheckInResponse> upsertCheckIn(@Valid @RequestBody CheckInUpsertRequest request) {
		return ApiResponse.of(checkInService.upsertCheckIn(currentMemberId(), request));
	}

	// JwtAuthenticationFilter가 SecurityContextHolder에 직접 채워 넣는 방식이라 여기서도 직접 꺼낸다
	// (다른 Controller와 동일한 이유).
	private UUID currentMemberId() {
		MemberAuthentication authentication =
				(MemberAuthentication) SecurityContextHolder.getContext().getAuthentication();
		return authentication.getMemberId();
	}

}
