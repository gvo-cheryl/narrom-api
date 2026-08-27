package com.naroom.api.admin.domain.error;

import com.naroom.api.global.error.code.ClientAction;
import com.naroom.api.global.error.code.ErrorCode;
import com.naroom.api.global.error.code.ErrorStage;
import org.springframework.http.HttpStatus;

// Admin Web Implementation Spec 17.12 오류 코드 표 기준.
public enum AdminErrorCode implements ErrorCode {

	ADMIN_AUTHENTICATION_FAILED(
			HttpStatus.UNAUTHORIZED,
			"ADMIN_AUTHENTICATION_FAILED",
			"urn:naroom:problem:admin-authentication-failed",
			"관리자 인증에 실패했습니다",
			"다시 로그인해 주세요.",
			ErrorStage.LOGIN,
			ClientAction.LOGIN_REQUIRED,
			false),

	ADMIN_ACCESS_DENIED(
			HttpStatus.FORBIDDEN,
			"ADMIN_ACCESS_DENIED",
			"urn:naroom:problem:admin-access-denied",
			"승인된 관리자 계정이 아닙니다",
			"관리자에게 문의해 주세요.",
			ErrorStage.ACCOUNT,
			ClientAction.CONTACT_SUPPORT,
			false),

	ADMIN_ACCOUNT_DISABLED(
			HttpStatus.FORBIDDEN,
			"ADMIN_ACCOUNT_DISABLED",
			"urn:naroom:problem:admin-account-disabled",
			"비활성화된 관리자 계정입니다",
			"관리자에게 문의해 주세요.",
			ErrorStage.ACCOUNT,
			ClientAction.CONTACT_SUPPORT,
			false),

	ADMIN_SESSION_EXPIRED(
			HttpStatus.UNAUTHORIZED,
			"ADMIN_SESSION_EXPIRED",
			"urn:naroom:problem:admin-session-expired",
			"관리자 세션이 만료되었습니다",
			"다시 로그인해 주세요.",
			ErrorStage.SESSION,
			ClientAction.LOGIN_REQUIRED,
			false),

	ADMIN_CSRF_INVALID(
			HttpStatus.FORBIDDEN,
			"ADMIN_CSRF_INVALID",
			"urn:naroom:problem:admin-csrf-invalid",
			"요청을 처리할 수 없습니다",
			"페이지를 새로고침한 뒤 다시 시도해 주세요.",
			ErrorStage.REQUEST,
			ClientAction.RELOAD_RESOURCE,
			false),

	ADMIN_REAUTH_REQUIRED(
			HttpStatus.FORBIDDEN,
			"ADMIN_REAUTH_REQUIRED",
			"urn:naroom:problem:admin-reauth-required",
			"민감한 작업은 재인증이 필요합니다",
			"다시 로그인한 뒤 시도해 주세요.",
			ErrorStage.SESSION,
			ClientAction.LOGIN_REQUIRED,
			false),

	ADMIN_INVITATION_NOT_FOUND(
			HttpStatus.NOT_FOUND,
			"ADMIN_INVITATION_NOT_FOUND",
			"urn:naroom:problem:admin-invitation-not-found",
			"초대를 찾을 수 없습니다",
			"존재하지 않는 관리자 초대입니다.",
			ErrorStage.REQUEST,
			ClientAction.CHECK_REQUEST,
			false),

	ADMIN_INVITATION_ALREADY_EXISTS(
			HttpStatus.CONFLICT,
			"ADMIN_INVITATION_ALREADY_EXISTS",
			"urn:naroom:problem:admin-invitation-already-exists",
			"이미 등록되었거나 초대된 이메일입니다",
			"기존 초대를 취소하거나, 이미 관리자로 등록된 계정인지 확인해 주세요.",
			ErrorStage.REQUEST,
			ClientAction.CHECK_REQUEST,
			false),

	ADMIN_INVITATION_NOT_PENDING(
			HttpStatus.CONFLICT,
			"ADMIN_INVITATION_NOT_PENDING",
			"urn:naroom:problem:admin-invitation-not-pending",
			"처리할 수 없는 상태입니다",
			"이미 사용되었거나 취소된 초대입니다.",
			ErrorStage.REQUEST,
			ClientAction.CHECK_REQUEST,
			false);

	private final HttpStatus httpStatus;
	private final String code;
	private final String type;
	private final String title;
	private final String detail;
	private final ErrorStage stage;
	private final ClientAction action;
	private final boolean retryable;

	AdminErrorCode(
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
