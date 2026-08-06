package com.naroom.api.experiment;

import com.naroom.api.auth.security.MemberAuthentication;
import com.naroom.api.experiment.dto.ExperimentActiveProgramResponse;
import com.naroom.api.experiment.dto.ExperimentCourseReviewRequest;
import com.naroom.api.experiment.dto.ExperimentCourseReviewResponse;
import com.naroom.api.experiment.dto.ExperimentEndEarlyResponse;
import com.naroom.api.experiment.dto.ExperimentMissionRecordRequest;
import com.naroom.api.experiment.dto.ExperimentMissionRecordResponse;
import com.naroom.api.experiment.dto.ExperimentMissionReplaceRequest;
import com.naroom.api.experiment.dto.ExperimentMissionReplaceResponse;
import com.naroom.api.experiment.dto.ExperimentPastProgramResponse;
import com.naroom.api.experiment.dto.ExperimentProgramDetailResponse;
import com.naroom.api.experiment.dto.ExperimentProgramSaveResponse;
import com.naroom.api.experiment.dto.ExperimentProgramStartRequest;
import com.naroom.api.experiment.dto.ExperimentProgramStartResponse;
import com.naroom.api.experiment.dto.ExperimentProgramSummaryResponse;
import com.naroom.api.experiment.dto.ExperimentRandomProgramResponse;
import com.naroom.api.experiment.dto.ExperimentRandomProgramStartRequest;
import com.naroom.api.experiment.dto.ExperimentTopicResponse;
import com.naroom.api.experiment.dto.ExperimentUserComposedProgramRequest;
import com.naroom.api.experiment.dto.ExperimentUserComposedProgramResponse;
import com.naroom.api.global.response.ApiResponse;
import jakarta.validation.Valid;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/experiments")
public class ExperimentController {

	private final ExperimentTopicService experimentTopicService;
	private final ExperimentProgramService experimentProgramService;
	private final ExperimentRandomProgramComposer experimentRandomProgramComposer;
	private final ExperimentEnrollmentService experimentEnrollmentService;
	private final ExperimentProgressService experimentProgressService;
	private final ExperimentReviewService experimentReviewService;

	public ExperimentController(
			ExperimentTopicService experimentTopicService,
			ExperimentProgramService experimentProgramService,
			ExperimentRandomProgramComposer experimentRandomProgramComposer,
			ExperimentEnrollmentService experimentEnrollmentService,
			ExperimentProgressService experimentProgressService,
			ExperimentReviewService experimentReviewService) {
		this.experimentTopicService = experimentTopicService;
		this.experimentProgramService = experimentProgramService;
		this.experimentRandomProgramComposer = experimentRandomProgramComposer;
		this.experimentEnrollmentService = experimentEnrollmentService;
		this.experimentProgressService = experimentProgressService;
		this.experimentReviewService = experimentReviewService;
	}

	@GetMapping("/topics")
	public ApiResponse<List<ExperimentTopicResponse>> getTopics() {
		return ApiResponse.of(experimentTopicService.listActive());
	}

	@GetMapping("/programs")
	public ApiResponse<List<ExperimentProgramSummaryResponse>> getPrograms(
			@RequestParam(required = false) Short durationDays,
			@RequestParam(required = false) String topicCode,
			@RequestParam(required = false) Boolean featured,
			@RequestParam(required = false) Boolean beginner) {
		return ApiResponse.of(experimentProgramService.list(durationDays, topicCode, featured, beginner));
	}

	@GetMapping("/programs/random")
	public ApiResponse<ExperimentRandomProgramResponse> getRandomProgram(@RequestParam short days) {
		return ApiResponse.of(experimentRandomProgramComposer.compose(currentMemberId(), days));
	}

	@GetMapping("/programs/{programId}")
	public ApiResponse<ExperimentProgramDetailResponse> getProgram(@PathVariable UUID programId) {
		return ApiResponse.of(experimentProgramService.getDetail(programId));
	}

	@PostMapping("/programs/{programId}/start")
	public ApiResponse<ExperimentProgramStartResponse> startProgram(
			@PathVariable UUID programId, @RequestBody(required = false) ExperimentProgramStartRequest request) {
		return ApiResponse.of(experimentEnrollmentService.startFromTemplate(currentMemberId(), programId, requestOrEmpty(request)));
	}

