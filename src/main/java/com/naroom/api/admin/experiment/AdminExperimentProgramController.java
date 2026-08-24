package com.naroom.api.admin.experiment;

import com.naroom.api.admin.auth.AdminAuthentication;
import com.naroom.api.admin.experiment.dto.AdminExperimentProgramCreateRequest;
import com.naroom.api.admin.experiment.dto.AdminExperimentProgramResponse;
import com.naroom.api.admin.experiment.dto.AdminExperimentProgramUpdateRequest;
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
@RequestMapping("/api/v1/admin/experiments/programs")
@PreAuthorize("hasAnyRole('SUPER_ADMIN', 'CONTENT_EDITOR')")
public class AdminExperimentProgramController {

	private final AdminExperimentProgramService adminExperimentProgramService;

	public AdminExperimentProgramController(AdminExperimentProgramService adminExperimentProgramService) {
		this.adminExperimentProgramService = adminExperimentProgramService;
	}

	@GetMapping
	public ApiResponse<List<AdminExperimentProgramResponse>> list() {
		return ApiResponse.of(adminExperimentProgramService.list());
	}

	@GetMapping("/{id}")
	public ApiResponse<AdminExperimentProgramResponse> get(@PathVariable UUID id) {
		return ApiResponse.of(adminExperimentProgramService.get(id));
	}

	@PostMapping
	public ApiResponse<AdminExperimentProgramResponse> create(
			@Valid @RequestBody AdminExperimentProgramCreateRequest request) {
		return ApiResponse.of(adminExperimentProgramService.create(request, currentAdminId()));
	}

	@PutMapping("/{id}")
	public ApiResponse<AdminExperimentProgramResponse> update(
			@PathVariable UUID id, @Valid @RequestBody AdminExperimentProgramUpdateRequest request) {
		return ApiResponse.of(adminExperimentProgramService.update(id, request));
	}

	@PostMapping("/{id}/versions")
	@ResponseStatus(HttpStatus.CREATED)
	public ApiResponse<AdminExperimentProgramResponse> createRevision(@PathVariable UUID id) {
		return ApiResponse.of(adminExperimentProgramService.createRevision(id, currentAdminId()));
	}

	@PostMapping("/{id}/publish")
	public ApiResponse<AdminExperimentProgramResponse> publish(@PathVariable UUID id) {
		return ApiResponse.of(adminExperimentProgramService.publish(id));
	}

	@PostMapping("/{id}/archive")
	public ApiResponse<AdminExperimentProgramResponse> archive(@PathVariable UUID id) {
		return ApiResponse.of(adminExperimentProgramService.archive(id));
	}

	// AdminSessionAuthenticationFilter가 SecurityContextHolder에 직접 채워 넣는 방식이라 여기서도 직접 꺼낸다.
	private UUID currentAdminId() {
		AdminAuthentication authentication = (AdminAuthentication) SecurityContextHolder.getContext().getAuthentication();
		return authentication.getAdminUserId();
	}

}
