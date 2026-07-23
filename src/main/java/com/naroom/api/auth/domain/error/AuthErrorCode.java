package com.naroom.api.auth.domain.error;

import com.naroom.api.global.error.code.ClientAction;
import com.naroom.api.global.error.code.ErrorCode;
import com.naroom.api.global.error.code.ErrorStage;
import org.springframework.http.HttpStatus;

/**
 * Auth 도메인 오류 코드. 카카오 로그인·계정 상태(ACCOUNT_LOCKED 등)·온보딩 관련 코드는
 * 해당 기능(오늘 12번)을 구현하며 error-response.md의 "인증·시작 단계 오류" 표를 기준으로 추가한다.
 * 여기 있는 TOKEN/SESSION/DEVICE 코드는 JwtAuthenticationFilter·AuthSessionService가 실제로 던진다.
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
			false),

	AUTH_ACCESS_TOKEN_EXPIRED(
			HttpStatus.UNAUTHORIZED,
			"AUTH_ACCESS_TOKEN_EXPIRED",
			"urn:naroom:problem:auth-access-token-expired",
			"인증 정보가 만료되었습니다",
			"인증 정보를 다시 확인해 주세요.",
			ErrorStage.TOKEN,
			ClientAction.REFRESH_REQUIRED,
			false),

	AUTH_ACCESS_TOKEN_INVALID(
			HttpStatus.UNAUTHORIZED,
			"AUTH_ACCESS_TOKEN_INVALID",
			"urn:naroom:problem:auth-access-token-invalid",
			"인증 정보가 올바르지 않습니다",
			"다시 로그인해 주세요.",
			ErrorStage.TOKEN,
			ClientAction.CLEAR_SESSION_AND_LOGIN,
			false),

	AUTH_REFRESH_TOKEN_EXPIRED(
			HttpStatus.UNAUTHORIZED,
			"AUTH_REFRESH_TOKEN_EXPIRED",
			"urn:naroom:problem:auth-refresh-token-expired",
			"로그인이 만료되었습니다",
			"다시 로그인해 주세요.",
			ErrorStage.TOKEN,
			ClientAction.CLEAR_SESSION_AND_LOGIN,
			false),

	AUTH_REFRESH_TOKEN_INVALID(
			HttpStatus.UNAUTHORIZED,
			"AUTH_REFRESH_TOKEN_INVALID",
			"urn:naroom:problem:auth-refresh-token-invalid",
			"인증 정보가 올바르지 않습니다",
			"다시 로그인해 주세요.",
			ErrorStage.TOKEN,
			ClientAction.CLEAR_SESSION_AND_LOGIN,
			false),

	AUTH_DEVICE_MISMATCH(
			HttpStatus.UNAUTHORIZED,
			"AUTH_DEVICE_MISMATCH",
			"urn:naroom:problem:auth-device-mismatch",
			"기기 정보가 일치하지 않습니다",
			"다시 로그인해 주세요.",
			ErrorStage.DEVICE,
			ClientAction.CLEAR_SESSION_AND_LOGIN,
			false),

	AUTH_SESSION_NOT_FOUND(
			HttpStatus.UNAUTHORIZED,
			"AUTH_SESSION_NOT_FOUND",
			"urn:naroom:problem:auth-session-not-found",
			"로그인 정보를 찾을 수 없습니다",
			"다시 로그인해 주세요.",
			ErrorStage.SESSION,
			ClientAction.CLEAR_SESSION_AND_LOGIN,
			false),

	AUTH_SESSION_EXPIRED(
			HttpStatus.UNAUTHORIZED,
			"AUTH_SESSION_EXPIRED",
			"urn:naroom:problem:auth-session-expired",
			"로그인이 만료되었습니다",
			"다시 로그인해 주세요.",
			ErrorStage.SESSION,
			ClientAction.CLEAR_SESSION_AND_LOGIN,
			false),

	AUTH_SESSION_REVOKED(
			HttpStatus.UNAUTHORIZED,
			"AUTH_SESSION_REVOKED",
			"urn:naroom:problem:auth-session-revoked",
			"로그아웃된 상태입니다",
			"다시 로그인해 주세요.",
			ErrorStage.SESSION,
			ClientAction.CLEAR_SESSION_AND_LOGIN,
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