	@PostMapping("/programs/{programId}/save")
	public ApiResponse<ExperimentProgramSaveResponse> saveProgram(
			@PathVariable UUID programId, @RequestBody(required = false) ExperimentProgramStartRequest request) {
		return ApiResponse.of(experimentEnrollmentService.saveFromTemplate(currentMemberId(), programId, requestOrEmpty(request)));
	}

	@PostMapping("/programs/user-composed")
	public ApiResponse<ExperimentUserComposedProgramResponse> createUserComposedProgram(
			@Valid @RequestBody ExperimentUserComposedProgramRequest request) {
		return ApiResponse.of(experimentEnrollmentService.createUserComposed(currentMemberId(), request));
	}

	@PostMapping("/programs/random/start")
	public ApiResponse<ExperimentProgramStartResponse> startRandomProgram(@RequestBody ExperimentRandomProgramStartRequest request) {
		return ApiResponse.of(experimentEnrollmentService.startFromRandom(currentMemberId(), request));
	}

	@PostMapping("/user-programs/{userExperimentProgramId}/activate")
	public ApiResponse<ExperimentProgramStartResponse> activateSavedProgram(
			@PathVariable UUID userExperimentProgramId,
			@RequestParam(defaultValue = "false") boolean replaceActiveProgram) {
		return ApiResponse.of(
				experimentEnrollmentService.activateSaved(currentMemberId(), userExperimentProgramId, replaceActiveProgram));
	}

	private ExperimentProgramStartRequest requestOrEmpty(ExperimentProgramStartRequest request) {
		return request != null ? request : new ExperimentProgramStartRequest(null, List.of(), null, false);
	}

	@GetMapping("/user-programs/active")
	public ApiResponse<ExperimentActiveProgramResponse> getActiveProgram() {
		return ApiResponse.of(experimentProgressService.getActive(currentMemberId()).orElse(null));
	}

	@PostMapping("/user-programs/{userExperimentProgramId}/missions/{userProgramMissionId}/record")
	public ApiResponse<ExperimentMissionRecordResponse> recordMission(
			@PathVariable UUID userExperimentProgramId,
			@PathVariable UUID userProgramMissionId,
			@Valid @RequestBody ExperimentMissionRecordRequest request) {
		return ApiResponse.of(experimentProgressService.recordMission(
				currentMemberId(), userExperimentProgramId, userProgramMissionId, request));
	}

	@PostMapping("/user-programs/{userExperimentProgramId}/missions/{userProgramMissionId}/replace")
	public ApiResponse<ExperimentMissionReplaceResponse> replaceMission(
			@PathVariable UUID userExperimentProgramId,
			@PathVariable UUID userProgramMissionId,
			@Valid @RequestBody ExperimentMissionReplaceRequest request) {
		return ApiResponse.of(experimentProgressService.replaceMission(
				currentMemberId(), userExperimentProgramId, userProgramMissionId, request));
	}

	@PostMapping("/user-programs/{userExperimentProgramId}/review")
	public ApiResponse<ExperimentCourseReviewResponse> completeReview(
			@PathVariable UUID userExperimentProgramId, @RequestBody ExperimentCourseReviewRequest request) {
		return ApiResponse.of(experimentReviewService.completeReview(currentMemberId(), userExperimentProgramId, request));
	}

	@PostMapping("/user-programs/{userExperimentProgramId}/end-early")
	public ApiResponse<ExperimentEndEarlyResponse> endEarly(@PathVariable UUID userExperimentProgramId) {
		return ApiResponse.of(experimentReviewService.endEarly(currentMemberId(), userExperimentProgramId));
	}

	@GetMapping("/user-programs/past")
	public ApiResponse<List<ExperimentPastProgramResponse>> getPastPrograms() {
		return ApiResponse.of(experimentReviewService.listPast(currentMemberId()));
	}

	// JwtAuthenticationFilter가 SecurityContextHolder에 직접 채워 넣는 방식이라 여기서도 직접 꺼낸다
	// (LifetimeController 등과 동일한 이유).
	private UUID currentMemberId() {
		MemberAuthentication authentication =
				(MemberAuthentication) SecurityContextHolder.getContext().getAuthentication();
		return authentication.getMemberId();
	}

}
