package com.naroom.api.global.error.exception;

import com.naroom.api.global.error.code.ErrorCode;

import java.util.Map;

public class BusinessException extends RuntimeException {

	private final ErrorCode errorCode;
	private final Map<String, Object> context;

	public BusinessException(ErrorCode errorCode) {
		this(errorCode, Map.of());
	}

	public BusinessException(ErrorCode errorCode, Map<String, Object> context) {
		super(errorCode.code());
		this.errorCode = errorCode;
		this.context = context == null ? Map.of() : Map.copyOf(context);
	}

	public ErrorCode errorCode() {
		return errorCode;
	}

	public Map<String, Object> context() {
		return context;
	}

}
