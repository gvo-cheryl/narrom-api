package com.naroom.api.admin.user;

import com.naroom.api.admin.auth.AdminAuthentication;
import com.naroom.api.admin.user.dto.AdminInvitationCreateRequest;
import com.naroom.api.admin.user.dto.AdminInvitationResponse;
import com.naroom.api.admin.user.dto.AdminUserResponse;
import com.naroom.api.global.response.ApiResponse;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/admin/admin-users")
@PreAuthorize("hasRole('SUPER_ADMIN')")
public class AdminUserManagementController {

	private final AdminUserManagementService adminUserManagementService;

	public AdminUserManagementController(AdminUserManagementService adminUserManagementService) {
		this.adminUserManagementService = adminUserManagementService;
	}

	@GetMapping
	public ApiResponse<List<AdminUserResponse>> listAdminUsers() {
		return ApiResponse.of(adminUserManagementService.listAdminUsers());
	}

	@GetMapping("/invitations")
	public ApiResponse<List<AdminInvitationResponse>> listInvitations() {
		return ApiResponse.of(adminUserManagementService.listInvitations());
	}

	@PostMapping("/invitations")
	@ResponseStatus(HttpStatus.CREATED)
	public ApiResponse<AdminInvitationResponse> createInvitation(@Valid @RequestBody AdminInvitationCreateRequest request) {
		return ApiResponse.of(adminUserManagementService.createInvitation(request, currentAdminId()));
	}

	@PostMapping("/invitations/{id}/revoke")
	public ApiResponse<AdminInvitationResponse> revokeInvitation(@PathVariable UUID id) {
		return ApiResponse.of(adminUserManagementService.revokeInvitation(id));
	}

	private UUID currentAdminId() {
		AdminAuthentication authentication = (AdminAuthentication) SecurityContextHolder.getContext().getAuthentication();
		return authentication.getAdminUserId();
	}

}
