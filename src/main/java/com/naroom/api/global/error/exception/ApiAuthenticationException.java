package com.naroom.api.global.error.exception;

import com.naroom.api.global.error.code.ErrorCode;
import org.springframework.security.core.AuthenticationException;

// TODO: JWT 인증 필터(오늘 10번)가 만들어지면 그 필터가 이 예외를 던지는 실제 발생 지점이 된다. 지금은 아직 아무도 던지지 않는다.
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
