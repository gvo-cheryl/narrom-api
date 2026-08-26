package com.naroom.api.admin.audit;

import com.naroom.api.admin.audit.dto.AdminAuditLogResponse;
import com.naroom.api.admin.domain.entity.AdminAuditOutcome;
import com.naroom.api.global.response.ApiResponse;
import com.naroom.api.global.response.CursorPageResponse;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/admin/audit-logs")
@PreAuthorize("hasAnyRole('SUPER_ADMIN', 'SUPPORT_READ_ONLY')")
public class AdminAuditLogController {

	private final AdminAuditLogService adminAuditLogService;

	public AdminAuditLogController(AdminAuditLogService adminAuditLogService) {
		this.adminAuditLogService = adminAuditLogService;
	}

	@GetMapping
	public ApiResponse<CursorPageResponse<AdminAuditLogResponse>> list(
			@RequestParam(required = false) UUID actorAdminId,
			@RequestParam(required = false) String action,
			@RequestParam(required = false) String resourceType,
			@RequestParam(required = false) AdminAuditOutcome outcome,
			@RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant from,
			@RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant to,
			@RequestParam(required = false) String cursor,
			@RequestParam(required = false) Integer size) {
		return ApiResponse.of(
				adminAuditLogService.list(actorAdminId, action, resourceType, outcome, from, to, cursor, size));
	}

}
