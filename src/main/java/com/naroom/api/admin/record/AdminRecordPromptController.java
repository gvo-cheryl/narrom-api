package com.naroom.api.admin.record;

import com.naroom.api.admin.auth.AdminAuthentication;
import com.naroom.api.admin.record.dto.AdminRecordPromptCreateRequest;
import com.naroom.api.admin.record.dto.AdminRecordPromptResponse;
import com.naroom.api.admin.record.dto.AdminRecordPromptUpdateRequest;
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
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/admin/content/record-prompts")
@PreAuthorize("hasAnyRole('SUPER_ADMIN', 'CONTENT_EDITOR')")
public class AdminRecordPromptController {

	private final AdminRecordPromptService adminRecordPromptService;

	public AdminRecordPromptController(AdminRecordPromptService adminRecordPromptService) {
		this.adminRecordPromptService = adminRecordPromptService;
	}

	@GetMapping
	public ApiResponse<List<AdminRecordPromptResponse>> list(
			@RequestParam(required = false) String q, @RequestParam(required = false) String sort) {
		return ApiResponse.of(adminRecordPromptService.list(q, sort));
	}

	@GetMapping("/{id}")
	public ApiResponse<AdminRecordPromptResponse> get(@PathVariable UUID id) {
		return ApiResponse.of(adminRecordPromptService.get(id));
	}

	@PostMapping
	public ApiResponse<AdminRecordPromptResponse> create(@Valid @RequestBody AdminRecordPromptCreateRequest request) {
		return ApiResponse.of(adminRecordPromptService.create(request, currentAdminId()));
	}

	@PutMapping("/{id}")
	public ApiResponse<AdminRecordPromptResponse> update(
			@PathVariable UUID id, @Valid @RequestBody AdminRecordPromptUpdateRequest request) {
		return ApiResponse.of(adminRecordPromptService.update(id, request));
	}

	@PostMapping("/{id}/revisions")
	@ResponseStatus(HttpStatus.CREATED)
	public ApiResponse<AdminRecordPromptResponse> createRevision(@PathVariable UUID id) {
		return ApiResponse.of(adminRecordPromptService.createRevision(id, currentAdminId()));
	}

	@PostMapping("/{id}/publish")
	public ApiResponse<AdminRecordPromptResponse> publish(@PathVariable UUID id) {
		return ApiResponse.of(adminRecordPromptService.publish(id));
	}

	@PostMapping("/{id}/archive")
	public ApiResponse<AdminRecordPromptResponse> archive(@PathVariable UUID id) {
		return ApiResponse.of(adminRecordPromptService.archive(id));
	}

	// AdminSessionAuthenticationFilter가 SecurityContextHolder에 직접 채워 넣는 방식이라 여기서도 직접 꺼낸다.
	private UUID currentAdminId() {
		AdminAuthentication authentication = (AdminAuthentication) SecurityContextHolder.getContext().getAuthentication();
		return authentication.getAdminUserId();
	}

}
