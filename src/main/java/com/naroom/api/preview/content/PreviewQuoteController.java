package com.naroom.api.preview.content;

import com.naroom.api.content.QuoteService;
import com.naroom.api.content.dto.QuoteResponse;
import com.naroom.api.global.error.exception.BusinessException;
import com.naroom.api.global.response.ApiResponse;
import com.naroom.api.preview.auth.PreviewAuthentication;
import com.naroom.api.preview.domain.error.PreviewErrorCode;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

// D-3: preview session이 선택한 "quote" 버전을 홈 화면과 동일한 응답 형태로 내려준다.
// 일반 회원 API(/api/v1/content/quotes/today)와 달리 오늘 날짜 순환 로직을 쓰지 않고
// 관리자가 preview session 발급 시 지정한 버전을 그대로 반환한다.
@RestController
@RequestMapping("/api/v1/preview/content/quotes")
public class PreviewQuoteController {

	private static final String CONTENT_KEY = "quote";

	private final QuoteService quoteService;

	public PreviewQuoteController(QuoteService quoteService) {
		this.quoteService = quoteService;
	}

	@GetMapping("/today")
	public ApiResponse<QuoteResponse> getPreviewTodayQuote() {
		UUID quoteId = currentPreviewAuthentication().getSelectedContentVersions().get(CONTENT_KEY);
		if (quoteId == null) {
			throw new BusinessException(PreviewErrorCode.PREVIEW_CONTENT_NOT_SELECTED);
		}
		return ApiResponse.of(quoteService.getQuoteForPreview(quoteId));
	}

	private PreviewAuthentication currentPreviewAuthentication() {
		return (PreviewAuthentication) SecurityContextHolder.getContext().getAuthentication();
	}

}
