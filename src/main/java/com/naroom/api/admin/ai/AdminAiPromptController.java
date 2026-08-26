package com.naroom.api.admin.ai;

import com.naroom.api.admin.ai.dto.AdminAiPromptCreateRequest;
import com.naroom.api.admin.ai.dto.AdminAiPromptCreateRevisionRequest;
import com.naroom.api.admin.ai.dto.AdminAiPromptResponse;
import com.naroom.api.admin.ai.dto.AdminAiPromptUpdateRequest;
import com.naroom.api.admin.auth.AdminAuthentication;
import com.naroom.api.global.response.ApiResponse;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/admin/ai/prompts")
@PreAuthorize("hasAnyRole('SUPER_ADMIN', 'AI_OPERATOR')")
public class AdminAiPromptController {

	private final AdminAiPromptService adminAiPromptService;

	public AdminAiPromptController(AdminAiPromptService adminAiPromptService) {
		this.adminAiPromptService = adminAiPromptService;
	}

	@GetMapping
	public ApiResponse<List<AdminAiPromptResponse>> list() {
		return ApiResponse.of(adminAiPromptService.list());
	}

	@GetMapping("/{id}")
	public ApiResponse<AdminAiPromptResponse> get(@PathVariable UUID id) {
		return ApiResponse.of(adminAiPromptService.get(id));
	}

	@PostMapping
	@ResponseStatus(HttpStatus.CREATED)
	public ApiResponse<AdminAiPromptResponse> create(@Valid @RequestBody AdminAiPromptCreateRequest request) {
		return ApiResponse.of(adminAiPromptService.create(request, currentAdminId()));
	}

	@PutMapping("/{id}")
	public ApiResponse<AdminAiPromptResponse> update(
			@PathVariable UUID id, @Valid @RequestBody AdminAiPromptUpdateRequest request) {
		return ApiResponse.of(adminAiPromptService.update(id, request));
	}

	@PostMapping("/{id}/revisions")
	@ResponseStatus(HttpStatus.CREATED)
	public ApiResponse<AdminAiPromptResponse> createRevision(
			@PathVariable UUID id, @Valid @RequestBody AdminAiPromptCreateRevisionRequest request) {
		return ApiResponse.of(adminAiPromptService.createRevision(id, request.versionLabel(), currentAdminId()));
	}

	@PostMapping("/{id}/publish")
	public ApiResponse<AdminAiPromptResponse> publish(@PathVariable UUID id) {
		return ApiResponse.of(adminAiPromptService.publish(id));
	}

	@PostMapping("/{id}/archive")
	public ApiResponse<AdminAiPromptResponse> archive(@PathVariable UUID id) {
		return ApiResponse.of(adminAiPromptService.archive(id));
	}

	private UUID currentAdminId() {
		AdminAuthentication authentication = (AdminAuthentication) SecurityContextHolder.getContext().getAuthentication();
		return authentication.getAdminUserId();
	}

}
