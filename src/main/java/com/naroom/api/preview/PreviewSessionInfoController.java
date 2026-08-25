package com.naroom.api.preview;

import com.naroom.api.global.response.ApiResponse;
import com.naroom.api.preview.auth.PreviewAuthentication;
import com.naroom.api.preview.dto.PreviewSessionInfoResponse;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

// naroom-app의 preview 빌드가 부팅 시 이 API로 자신의 미리보기 컨텍스트(어떤 DRAFT 버전을 보여줄지,
// 어떤 시나리오인지)를 읽는다. 실제 콘텐츠별 preview 응답(D-3/D-4)은 이 인증 기반 위에 얹는다.
@RestController
@RequestMapping("/api/v1/preview/session")
public class PreviewSessionInfoController {

	@GetMapping
	public ApiResponse<PreviewSessionInfoResponse> get() {
		PreviewAuthentication authentication = (PreviewAuthentication) SecurityContextHolder.getContext().getAuthentication();
		return ApiResponse.of(PreviewSessionInfoResponse.from(authentication));
	}

}
