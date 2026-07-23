package com.naroom.api.auth.domain.error;

import com.naroom.api.global.error.code.ClientAction;
import com.naroom.api.global.error.code.ErrorCode;
import com.naroom.api.global.error.code.ErrorStage;
import org.springframework.http.HttpStatus;

/**
 * Auth 도메인 오류 코드. Access Token/Refresh Token/카카오 로그인 등 나머지 코드는
 * 인증 기능(오늘 12번)을 구현하며 error-response.md의 "인증·시작 단계 오류" 표를 기준으로 추가한다.
 */
public enum AuthErrorCode implements ErrorCode {

	AUTH_REQUIRED(
			HttpStatus.UNAUTHORIZED,
			"AUTH_REQUIRED",
			"urn:naroom:problem:auth-required",
			"로그인이 필요합니다",
			"다시 로그인해 주세요.",
			ErrorStage.TOKEN,
			ClientAction.LOGIN_REQUIRED,
			false),

	AUTH_FORBIDDEN(
			HttpStatus.FORBIDDEN,
			"AUTH_FORBIDDEN",
			"urn:naroom:problem:auth-forbidden",
			"접근 권한이 없습니다",
			"이 기능에 접근할 권한이 없습니다.",
			ErrorStage.ACCOUNT,
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

	AuthErrorCode(
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
