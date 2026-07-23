package com.naroom.api.global.error.handler;

import com.naroom.api.global.error.code.CommonErrorCode;
import com.naroom.api.global.error.code.ErrorCode;
import com.naroom.api.global.error.exception.BusinessException;
import com.naroom.api.global.error.response.ProblemDetailFactory;
import com.naroom.api.global.error.response.ValidationViolation;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.FieldError;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.ServletWebRequest;
import org.springframework.web.context.request.WebRequest;
import org.springframework.validation.method.ParameterValidationResult;
import org.springframework.web.method.annotation.HandlerMethodValidationException;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

import java.util.List;

/**
 * 모든 실패를 같은 ProblemDetail 계약으로 수렴시키는 단일 진입점.
 * Controller에서 개별 try-catch를 작성하지 않는다.
 */
@RestControllerAdvice
public class GlobalExceptionHandler extends ResponseEntityExceptionHandler {

	private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

	private final ProblemDetailFactory problemDetailFactory;

	public GlobalExceptionHandler(ProblemDetailFactory problemDetailFactory) {
		this.problemDetailFactory = problemDetailFactory;
	}

	@ExceptionHandler(BusinessException.class)
	public ResponseEntity<ProblemDetail> handleBusinessException(BusinessException ex, HttpServletRequest request) {
		ProblemDetail problemDetail = problemDetailFactory.create(ex.errorCode(), request, ex.context());
		return ResponseEntity.status(ex.errorCode().httpStatus()).body(problemDetail);
	}

	@ExceptionHandler(ConstraintViolationException.class)
	public ResponseEntity<ProblemDetail> handleConstraintViolation(
			ConstraintViolationException ex,
			HttpServletRequest request) {
		List<ValidationViolation> violations = ex.getConstraintViolations().stream()
				.map(violation -> new ValidationViolation(
						violation.getPropertyPath().toString(),
						"INVALID",
						violation.getMessage()))
				.toList();
		ProblemDetail problemDetail =
				problemDetailFactory.createValidation(CommonErrorCode.VALIDATION_FAILED, request, violations);
		return ResponseEntity.status(CommonErrorCode.VALIDATION_FAILED.httpStatus()).body(problemDetail);
	}

	@ExceptionHandler(Exception.class)
	public ResponseEntity<ProblemDetail> handleUnexpected(Exception ex, HttpServletRequest request) {
		ProblemDetail problemDetail = problemDetailFactory.create(CommonErrorCode.INTERNAL_ERROR, request);
		log.error("Unexpected error. traceId={}", problemDetail.getProperties().get("traceId"), ex);
		return ResponseEntity.status(CommonErrorCode.INTERNAL_ERROR.httpStatus()).body(problemDetail);
	}

	@Override
	protected ResponseEntity<Object> handleMethodArgumentNotValid(
			MethodArgumentNotValidException ex,
			HttpHeaders headers,
			HttpStatusCode status,
			WebRequest request) {
		List<ValidationViolation> violations = ex.getBindingResult().getFieldErrors().stream()
				.map(this::toViolation)
				.toList();
		return buildValidationResponse(request, violations);
	}

	@Override
	protected ResponseEntity<Object> handleHandlerMethodValidationException(
			HandlerMethodValidationException ex,
			HttpHeaders headers,
			HttpStatusCode status,
			WebRequest request) {
		List<ValidationViolation> violations = ex.getParameterValidationResults().stream()
				.flatMap(result -> result.getResolvableErrors().stream()
						.map(error -> new ValidationViolation(
								parameterNameOf(result),
								"INVALID",
								error.getDefaultMessage())))
				.toList();
		return buildValidationResponse(request, violations);
	}

	@Override
	protected ResponseEntity<Object> handleHttpMessageNotReadable(
			HttpMessageNotReadableException ex,
			HttpHeaders headers,
			HttpStatusCode status,
			WebRequest request) {
		// JSON 파싱 자체가 실패한 경우라 필드 단위 violations를 만들 수 없다.
		return buildValidationResponse(request, List.of());
	}

	@Override
	protected ResponseEntity<Object> handleHttpRequestMethodNotSupported(
			HttpRequestMethodNotSupportedException ex,
			HttpHeaders headers,
			HttpStatusCode status,
			WebRequest request) {
		ResponseEntity<Object> defaultResponse = super.handleHttpRequestMethodNotSupported(ex, headers, status, request);
		return enrichDefaultResponse(defaultResponse, CommonErrorCode.VALIDATION_FAILED, request);
	}

	@Override
	protected ResponseEntity<Object> handleHttpMediaTypeNotSupported(
			HttpMediaTypeNotSupportedException ex,
			HttpHeaders headers,
			HttpStatusCode status,
			WebRequest request) {
		ResponseEntity<Object> defaultResponse = super.handleHttpMediaTypeNotSupported(ex, headers, status, request);
		return enrichDefaultResponse(defaultResponse, CommonErrorCode.VALIDATION_FAILED, request);
	}

	private ResponseEntity<Object> buildValidationResponse(WebRequest request, List<ValidationViolation> violations) {
		ProblemDetail problemDetail = problemDetailFactory.createValidation(
				CommonErrorCode.VALIDATION_FAILED, servletRequestOf(request), violations);
		return ResponseEntity.status(CommonErrorCode.VALIDATION_FAILED.httpStatus()).body(problemDetail);
	}

	private ResponseEntity<Object> enrichDefaultResponse(
			ResponseEntity<Object> defaultResponse,
			ErrorCode errorCode,
			WebRequest request) {
		if (defaultResponse.getBody() instanceof ProblemDetail problemDetail) {
			problemDetailFactory.enrich(problemDetail, errorCode, servletRequestOf(request));
		}
		return defaultResponse;
	}

	private ValidationViolation toViolation(FieldError fieldError) {
		return new ValidationViolation(fieldError.getField(), "INVALID", fieldError.getDefaultMessage());
	}

	// TODO: 컴파일 옵션에 -parameters가 없으면 getParameterName()이 null을 반환해 "parameter"로 뭉뚱그려진다.
	// @Validated 쿼리/경로 파라미터 검증을 실제로 쓰게 되면 정확한 필드명이 필요한지 다시 확인한다.
	private String parameterNameOf(ParameterValidationResult result) {
		String name = result.getMethodParameter().getParameterName();
		return name != null ? name : "parameter";
	}

	private HttpServletRequest servletRequestOf(WebRequest request) {
		return ((ServletWebRequest) request).getRequest();
	}

}
