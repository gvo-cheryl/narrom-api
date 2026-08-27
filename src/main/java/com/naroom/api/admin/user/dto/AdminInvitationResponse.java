package com.naroom.api.admin.user.dto;

import com.naroom.api.admin.domain.entity.AdminInvitation;
import com.naroom.api.admin.domain.entity.AdminRole;

import java.time.Instant;
import java.util.Set;
import java.util.UUID;

public record AdminInvitationResponse(
		UUID id,
		String email,
		Set<AdminRole> roles,
		String status,
		UUID invitedByAdminId,
		Instant invitedAt,
		Instant consumedAt,
		UUID consumedAdminUserId,
		Instant revokedAt) {

	public static AdminInvitationResponse from(AdminInvitation invitation) {
		String status = invitation.getRevokedAt() != null
				? "REVOKED"
				: invitation.getConsumedAt() != null ? "CONSUMED" : "PENDING";
		return new AdminInvitationResponse(
				invitation.getId(),
				invitation.getEmail(),
				invitation.getRoles(),
				status,
				invitation.getInvitedByAdminId(),
				invitation.getInvitedAt(),
				invitation.getConsumedAt(),
				invitation.getConsumedAdminUserId(),
				invitation.getRevokedAt());
	}

}
