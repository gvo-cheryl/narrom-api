package com.naroom.api.admin.experiment;

import com.naroom.api.admin.experiment.dto.AdminExperimentMissionCreateRequest;
import com.naroom.api.admin.experiment.dto.AdminExperimentMissionResponse;
import com.naroom.api.admin.experiment.dto.AdminExperimentMissionUpdateRequest;
import com.naroom.api.global.response.ApiResponse;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/admin/experiments/missions")
@PreAuthorize("hasAnyRole('SUPER_ADMIN', 'CONTENT_EDITOR')")
public class AdminExperimentMissionController {

	private final AdminExperimentMissionService adminExperimentMissionService;

	public AdminExperimentMissionController(AdminExperimentMissionService adminExperimentMissionService) {
		this.adminExperimentMissionService = adminExperimentMissionService;
	}

	@GetMapping
	public ApiResponse<List<AdminExperimentMissionResponse>> list(
			@RequestParam(required = false) String q, @RequestParam(required = false) String sort) {
		return ApiResponse.of(adminExperimentMissionService.list(q, sort));
	}

	@GetMapping("/{id}")
	public ApiResponse<AdminExperimentMissionResponse> get(@PathVariable UUID id) {
		return ApiResponse.of(adminExperimentMissionService.get(id));
	}

	@PostMapping
	public ApiResponse<AdminExperimentMissionResponse> create(
			@Valid @RequestBody AdminExperimentMissionCreateRequest request) {
		return ApiResponse.of(adminExperimentMissionService.create(request));
	}

	@PutMapping("/{id}")
	public ApiResponse<AdminExperimentMissionResponse> update(
			@PathVariable UUID id, @Valid @RequestBody AdminExperimentMissionUpdateRequest request) {
		return ApiResponse.of(adminExperimentMissionService.update(id, request));
	}

}
