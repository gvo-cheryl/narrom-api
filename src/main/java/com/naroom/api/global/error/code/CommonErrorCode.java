package com.naroom.api.global.error.code;

import org.springframework.http.HttpStatus;

public enum CommonErrorCode implements ErrorCode {

	VALIDATION_FAILED(
			HttpStatus.BAD_REQUEST,
			"COMMON_VALIDATION_FAILED",
			"urn:naroom:problem:validation-failed",
			"요청 내용을 확인해 주세요",
			"입력한 값 중 확인이 필요한 항목이 있습니다.",
			ErrorStage.REQUEST,
			ClientAction.CHECK_REQUEST,
			false),

	RATE_LIMITED(
			HttpStatus.TOO_MANY_REQUESTS,
			"COMMON_RATE_LIMITED",
			"urn:naroom:problem:common-rate-limited",
			"요청이 너무 많습니다",
			"잠시 후 다시 시도해 주세요.",
			ErrorStage.REQUEST,
			ClientAction.RETRY,
			true),

	INTERNAL_ERROR(
			HttpStatus.INTERNAL_SERVER_ERROR,
			"COMMON_INTERNAL_ERROR",
			"urn:naroom:problem:common-internal-error",
			"일시적인 오류가 발생했습니다",
			"잠시 후 다시 시도해 주세요.",
			ErrorStage.INTERNAL,
			ClientAction.CONTACT_SUPPORT,
			false);

	private final HttpStatus httpStatus;
	private final String code;
	private final String type;
	private final String title;
	private final String detail;
	private final ErrorStage stage;
	private final ClientAction action;
	private final boolean retryable;

	CommonErrorCode(
			HttpStatus httpStatus,
			String code,
			String type,
			String title,
			String detail,
			ErrorStage stage,
			ClientAction action,
			boolean retryable) {
		this.httpStatus = httpStatus;
		this.code = code;
		this.type = type;
		this.title = title;
		this.detail = detail;
		this.stage = stage;
		this.action = action;
		this.retryable = retryable;
	}

	@Override
	public HttpStatus httpStatus() {
		return httpStatus;
	}

	@Override
	public String code() {
		return code;
	}

	@Override
	public String type() {
		return type;
	}

	@Override
	public String title() {
		return title;
	}

	@Override
	public String detail() {
		return detail;
	}

	@Override
	public ErrorStage stage() {
		return stage;
	}

	@Override
	public ClientAction action() {
		return action;
	}

	@Override
	public boolean retryable() {
		return retryable;
	}

}
