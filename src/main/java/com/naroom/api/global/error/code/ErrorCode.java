package com.naroom.api.global.error.code;

import org.springframework.http.HttpStatus;

// 구체 구현은 기능 패키지가 소유한다(예: CommonErrorCode, auth.domain.error.AuthErrorCode).
// 필드 의미는 docs/api/error-response.md가 기준이다.
public interface ErrorCode {

	HttpStatus httpStatus();

	String code();

	String type();

	String title();

	String detail();

	ErrorStage stage();

	ClientAction action();

	boolean retryable();

}
