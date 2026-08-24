package com.naroom.api.admin.ai;

import com.naroom.api.admin.ai.dto.AdminAiRuntimeStatusResponse;
import com.naroom.api.global.response.ApiResponse;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/admin/ai/runtime-status")
@PreAuthorize("hasAnyRole('SUPER_ADMIN', 'AI_OPERATOR', 'SUPPORT_READ_ONLY')")
public class AdminAiRuntimeStatusController {

	private final AdminAiRuntimeStatusService adminAiRuntimeStatusService;

	public AdminAiRuntimeStatusController(AdminAiRuntimeStatusService adminAiRuntimeStatusService) {
		this.adminAiRuntimeStatusService = adminAiRuntimeStatusService;
	}

	@GetMapping
	public ApiResponse<List<AdminAiRuntimeStatusResponse>> list() {
		return ApiResponse.of(adminAiRuntimeStatusService.list());
	}

}
