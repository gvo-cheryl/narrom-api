package com.naroom.api.account.domain.error;

import com.naroom.api.global.error.code.ClientAction;
import com.naroom.api.global.error.code.ErrorCode;
import com.naroom.api.global.error.code.ErrorStage;
import org.springframework.http.HttpStatus;

// exception-handling.md 패키지 규칙: 기능별 오류 코드는 각 기능이 소유한다.
// ACCOUNT_LOCKED/ACCOUNT_PENDING_DELETION은 로그인·재발급 흐름에서 먼저 쓰였던 이력 때문에 AuthErrorCode에 남아 있다.
public enum AccountErrorCode implements ErrorCode {

	ACCOUNT_VERSION_CONFLICT(
			HttpStatus.CONFLICT,
			"ACCOUNT_VERSION_CONFLICT",
			"urn:naroom:problem:account-version-conflict",
			"회원 정보가 변경되었습니다",
			"최신 정보를 다시 불러온 뒤 수정해 주세요.",
			ErrorStage.PERSISTENCE,
			ClientAction.RELOAD_RESOURCE,
			false),

	ONBOARDING_CONSENT_REQUIRED(
			HttpStatus.BAD_REQUEST,
			"ONBOARDING_CONSENT_REQUIRED",
			"urn:naroom:problem:onboarding-consent-required",
			"필수 동의가 필요합니다",
			"필수 동의를 확인해 주세요.",
			ErrorStage.ONBOARDING,
			ClientAction.CHECK_REQUEST,
			false),

	ONBOARDING_DOCUMENT_VERSION_INVALID(
			HttpStatus.CONFLICT,
			"ONBOARDING_DOCUMENT_VERSION_INVALID",
			"urn:naroom:problem:onboarding-document-version-invalid",
			"현재 허용하지 않는 문서 버전입니다",
			"앱을 최신 버전으로 업데이트해 주세요.",
			ErrorStage.ONBOARDING,
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

	AccountErrorCode(
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
