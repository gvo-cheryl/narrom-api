package com.naroom.api.global.error.exception;

import com.naroom.api.global.error.code.ErrorCode;
import org.springframework.security.core.AuthenticationException;

public class ApiAuthenticationException extends AuthenticationException {

	private final ErrorCode errorCode;

	public ApiAuthenticationException(ErrorCode errorCode) {
		super(errorCode.code());
		this.errorCode = errorCode;
	}

	public ErrorCode errorCode() {
		return errorCode;
	}

}
