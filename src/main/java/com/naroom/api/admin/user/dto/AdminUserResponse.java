package com.naroom.api.admin.user.dto;

import com.naroom.api.admin.domain.entity.AdminRole;
import com.naroom.api.admin.domain.entity.AdminStatus;
import com.naroom.api.admin.domain.entity.AdminUser;

import java.time.Instant;
import java.util.Set;
import java.util.UUID;

public record AdminUserResponse(
		UUID id,
		String email,
		String displayName,
		Set<AdminRole> roles,
		AdminStatus status,
		Instant lastLoginAt,
		Instant createdAt) {

	public static AdminUserResponse from(AdminUser adminUser) {
		return new AdminUserResponse(
				adminUser.getId(),
				adminUser.getEmail(),
				adminUser.getDisplayName(),
				adminUser.getRoles(),
				adminUser.getStatus(),
				adminUser.getLastLoginAt(),
				adminUser.getCreatedAt());
	}

}
