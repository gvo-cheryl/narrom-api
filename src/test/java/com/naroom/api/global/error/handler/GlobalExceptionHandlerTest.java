package com.naroom.api.global.error.handler;

import com.naroom.api.global.error.response.ProblemDetailFactory;
import org.junit.jupiter.api.Test;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockHttpServletRequest;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 * 보안 3단계 "원문 로그 금지" 관련 검증: 예외 메시지에 기록 원문이 섞여 들어와도
 * 공개 오류 응답(ProblemDetail)에는 고정된 문구만 노출되고 원문이 그대로 새어나가지 않아야 한다.
 */
class GlobalExceptionHandlerTest {

	private final GlobalExceptionHandler handler = new GlobalExceptionHandler(new ProblemDetailFactory());

	@Test
	void handleUnexpected_neverExposesExceptionMessageInResponse() {
		String sensitiveContent = "오늘 정말 힘들었다. 아무한테도 말 못 할 얘기지만 회사에서...";
		RuntimeException ex = new RuntimeException("failed while processing entry body: " + sensitiveContent);
		MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/v1/record/entries");

		ResponseEntity<ProblemDetail> response = handler.handleUnexpected(ex, request);

		ProblemDetail body = response.getBody();
		assertNotNull(body);
		assertFalse(String.valueOf(body.getDetail()).contains(sensitiveContent));
		assertFalse(String.valueOf(body.getTitle()).contains(sensitiveContent));
		assertFalse(body.getProperties().toString().contains(sensitiveContent));
	}

}
