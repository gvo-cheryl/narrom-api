package com.naroom.api.preview.content;

import com.naroom.api.experiment.ExperimentProgramService;
import com.naroom.api.experiment.dto.ExperimentProgramDetailResponse;
import com.naroom.api.global.error.exception.BusinessException;
import com.naroom.api.global.response.ApiResponse;
import com.naroom.api.preview.auth.PreviewAuthentication;
import com.naroom.api.preview.domain.error.PreviewErrorCode;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

// D-4(카탈로그 열람 우선 스코프): preview session이 선택한 "experimentProgram" 버전의 상세를
// 회원 화면(E04)과 동일한 응답 형태로 내려준다. 실제 진행 상태(UserExperimentProgram)는 다루지
// 않는다 - synthetic member 인프라가 아직 없어 별도 작업으로 분리했다.
@RestController
@RequestMapping("/api/v1/preview/content/experiment-programs")
public class PreviewExperimentProgramController {

	private static final String CONTENT_KEY = "experimentProgram";

	private final ExperimentProgramService experimentProgramService;

	public PreviewExperimentProgramController(ExperimentProgramService experimentProgramService) {
		this.experimentProgramService = experimentProgramService;
	}

	@GetMapping
	public ApiResponse<ExperimentProgramDetailResponse> getPreviewProgramDetail() {
		UUID programId = currentPreviewAuthentication().getSelectedContentVersions().get(CONTENT_KEY);
		if (programId == null) {
			throw new BusinessException(PreviewErrorCode.PREVIEW_CONTENT_NOT_SELECTED);
		}
		return ApiResponse.of(experimentProgramService.getDetail(programId));
	}

	private PreviewAuthentication currentPreviewAuthentication() {
		return (PreviewAuthentication) SecurityContextHolder.getContext().getAuthentication();
	}

}
