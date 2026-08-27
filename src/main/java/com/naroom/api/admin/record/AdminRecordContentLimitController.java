package com.naroom.api.admin.record;

import com.naroom.api.admin.auth.AdminAuthentication;
import com.naroom.api.admin.record.dto.AdminRecordContentLimitResponse;
import com.naroom.api.admin.record.dto.AdminRecordContentLimitUpdateRequest;
import com.naroom.api.global.response.ApiResponse;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/admin/record/content-limits")
@PreAuthorize("hasRole('SUPER_ADMIN')")
public class AdminRecordContentLimitController {

	private final AdminRecordContentLimitService adminRecordContentLimitService;

	public AdminRecordContentLimitController(AdminRecordContentLimitService adminRecordContentLimitService) {
		this.adminRecordContentLimitService = adminRecordContentLimitService;
	}

	@GetMapping
	public ApiResponse<AdminRecordContentLimitResponse> get() {
		return ApiResponse.of(adminRecordContentLimitService.get());
	}

	@PutMapping
	public ApiResponse<AdminRecordContentLimitResponse> update(@Valid @RequestBody AdminRecordContentLimitUpdateRequest request) {
		return ApiResponse.of(adminRecordContentLimitService.update(request, currentAdminId()));
	}

	private UUID currentAdminId() {
		AdminAuthentication authentication = (AdminAuthentication) SecurityContextHolder.getContext().getAuthentication();
		return authentication.getAdminUserId();
	}

}
