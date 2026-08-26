package com.naroom.api.admin.audit.dto;

import com.naroom.api.admin.domain.entity.AdminAuditLog;
import com.naroom.api.admin.domain.entity.AdminAuditOutcome;

import java.time.Instant;
import java.util.UUID;

public record AdminAuditLogResponse(
		UUID id,
		UUID actorAdminId,
		String actorDisplayName,
		String action,
		String resourceType,
		String resourceId,
		String changeReason,
		String traceId,
		String requestMethod,
		String requestPath,
		AdminAuditOutcome outcome,
		Instant createdAt) {

	public static AdminAuditLogResponse of(AdminAuditLog log, String actorDisplayName) {
		return new AdminAuditLogResponse(
				log.getId(),
				log.getActorAdminId(),
				actorDisplayName,
				log.getAction(),
				log.getResourceType(),
				log.getResourceId(),
				log.getChangeReason(),
				log.getTraceId(),
				log.getRequestMethod(),
				log.getRequestPath(),
				log.getOutcome(),
				log.getCreatedAt());
	}

}
