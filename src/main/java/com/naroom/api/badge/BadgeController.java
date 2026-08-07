package com.naroom.api.badge;

import com.naroom.api.auth.security.MemberAuthentication;
import com.naroom.api.badge.dto.MemberBadgeResponse;
import com.naroom.api.global.response.ApiResponse;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/badges")
public class BadgeController {

	private final BadgeQueryService badgeQueryService;

	public BadgeController(BadgeQueryService badgeQueryService) {
		this.badgeQueryService = badgeQueryService;
	}

	@GetMapping
	public ApiResponse<List<MemberBadgeResponse>> getEarnedBadges() {
		return ApiResponse.of(badgeQueryService.listEarned(currentMemberId()));
	}

	// JwtAuthenticationFilter가 SecurityContextHolder에 직접 채워 넣는 방식이라 여기서도 직접 꺼낸다
	// (ExperimentController 등과 동일한 이유).
	private UUID currentMemberId() {
		MemberAuthentication authentication =
				(MemberAuthentication) SecurityContextHolder.getContext().getAuthentication();
		return authentication.getMemberId();
	}

}
