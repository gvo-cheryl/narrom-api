package com.naroom.api.admin.preview;

import com.naroom.api.admin.auth.AdminAuthentication;
import com.naroom.api.admin.audit.AdminAuditLogService;
import com.naroom.api.admin.domain.entity.AdminAuditOutcome;
import com.naroom.api.admin.domain.entity.AdminUser;
import com.naroom.api.admin.domain.repository.AdminUserRepository;
import com.naroom.api.admin.preview.dto.AdminPreviewSessionCreateRequest;
import com.naroom.api.admin.preview.dto.AdminPreviewSessionResponse;
import com.naroom.api.global.response.ApiResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/admin/preview/sessions")
@PreAuthorize("hasAnyRole('SUPER_ADMIN', 'CONTENT_EDITOR')")
public class AdminPreviewSessionController {

	private final PreviewSessionService previewSessionService;
	private final AdminUserRepository adminUserRepository;
	private final AdminAuditLogService auditLogService;

	public AdminPreviewSessionController(
			PreviewSessionService previewSessionService,
			AdminUserRepository adminUserRepository,
			AdminAuditLogService auditLogService) {
		this.previewSessionService = previewSessionService;
		this.adminUserRepository = adminUserRepository;
		this.auditLogService = auditLogService;
	}

	@PostMapping
	@ResponseStatus(HttpStatus.CREATED)
	public ApiResponse<AdminPreviewSessionResponse> create(
			@Valid @RequestBody AdminPreviewSessionCreateRequest request, HttpServletRequest httpRequest) {
		UUID adminId = currentAdminId();
		AdminUser adminUser = adminUserRepository.getReferenceById(adminId);
		IssuedPreviewSession issued = previewSessionService.issue(adminUser, request.selectedContentVersions(), request.scenarioKey());

		// §18.1: preview session 생성은 감사 로그 대상이다.
		auditLogService.record(
				adminId, "PREVIEW_SESSION_CREATE", "PreviewSession", issued.session().getId().toString(),
				null, httpRequest.getHeader("X-Trace-Id"), httpRequest.getMethod(), httpRequest.getRequestURI(),
				AdminAuditOutcome.SUCCESS);

		return ApiResponse.of(AdminPreviewSessionResponse.from(issued));
	}

	private UUID currentAdminId() {
		AdminAuthentication authentication = (AdminAuthentication) SecurityContextHolder.getContext().getAuthentication();
		return authentication.getAdminUserId();
	}

}
