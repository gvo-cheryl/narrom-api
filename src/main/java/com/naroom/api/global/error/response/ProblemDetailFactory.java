package com.naroom.api.global.error.response;

import com.naroom.api.global.error.code.ErrorCode;
import com.naroom.api.global.web.RequestTraceFilter;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.ProblemDetail;
import org.springframework.stereotype.Component;

import java.net.URI;
import java.time.Instant;
import java.util.List;
import java.util.Map;

@Component
public class ProblemDetailFactory {

	public ProblemDetail create(ErrorCode errorCode, HttpServletRequest request) {
		return create(errorCode, request, null, null);
	}

	public ProblemDetail create(ErrorCode errorCode, HttpServletRequest request, Map<String, Object> context) {
		return create(errorCode, request, context, null);
	}

	public ProblemDetail createValidation(
			ErrorCode errorCode,
			HttpServletRequest request,
			List<ValidationViolation> violations) {
		return create(errorCode, request, null, violations);
	}

	private ProblemDetail create(
			ErrorCode errorCode,
			HttpServletRequest request,
			Map<String, Object> context,
			List<ValidationViolation> violations) {
		ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(errorCode.httpStatus(), errorCode.detail());
		problemDetail.setType(URI.create(errorCode.type()));
		problemDetail.setTitle(errorCode.title());
		// getRequestURI() already excludes the query string.
		problemDetail.setInstance(URI.create(request.getRequestURI()));
		enrich(problemDetail, errorCode, request);

		if (context != null && !context.isEmpty()) {
			problemDetail.setProperty("context", context);
		}
		if (violations != null && !violations.isEmpty()) {
			problemDetail.setProperty("violations", violations);
		}

		return problemDetail;
	}

	/**
	 * Spring MVC 기본 예외 처리기가 이미 만든 {@link ProblemDetail}(상태·title·detail 포함)에
	 * 공통 확장 필드(code/stage/action/retryable/timestamp/traceId)만 추가할 때 사용한다.
	 */
	public void enrich(ProblemDetail problemDetail, ErrorCode errorCode, HttpServletRequest request) {
		problemDetail.setProperty("code", errorCode.code());
		problemDetail.setProperty("stage", errorCode.stage().name());
		problemDetail.setProperty("action", errorCode.action().name());
		problemDetail.setProperty("retryable", errorCode.retryable());
		problemDetail.setProperty("timestamp", Instant.now());
		problemDetail.setProperty("traceId", traceIdOf(request));
	}

	private String traceIdOf(HttpServletRequest request) {
		Object traceId = request.getAttribute(RequestTraceFilter.TRACE_ID_ATTRIBUTE);
		return traceId != null ? traceId.toString() : null;
	}

}
