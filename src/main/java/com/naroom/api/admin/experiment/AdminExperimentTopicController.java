package com.naroom.api.admin.experiment;

import com.naroom.api.admin.experiment.dto.AdminExperimentTopicCreateRequest;
import com.naroom.api.admin.experiment.dto.AdminExperimentTopicResponse;
import com.naroom.api.admin.experiment.dto.AdminExperimentTopicUpdateRequest;
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
@RequestMapping("/api/v1/admin/experiments/topics")
@PreAuthorize("hasAnyRole('SUPER_ADMIN', 'CONTENT_EDITOR')")
public class AdminExperimentTopicController {

	private final AdminExperimentTopicService adminExperimentTopicService;

	public AdminExperimentTopicController(AdminExperimentTopicService adminExperimentTopicService) {
		this.adminExperimentTopicService = adminExperimentTopicService;
	}

	@GetMapping
	public ApiResponse<List<AdminExperimentTopicResponse>> list(
			@RequestParam(required = false) String q, @RequestParam(required = false) String sort) {
		return ApiResponse.of(adminExperimentTopicService.list(q, sort));
	}

	@GetMapping("/{id}")
	public ApiResponse<AdminExperimentTopicResponse> get(@PathVariable UUID id) {
		return ApiResponse.of(adminExperimentTopicService.get(id));
	}

	@PostMapping
	public ApiResponse<AdminExperimentTopicResponse> create(@Valid @RequestBody AdminExperimentTopicCreateRequest request) {
		return ApiResponse.of(adminExperimentTopicService.create(request));
	}

	@PutMapping("/{id}")
	public ApiResponse<AdminExperimentTopicResponse> update(
			@PathVariable UUID id, @Valid @RequestBody AdminExperimentTopicUpdateRequest request) {
		return ApiResponse.of(adminExperimentTopicService.update(id, request));
	}

}
