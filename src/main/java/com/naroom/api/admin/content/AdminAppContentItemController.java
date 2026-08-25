package com.naroom.api.admin.content;

import com.naroom.api.admin.auth.AdminAuthentication;
import com.naroom.api.admin.content.dto.AdminAppContentItemCreateRequest;
import com.naroom.api.admin.content.dto.AdminAppContentItemResponse;
import com.naroom.api.admin.content.dto.AdminAppContentItemUpdateRequest;
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
@RequestMapping("/api/v1/admin/content/app-copy")
@PreAuthorize("hasAnyRole('SUPER_ADMIN', 'CONTENT_EDITOR')")
public class AdminAppContentItemController {

	private final AdminAppContentItemService adminAppContentItemService;

	public AdminAppContentItemController(AdminAppContentItemService adminAppContentItemService) {
		this.adminAppContentItemService = adminAppContentItemService;
	}

	@GetMapping
	public ApiResponse<List<AdminAppContentItemResponse>> list(
			@RequestParam(required = false) String q, @RequestParam(required = false) String sort) {
		return ApiResponse.of(adminAppContentItemService.list(q, sort));
	}

	@GetMapping("/{id}")
	public ApiResponse<AdminAppContentItemResponse> get(@PathVariable UUID id) {
		return ApiResponse.of(adminAppContentItemService.get(id));
	}

	@PostMapping
	public ApiResponse<AdminAppContentItemResponse> create(
			@Valid @RequestBody AdminAppContentItemCreateRequest request) {
		return ApiResponse.of(adminAppContentItemService.create(request, currentAdminId()));
	}

	@PutMapping("/{id}")
	public ApiResponse<AdminAppContentItemResponse> update(
			@PathVariable UUID id, @Valid @RequestBody AdminAppContentItemUpdateRequest request) {
		return ApiResponse.of(adminAppContentItemService.update(id, request));
	}

	@PostMapping("/{id}/revisions")
	@ResponseStatus(HttpStatus.CREATED)
	public ApiResponse<AdminAppContentItemResponse> createRevision(@PathVariable UUID id) {
		return ApiResponse.of(adminAppContentItemService.createRevision(id, currentAdminId()));
	}

	@PostMapping("/{id}/publish")
	public ApiResponse<AdminAppContentItemResponse> publish(@PathVariable UUID id) {
		return ApiResponse.of(adminAppContentItemService.publish(id));
	}

	@PostMapping("/{id}/archive")
	public ApiResponse<AdminAppContentItemResponse> archive(@PathVariable UUID id) {
		return ApiResponse.of(adminAppContentItemService.archive(id));
	}

	// AdminSessionAuthenticationFilter가 SecurityContextHolder에 직접 채워 넣는 방식이라 여기서도 직접 꺼낸다.
	private UUID currentAdminId() {
		AdminAuthentication authentication = (AdminAuthentication) SecurityContextHolder.getContext().getAuthentication();
		return authentication.getAdminUserId();
	}

}
