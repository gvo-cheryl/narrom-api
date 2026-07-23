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
		// 메시지는 code만 담는다. 실제 노출 문구는 ErrorCode의 title/detail이 담당하고,
		// 여기 message는 서버 로그·스택트레이스 식별용이다(exception-handling.md 원칙: 메시지를 detail로 쓰지 않음).
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
