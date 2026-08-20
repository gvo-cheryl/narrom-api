package com.naroom.api.admin.dto;

import com.naroom.api.admin.domain.entity.AdminRole;

import java.time.Instant;
import java.util.Set;
import java.util.UUID;

public record AdminSessionResponse(
		UUID adminId,
		String email,
		String displayName,
		Set<AdminRole> roles,
		Instant expiresAt) {
}
