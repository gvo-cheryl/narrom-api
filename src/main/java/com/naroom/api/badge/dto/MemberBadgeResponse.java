package com.naroom.api.badge.dto;

import com.naroom.api.badge.domain.entity.BadgeCategory;
import com.naroom.api.badge.domain.entity.BadgeCode;
import com.naroom.api.badge.domain.entity.BadgeDefinition;
import com.naroom.api.badge.domain.entity.MemberBadge;

import java.time.Instant;
import java.util.UUID;

public record MemberBadgeResponse(
		UUID badgeDefinitionId,
		BadgeCode code,
		BadgeCategory category,
		String title,
		String description,
		Instant earnedAt) {

	public static MemberBadgeResponse from(MemberBadge memberBadge) {
		BadgeDefinition definition = memberBadge.getBadgeDefinition();
		return new MemberBadgeResponse(
				definition.getId(), definition.getCode(), definition.getCategory(),
				definition.getTitle(), definition.getDescription(), memberBadge.getEarnedAt());
	}

}
