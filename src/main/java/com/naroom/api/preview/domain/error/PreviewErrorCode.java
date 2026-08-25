package com.naroom.api.preview.domain.error;

import com.naroom.api.global.error.code.ClientAction;
import com.naroom.api.global.error.code.ErrorCode;
import com.naroom.api.global.error.code.ErrorStage;
import org.springframework.http.HttpStatus;

// Admin Web Implementation Spec §16 기준. 회원/관리자 오류 체계와 분리된 미리보기 전용 오류 코드.
public enum PreviewErrorCode implements ErrorCode {

	PREVIEW_AUTHENTICATION_FAILED(
			HttpStatus.UNAUTHORIZED,
			"PREVIEW_AUTHENTICATION_FAILED",
			"urn:naroom:problem:preview-authentication-failed",
			"미리보기 인증에 실패했습니다",
			"관리자 화면에서 미리보기를 다시 열어 주세요.",
			ErrorStage.LOGIN,
			ClientAction.LOGIN_REQUIRED,
			false),

	PREVIEW_SESSION_EXPIRED(
			HttpStatus.UNAUTHORIZED,
			"PREVIEW_SESSION_EXPIRED",
			"urn:naroom:problem:preview-session-expired",
			"미리보기 세션이 만료되었습니다",
			"관리자 화면에서 미리보기를 다시 열어 주세요.",
			ErrorStage.SESSION,
			ClientAction.LOGIN_REQUIRED,
			false),

	PREVIEW_CONTENT_NOT_SELECTED(
			HttpStatus.NOT_FOUND,
			"PREVIEW_CONTENT_NOT_SELECTED",
			"urn:naroom:problem:preview-content-not-selected",
			"미리보기로 선택된 콘텐츠가 없습니다",
			"관리자 화면에서 미리보기할 콘텐츠를 선택해 주세요.",
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

	PreviewErrorCode(
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
