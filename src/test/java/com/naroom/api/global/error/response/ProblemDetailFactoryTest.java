package com.naroom.api.global.error.response;

import com.naroom.api.global.error.code.CommonErrorCode;
import com.naroom.api.global.web.RequestTraceFilter;
import org.junit.jupiter.api.Test;
import org.springframework.http.ProblemDetail;
import org.springframework.mock.web.MockHttpServletRequest;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ProblemDetailFactoryTest {

	private final ProblemDetailFactory problemDetailFactory = new ProblemDetailFactory();

	@Test
	void create_setsAllContractFieldsFromErrorCode() {
		MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/v1/example");
		request.setQueryString("foo=bar");
		request.setAttribute(RequestTraceFilter.TRACE_ID_ATTRIBUTE, "trace-123");

		ProblemDetail problemDetail = problemDetailFactory.create(CommonErrorCode.RATE_LIMITED, request);

		assertEquals(CommonErrorCode.RATE_LIMITED.httpStatus().value(), problemDetail.getStatus());
		assertEquals(CommonErrorCode.RATE_LIMITED.type(), problemDetail.getType().toString());
		assertEquals(CommonErrorCode.RATE_LIMITED.title(), problemDetail.getTitle());
		assertEquals(CommonErrorCode.RATE_LIMITED.detail(), problemDetail.getDetail());
		assertEquals("/api/v1/example", problemDetail.getInstance().toString());
		assertEquals("COMMON_RATE_LIMITED", problemDetail.getProperties().get("code"));
		assertEquals("REQUEST", problemDetail.getProperties().get("stage"));
		assertEquals("RETRY", problemDetail.getProperties().get("action"));
		assertEquals(true, problemDetail.getProperties().get("retryable"));
		assertNotNull(problemDetail.getProperties().get("timestamp"));
		assertEquals("trace-123", problemDetail.getProperties().get("traceId"));
	}

	@Test
	void create_withoutTraceIdAttribute_setsNullTraceId() {
		MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/v1/example");

		ProblemDetail problemDetail = problemDetailFactory.create(CommonErrorCode.INTERNAL_ERROR, request);

		assertNull(problemDetail.getProperties().get("traceId"));
	}

	@Test
	void create_withContext_addsContextProperty() {
		MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/v1/example");

		ProblemDetail problemDetail = problemDetailFactory.create(
				CommonErrorCode.VALIDATION_FAILED, request, Map.of("retryAfterSeconds", 30));

		assertEquals(Map.of("retryAfterSeconds", 30), problemDetail.getProperties().get("context"));
	}

	@Test
	void createValidation_addsViolationsProperty() {
		MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/v1/example");
		List<ValidationViolation> violations = List.of(new ValidationViolation("field", "REQUIRED", "필수 값입니다."));

		ProblemDetail problemDetail =
				problemDetailFactory.createValidation(CommonErrorCode.VALIDATION_FAILED, request, violations);

		assertEquals(violations, problemDetail.getProperties().get("violations"));
	}

	@Test
	void enrich_addsExtensionFieldsToExistingProblemDetail() {
		MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/v1/example");
		ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(
				CommonErrorCode.VALIDATION_FAILED.httpStatus(), "native detail");

		problemDetailFactory.enrich(problemDetail, CommonErrorCode.VALIDATION_FAILED, request);

		assertEquals("native detail", problemDetail.getDetail());
		assertEquals("COMMON_VALIDATION_FAILED", problemDetail.getProperties().get("code"));
		assertFalse((Boolean) problemDetail.getProperties().get("retryable"));
		assertTrue(problemDetail.getProperties().containsKey("timestamp"));
	}

}
