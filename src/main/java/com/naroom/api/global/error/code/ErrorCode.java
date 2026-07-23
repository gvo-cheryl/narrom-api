package com.naroom.api.global.error.code;

import org.springframework.http.HttpStatus;

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
