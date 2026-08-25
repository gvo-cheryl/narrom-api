package com.naroom.api.admin.content;

import com.naroom.api.admin.auth.AdminAuthentication;
import com.naroom.api.admin.content.dto.AdminQuoteCreateRequest;
import com.naroom.api.admin.content.dto.AdminQuoteResponse;
import com.naroom.api.admin.content.dto.AdminQuoteUpdateRequest;
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
@RequestMapping("/api/v1/admin/content/quotes")
@PreAuthorize("hasAnyRole('SUPER_ADMIN', 'CONTENT_EDITOR')")
public class AdminQuoteController {

	private final AdminQuoteService adminQuoteService;

	public AdminQuoteController(AdminQuoteService adminQuoteService) {
		this.adminQuoteService = adminQuoteService;
	}

	@GetMapping
	public ApiResponse<List<AdminQuoteResponse>> list(
			@RequestParam(required = false) String q, @RequestParam(required = false) String sort) {
		return ApiResponse.of(adminQuoteService.list(q, sort));
	}

	@GetMapping("/{id}")
	public ApiResponse<AdminQuoteResponse> get(@PathVariable UUID id) {
		return ApiResponse.of(adminQuoteService.get(id));
	}

	@PostMapping
	public ApiResponse<AdminQuoteResponse> create(@Valid @RequestBody AdminQuoteCreateRequest request) {
		return ApiResponse.of(adminQuoteService.create(request, currentAdminId()));
	}

	@PutMapping("/{id}")
	public ApiResponse<AdminQuoteResponse> update(
			@PathVariable UUID id, @Valid @RequestBody AdminQuoteUpdateRequest request) {
		return ApiResponse.of(adminQuoteService.update(id, request));
	}

	@PostMapping("/{id}/revisions")
	@ResponseStatus(HttpStatus.CREATED)
	public ApiResponse<AdminQuoteResponse> createRevision(@PathVariable UUID id) {
		return ApiResponse.of(adminQuoteService.createRevision(id, currentAdminId()));
	}

	@PostMapping("/{id}/publish")
	public ApiResponse<AdminQuoteResponse> publish(@PathVariable UUID id) {
		return ApiResponse.of(adminQuoteService.publish(id));
	}

	@PostMapping("/{id}/archive")
	public ApiResponse<AdminQuoteResponse> archive(@PathVariable UUID id) {
		return ApiResponse.of(adminQuoteService.archive(id));
	}

	// AdminSessionAuthenticationFilter가 SecurityContextHolder에 직접 채워 넣는 방식이라 여기서도 직접 꺼낸다.
	private UUID currentAdminId() {
		AdminAuthentication authentication = (AdminAuthentication) SecurityContextHolder.getContext().getAuthentication();
		return authentication.getAdminUserId();
	}

}
